package at.simulevski.weatherinducer.content.resistor;

import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Holds the SU draw cap for one SU Resistor: the most SU whatever is hooked up
 * through this resistor may pull from the network. The value is stored and
 * synced by the {@link ScrollValueBehaviour} itself; this block entity just
 * exposes it so a downstream Weather Inducer can read it.
 */
public class SUResistorBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

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

    private ScrollValueBehaviour suLimit;

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
        return true;
    }
}
