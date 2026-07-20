package at.simulevski.weatherinducer.content.gate;

import at.simulevski.weatherinducer.content.util.SUValueLadder;
import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The Stress Gate's brains: every few ticks it reads the total SU its
 * kinetic network provides and unlocks when that reaches the configured
 * threshold, locking again if the provision falls below it. The provided
 * capacity comes from the source side only, so locking the downstream away
 * never changes the reading and the gate cannot flap.
 */
public class StressGateBlockEntity extends SplitShaftBlockEntity implements IHaveGoggleInformation {

    /** How often the network provision is re-read, in ticks. */
    private static final int MEASURE_INTERVAL = 10;

    /** Default threshold: 100,000 SU (index into the shared ladder). */
    private static final int DEFAULT_INDEX = 10;

    private ScrollValueBehaviour threshold;
    private double lastProvided;

    public StressGateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        // Value box on the four faces perpendicular to the shaft axis.
        threshold = new ScrollValueBehaviour(
                Component.translatable("weatherinducer.value.threshold"),
                this,
                new SideValueBoxTransform((state, dir) -> dir.getAxis() != state.getValue(StressGateBlock.AXIS)));
        threshold.between(0, SUValueLadder.STEPS.length - 1);
        threshold.withFormatter(SUValueLadder::format);
        threshold.setValue(DEFAULT_INDEX);
        behaviours.add(threshold);
    }

    /** The provided-SU level at which the gate unlocks. */
    public int getThreshold() {
        return threshold != null ? SUValueLadder.value(threshold.getValue())
                : SUValueLadder.STEPS[DEFAULT_INDEX];
    }

    /** Clutch semantics: a locked gate passes nothing downstream. */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (hasSource() && face != getSourceFacing()
                && getBlockState().getValue(StressGateBlock.LOCKED)) {
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
        if (level.getGameTime() % MEASURE_INTERVAL != 0) {
            return;
        }
        KineticNetwork network = getOrCreateNetwork();
        lastProvided = network == null ? 0 : Math.max(0, network.calculateCapacity());
        boolean shouldLock = lastProvided < getThreshold();
        if (shouldLock != getBlockState().getValue(StressGateBlock.LOCKED)) {
            setLocked(shouldLock);
        }
    }

    private void setLocked(boolean locked) {
        level.setBlock(worldPosition,
                getBlockState().setValue(StressGateBlock.LOCKED, locked), Block.UPDATE_ALL);
        // Re-propagate rotation with the new modifier, gearshift-style.
        if (level.getBlockState(worldPosition).getBlock() instanceof StressGateBlock block) {
            block.detachKinetics(level, worldPosition, true);
        }
    }

    public boolean isLocked() {
        return getBlockState().getValue(StressGateBlock.LOCKED);
    }

    // --- Test hook (used by the game tests; harmless in normal play) ---------

    public void setThresholdIndexForTesting(int index) {
        if (threshold != null) {
            threshold.setValue(SUValueLadder.clampIndex(index));
        }
    }

    // @Override intentionally present: if Create ever changes this signature,
    // the compile breaks here instead of goggles silently going blank.
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.stress_gate")
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.threshold",
                        String.format("%,d", getThreshold()))
                        .withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.provided",
                        String.format("%,.0f", lastProvided))
                        .withStyle(lastProvided >= getThreshold() ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
        if (isLocked()) {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("weatherinducer.tooltip.locked")
                            .withStyle(ChatFormatting.GOLD)));
        }
        return true;
    }
}
