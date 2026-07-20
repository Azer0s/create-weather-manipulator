package at.simulevski.weatherinducer.content.resistor;

import at.simulevski.weatherinducer.content.util.SUValueLadder;
import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import at.simulevski.weatherinducer.network.SUNetwork;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The SU Resistor: a circuit breaker for kinetic stress. While closed it
 * periodically sums the stress demand of everything downstream of it; if
 * that exceeds the configured limit it trips, cutting rotation to the
 * downstream side exactly like a powered clutch (the block entity is a
 * {@link SplitShaftBlockEntity}, so Create's rotation propagator honours the
 * per-face speed modifier).
 *
 * <p>The trip records the demand that broke it. A tripped breaker does no
 * scanning at all; it simply closes again the moment its limit covers that
 * recorded demand (scroll the limit up to reset it). Because closing
 * requires {@code limit >= demand at break}, the same load can never re-trip
 * it, so the breaker latches instead of oscillating.
 */
public class SUResistorBlockEntity extends SplitShaftBlockEntity implements IHaveGoggleInformation {

    /** How often the downstream demand is re-measured while closed, in ticks. */
    private static final int MEASURE_INTERVAL = 10;

    private ScrollValueBehaviour suLimit;
    private double demandAtBreak;
    private double lastDemand;

    public SUResistorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        // Value box on the four faces perpendicular to the shaft axis.
        suLimit = new ScrollValueBehaviour(
                Component.translatable("weatherinducer.value.su_limit"),
                this,
                new SideValueBoxTransform((state, dir) -> dir.getAxis() != state.getValue(SUResistorBlock.AXIS)));
        suLimit.between(0, SUValueLadder.STEPS.length - 1);
        suLimit.withFormatter(SUValueLadder::format);
        suLimit.setValue(4); // 1,000 SU
        behaviours.add(suLimit);
    }

    /** The configured SU draw limit this breaker allows downstream. */
    public int getSuLimit() {
        return suLimit != null ? SUValueLadder.value(suLimit.getValue()) : 1_000;
    }

    /** Clutch semantics: a tripped breaker passes nothing downstream. */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (hasSource() && face != getSourceFacing()
                && getBlockState().getValue(SUResistorBlock.TRIPPED)) {
            return 0;
        }
        return 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (getBlockState().getValue(SUResistorBlock.TRIPPED)) {
            // No scanning while tripped: close again once the limit covers
            // the demand recorded at break time. Closing with limit >= that
            // demand means the same load cannot immediately re-trip us.
            if (getSuLimit() >= demandAtBreak) {
                demandAtBreak = 0;
                setTripped(false);
            }
            return;
        }
        if (level.getGameTime() % MEASURE_INTERVAL != 0) {
            return;
        }
        if (getSpeed() == 0 || !hasSource()) {
            lastDemand = 0;
            return;
        }
        lastDemand = SUNetwork.downstreamStressDemand(this, getSourceFacing().getOpposite());
        if (lastDemand > getSuLimit()) {
            demandAtBreak = lastDemand;
            setChanged();
            setTripped(true);
        }
    }

    private void setTripped(boolean tripped) {
        BlockState state = getBlockState();
        if (state.getValue(SUResistorBlock.TRIPPED) == tripped) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(SUResistorBlock.TRIPPED, tripped),
                Block.UPDATE_ALL);
        // Re-propagate rotation with the new modifier, gearshift-style.
        if (level.getBlockState(worldPosition).getBlock() instanceof SUResistorBlock block) {
            block.detachKinetics(level, worldPosition, true);
        }
    }

    public boolean isTripped() {
        return getBlockState().getValue(SUResistorBlock.TRIPPED);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("DemandAtBreak", demandAtBreak);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        demandAtBreak = compound.getDouble("DemandAtBreak");
    }

    // --- Test hook (used by the game tests; harmless in normal play) ---------

    public void setLimitIndexForTesting(int index) {
        if (suLimit != null) {
            suLimit.setValue(SUValueLadder.clampIndex(index));
        }
    }

    // @Override intentionally present: if Create ever changes this signature,
    // the compile breaks here instead of goggles silently going blank.
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.su_resistor")
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.su_limit",
                        String.format("%,d", getSuLimit()))
                        .withStyle(ChatFormatting.AQUA)));
        if (isTripped()) {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("weatherinducer.tooltip.tripped",
                            String.format("%,.0f", demandAtBreak))
                            .withStyle(ChatFormatting.GOLD)));
        } else {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("weatherinducer.tooltip.demand",
                            String.format("%,.0f", lastDemand))
                            .withStyle(lastDemand > getSuLimit() ? ChatFormatting.RED : ChatFormatting.GRAY)));
        }
        return true;
    }
}
