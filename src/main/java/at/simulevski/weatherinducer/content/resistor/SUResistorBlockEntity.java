package at.simulevski.weatherinducer.content.resistor;

import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import at.simulevski.weatherinducer.network.SUNetwork;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
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
 * The SU Resistor's brains. Two jobs:
 *
 * <ul>
 *   <li>Hold the configured draw cap for the mod's own SU model (the Weather
 *       Inducer and SU Charger read it through {@link SUNetwork}).</li>
 *   <li>Enforce the same cap on real Create machines by acting as a circuit
 *       breaker. Every few ticks it sums the stress demand of everything
 *       downstream of it; if that exceeds the cap it trips, cutting rotation
 *       to the downstream side exactly like a powered clutch (the block
 *       entity is a {@link SplitShaftBlockEntity}, so Create's rotation
 *       propagator honours the per-face speed modifier). A tripped resistor
 *       re-closes after a moment; if the load is still too high, the next
 *       measurement trips it again.</li>
 * </ul>
 */
public class SUResistorBlockEntity extends SplitShaftBlockEntity implements IHaveGoggleInformation {

    /**
     * The selectable draw caps, a logarithmic 1-2.5-5 ladder in SU. Scrolling
     * linearly over a 0..1,000,000 range was hopeless; the scroll behaviour
     * stores an index into this table instead and the formatter shows the SU
     * value it stands for.
     */
    public static final int[] STEPS = {
            0, 100, 250, 500, 1_000, 2_500, 5_000, 10_000,
            25_000, 50_000, 100_000, 250_000, 500_000, 1_000_000,
    };

    /** Index of the 1,000 SU default in {@link #STEPS}. */
    private static final int DEFAULT_INDEX = 4;

    /** How often the downstream demand is re-measured, in ticks. */
    private static final int MEASURE_INTERVAL = 10;

    /** How long a tripped breaker waits before it tries to close again. */
    private static final int RETRY_TICKS = 60;

    private ScrollValueBehaviour suLimit;
    private int retryCooldown;
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
        suLimit.between(0, STEPS.length - 1);
        suLimit.withFormatter(index -> String.format("%,d", STEPS[clampIndex(index)]));
        suLimit.setValue(DEFAULT_INDEX);
        behaviours.add(suLimit);
    }

    private static int clampIndex(int index) {
        return Math.max(0, Math.min(index, STEPS.length - 1));
    }

    /** The configured SU draw cap this resistor allows to pass. */
    public int getSuLimit() {
        return suLimit != null ? STEPS[clampIndex(suLimit.getValue())] : STEPS[DEFAULT_INDEX];
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
            // Try closing again after a moment; if the load is still too
            // high, the next measurement below trips it right back.
            if (--retryCooldown <= 0) {
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
            retryCooldown = RETRY_TICKS;
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

    // --- Test hook (used by the game tests; harmless in normal play) ---------

    public void setLimitIndexForTesting(int index) {
        if (suLimit != null) {
            suLimit.setValue(clampIndex(index));
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
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.demand",
                        String.format("%,.0f", lastDemand))
                        .withStyle(lastDemand > getSuLimit() ? ChatFormatting.RED : ChatFormatting.GRAY)));
        if (isTripped()) {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("weatherinducer.tooltip.tripped")
                            .withStyle(ChatFormatting.GOLD)));
        }
        return true;
    }
}
