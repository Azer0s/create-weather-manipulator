package at.simulevski.weatherinducer.content.inducer;

import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import at.simulevski.weatherinducer.network.SUNetwork;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Drives the Weather Inducer:
 * <ul>
 *   <li>charges from the kinetic network's SU (see {@link SUNetwork}) up to
 *       {@link #MAX_CHARGE}, but only while the shaft is turning;</li>
 *   <li>when fully charged, a rising redstone edge fires the selected weather
 *       effect, provided the block above can see the sky;</li>
 *   <li>exposes three scroll value boxes: mode (top), and the lightning X/Z
 *       offsets (the two side faces).</li>
 * </ul>
 */
public class WeatherInducerBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

    /** The Weather Inducer must accumulate this many SU before it can fire. */
    public static final double MAX_CHARGE = 100_000.0;

    /** Weather effect durations (ticks). 6000 ticks = 5 in-game minutes. */
    private static final int RAIN_TIME = 6000;
    private static final int CLEAR_TIME = 6000;

    private static final int OFFSET_RANGE = 64;

    private double charge;
    private boolean wasPowered;

    // --- Value box behaviours -------------------------------------------
    // NOTE (verify against your Create build): ScrollValueBehaviour's fluent
    // methods used below are `between`, `withFormatter` and `getValue`. If a
    // signature differs in your Create 6.0.10 artifact, these three call sites
    // are the only ones to adjust.
    private ScrollValueBehaviour modeScroll;
    private ScrollValueBehaviour offsetXScroll;
    private ScrollValueBehaviour offsetZScroll;

    public WeatherInducerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        modeScroll = new ScrollValueBehaviour(
                Component.translatable("weatherinducer.value.mode"),
                this,
                new SideValueBoxTransform((state, dir) -> dir == Direction.UP));
        modeScroll.between(0, WeatherMode.values().length - 1);
        modeScroll.withFormatter(value ->
                Component.translatable("weatherinducer.mode." + WeatherMode.fromIndex(value).translationKey())
                        .getString());
        behaviours.add(modeScroll);

        offsetXScroll = new ScrollValueBehaviour(
                Component.translatable("weatherinducer.value.offset_x"),
                this,
                new SideValueBoxTransform((state, dir) ->
                        dir == state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getClockWise()));
        offsetXScroll.between(-OFFSET_RANGE, OFFSET_RANGE);
        behaviours.add(offsetXScroll);

        offsetZScroll = new ScrollValueBehaviour(
                Component.translatable("weatherinducer.value.offset_z"),
                this,
                new SideValueBoxTransform((state, dir) ->
                        dir == state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getCounterClockWise()));
        offsetZScroll.between(-OFFSET_RANGE, OFFSET_RANGE);
        behaviours.add(offsetZScroll);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        // Charge from the network's SU, but only while the shaft is turning and
        // we are not already full.
        if (getSpeed() != 0 && charge < MAX_CHARGE) {
            double intake = SUNetwork.intakeThisTick(this);
            if (intake > 0) {
                double before = charge;
                charge = Math.min(MAX_CHARGE, charge + intake);
                if (charge != before) {
                    setChanged();
                    // Keep the comparator output in step with the charge level.
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                    if (charge >= MAX_CHARGE) {
                        sendData();
                    }
                }
            }
        }

        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered && !wasPowered) {
            tryFire((ServerLevel) level);
        }
        wasPowered = powered;
    }

    /** Fires the selected weather effect if charged and the sky is visible. */
    private void tryFire(ServerLevel server) {
        if (charge < MAX_CHARGE) {
            return;
        }
        // Line of sight to the sky: the column directly above must be open.
        if (!server.canSeeSky(worldPosition.above())) {
            return;
        }

        WeatherMode mode = WeatherMode.fromIndex(modeScroll.getValue());
        switch (mode) {
            case RAIN -> server.setWeatherParameters(0, RAIN_TIME, true, false);
            case CLEAR -> server.setWeatherParameters(CLEAR_TIME, 0, false, false);
            case LIGHTNING -> summonLightning(server);
        }

        // Discharge: the capacitor is spent and must recharge before firing again.
        charge = 0;
        setChanged();
        sendData();
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void summonLightning(ServerLevel server) {
        int dx = offsetXScroll.getValue();
        int dz = offsetZScroll.getValue();
        BlockPos target = worldPosition.offset(dx, 0, dz);
        BlockPos surface = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(surface));
            server.addFreshEntity(bolt);
        }
    }

    /** 0..15 comparator output scaling with charge fraction. */
    public int getComparatorOutput() {
        if (charge <= 0) {
            return 0;
        }
        return (int) Math.ceil(15.0 * Math.min(1.0, charge / MAX_CHARGE));
    }

    public double getCharge() {
        return charge;
    }

    // The Weather Inducer is a pure consumer: it applies stress to the network.
    @Override
    public float calculateStressApplied() {
        float impact = 8f; // SU per RPM
        this.lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("Charge", charge);
        compound.putBoolean("WasPowered", wasPowered);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        charge = compound.getDouble("Charge");
        wasPowered = compound.getBoolean("WasPowered");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.title")
                        .withStyle(ChatFormatting.GRAY)));

        int percent = (int) Math.floor(100.0 * Math.min(1.0, charge / MAX_CHARGE));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.charge",
                        String.format("%,.0f", charge), String.format("%,.0f", MAX_CHARGE), percent)
                        .withStyle(charge >= MAX_CHARGE ? ChatFormatting.GREEN : ChatFormatting.AQUA)));

        WeatherMode mode = WeatherMode.fromIndex(modeScroll.getValue());
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.mode",
                        Component.translatable("weatherinducer.mode." + mode.translationKey()))
                        .withStyle(ChatFormatting.GRAY)));

        return true;
    }
}
