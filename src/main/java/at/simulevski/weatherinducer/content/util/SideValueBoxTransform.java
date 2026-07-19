package at.simulevski.weatherinducer.content.util;

import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiPredicate;

/**
 * Thin convenience wrapper around Create's {@link CenteredSideValueBoxTransform}
 * so the block entities can place a scroll value box on a specific set of block
 * faces without repeating the predicate plumbing.
 *
 * <p>{@code CenteredSideValueBoxTransform} handles the pose/rotation maths for
 * us; we only supply which faces are valid for a given block state.
 */
public class SideValueBoxTransform extends CenteredSideValueBoxTransform {

    public SideValueBoxTransform(BiPredicate<BlockState, Direction> allowedDirections) {
        super(allowedDirections);
    }
}
