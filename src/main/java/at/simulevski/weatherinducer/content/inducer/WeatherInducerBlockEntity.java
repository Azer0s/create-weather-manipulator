package at.simulevski.weatherinducer.content.inducer;

import at.simulevski.weatherinducer.content.util.KeyedScrollOptionBehaviour;
import at.simulevski.weatherinducer.content.util.KeyedScrollValueBehaviour;
import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import at.simulevski.weatherinducer.network.SUNetwork;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Drives the Weather Inducer:
 * <ul>
 *   <li>charges from the kinetic network's spare SU (provided capacity minus
 *       used stress, see {@link SUNetwork}) up to {@link #MAX_CHARGE},
 *       taking at most {@link #MAX_INTAKE_PER_TICK} per tick. A stopped line
 *       offers nothing unless a discharging SU Charger feeds it;</li>
 *   <li>when fully charged, a rising redstone edge fires the selected weather
 *       effect, provided the block above can see the sky;</li>
 *   <li>exposes three scroll value boxes: mode (top), and the lightning X/Z
 *       offsets (the two side faces).</li>
 * </ul>
 */
public class WeatherInducerBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

    /** The Weather Inducer must accumulate this many SU before it can fire. */
    public static final double MAX_CHARGE = 1_048_576.0; // 2^20

    /** The most SU the inducer itself can pull in per tick, resistors aside. */
    public static final double MAX_INTAKE_PER_TICK = 131_072.0; // 2^17, full in 8 ticks

    /** Weather effect durations (ticks). 6000 ticks = 5 in-game minutes. */
    private static final int RAIN_TIME = 6000;
    private static final int CLEAR_TIME = 6000;

    private static final int OFFSET_RANGE = 64;

    private double charge;
    private boolean wasPowered;

    // --- Value box behaviours -------------------------------------------
    // The mode selector is a ScrollOptionBehaviour, which gives Create's
    // option menu (icon plus label per entry) instead of a bare number.
    private ScrollOptionBehaviour<WeatherMode> modeScroll;
    private ScrollValueBehaviour offsetXScroll;
    private ScrollValueBehaviour offsetZScroll;

    public WeatherInducerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        // Three boxes on one block entity: Create keeps behaviours in a map
        // keyed by type, and its stock scroll behaviours all share a single
        // type, so plain ones would silently replace each other (only the
        // last would exist; the mode selector was unreachable). The keyed
        // variants get a type and packet id each, so all three coexist and
        // clicks route to the box that was actually hit.
        modeScroll = new KeyedScrollOptionBehaviour<>("Mode", 1, WeatherMode.class,
                Component.translatable("weatherinducer.value.mode"),
                this,
                new SideValueBoxTransform((state, dir) -> dir == Direction.UP));
        behaviours.add(modeScroll);

        offsetXScroll = new KeyedScrollValueBehaviour("OffsetX", 2,
                Component.translatable("weatherinducer.value.offset_x"),
                this,
                new SideValueBoxTransform((state, dir) ->
                        dir == state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getClockWise()));
        offsetXScroll.between(-OFFSET_RANGE, OFFSET_RANGE);
        behaviours.add(offsetXScroll);

        offsetZScroll = new KeyedScrollValueBehaviour("OffsetZ", 3,
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

        // Charge from the network's spare SU (provided capacity minus what
        // the machines use), topping up from any discharging SU Charger. A
        // dead network has no spare capacity, so no speed gate is needed.
        // SU Resistors on the way to the generators cap the per-tick draw
        // (that is their original job: without one, a full charge is
        // gulped down at 131,072 SU per tick).
        if (charge < MAX_CHARGE) {
            double wanted = Math.min(MAX_INTAKE_PER_TICK, MAX_CHARGE - charge);
            wanted = Math.min(wanted, SUNetwork.resistorIntakeCap(this));
            double intake = Math.min(wanted, SUNetwork.remainingSU(this));
            if (intake < wanted) {
                intake += SUNetwork.drawFromChargers(this, wanted - intake);
            }
            if (intake > 0) {
                double before = charge;
                charge = Math.min(MAX_CHARGE, charge + intake);
                if (charge != before) {
                    setChanged();
                    // Keep the comparator output in step with the charge level.
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                    updateChargeIndicator();
                    // Sync the exact value now and then so the goggle
                    // readout tracks the charge, not just chunk loads.
                    if (charge >= MAX_CHARGE || level.getGameTime() % 8 == 0) {
                        sendData();
                    }
                }
            }
        }

        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered && !wasPowered) {
            fire();
        }
        wasPowered = powered;
    }

    /**
     * Attempts to fire the selected weather effect. Succeeds only if fully
     * charged and the sky is visible; on success the effect is applied and the
     * block discharges to 0. Server-side only.
     *
     * @return {@code true} if the inducer fired.
     */
    public boolean fire() {
        if (level == null || level.isClientSide) {
            return false;
        }
        if (charge < MAX_CHARGE) {
            return false;
        }
        ServerLevel server = (ServerLevel) level;
        // Line of sight to the sky: the column directly above must be open.
        if (!server.canSeeSky(worldPosition.above())) {
            return false;
        }

        applyWeather(server, getMode());

        // Discharge: the capacitor is spent and must recharge before firing again.
        charge = 0;
        setChanged();
        sendData();
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        updateChargeIndicator();
        return true;
    }

    /**
     * Mirrors the charge into the {@link WeatherInducerBlock#CHARGE} property
     * (sixths of a full charge) so the bolt emblem on the block lights up with
     * the fill level.
     */
    private void updateChargeIndicator() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        int target = (int) Math.floor(5.0 * Math.min(1.0, charge / MAX_CHARGE));
        if (state.getValue(WeatherInducerBlock.CHARGE) != target) {
            level.setBlock(worldPosition, state.setValue(WeatherInducerBlock.CHARGE, target),
                    Block.UPDATE_ALL);
        }
    }

    private void applyWeather(ServerLevel server, WeatherMode mode) {
        switch (mode) {
            case RAIN -> server.setWeatherParameters(0, RAIN_TIME, true, false);
            case CLEAR -> server.setWeatherParameters(CLEAR_TIME, 0, false, false);
            case LIGHTNING -> summonLightning(server);
        }
    }

    private void summonLightning(ServerLevel server) {
        BlockPos target = worldPosition.offset(getLightningOffsetX(), 0, getLightningOffsetZ());
        BlockPos surface = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(surface));
            server.addFreshEntity(bolt);
        }
    }

    // --- Public API (used by ComputerCraft / KubeJS integrations) -----------

    public WeatherMode getMode() {
        return WeatherMode.fromIndex(modeScroll.getValue());
    }

    public void setMode(WeatherMode mode) {
        modeScroll.setValue(mode.ordinal());
        setChanged();
    }

    public boolean isCharged() {
        return charge >= MAX_CHARGE;
    }

    public boolean hasSkyAccess() {
        return level != null && level.canSeeSky(worldPosition.above());
    }

    public int getLightningOffsetX() {
        return offsetXScroll.getValue();
    }

    public int getLightningOffsetZ() {
        return offsetZScroll.getValue();
    }

    public void setLightningOffset(int x, int z) {
        offsetXScroll.setValue(x);
        offsetZScroll.setValue(z);
        setChanged();
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

    // --- Test hooks (used by the game tests; harmless in normal play) --------

    public void setChargeForTesting(double value) {
        this.charge = Math.max(0, Math.min(MAX_CHARGE, value));
        setChanged();
        updateChargeIndicator();
    }

    public void setModeForTesting(int mode) {
        if (modeScroll != null) {
            modeScroll.setValue(mode);
        }
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

    // @Override intentionally present: if Create ever changes this signature,
    // the compile breaks here instead of goggles silently going blank.
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
