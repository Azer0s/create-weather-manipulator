package at.simulevski.weatherinducer.content.resistor;

import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
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
 * Holds the SU/tick cap for one SU Resistor. The value is stored and synced by
 * the {@link ScrollValueBehaviour} itself; this block entity just exposes it so
 * a downstream Weather Inducer can read it.
 */
public class SUResistorBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

    /** Default cap in SU/tick. */
    public static final int DEFAULT_LIMIT = 1_000;
    /** Upper bound of the configurable cap. */
    public static final int MAX_LIMIT = 1_000_000;

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
        suLimit.between(0, MAX_LIMIT);
        suLimit.setValue(DEFAULT_LIMIT);
        behaviours.add(suLimit);
    }

    /** The configured SU/tick cap this resistor allows to pass. */
    public int getSuLimit() {
        return suLimit != null ? suLimit.getValue() : DEFAULT_LIMIT;
    }

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
