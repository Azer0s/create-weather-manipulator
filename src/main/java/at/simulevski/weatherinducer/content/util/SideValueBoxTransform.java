package at.simulevski.weatherinducer.content.util;

import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiPredicate;

/**
 * Thin convenience wrapper around Create's {@link CenteredSideValueBoxTransform}
 * so the block entities can place a scroll value box on a specific set of block
 * faces without repeating the predicate plumbing.
 *
 * <p>{@code CenteredSideValueBoxTransform} handles the pose/rotation maths for
 * us; we only supply which faces are valid for a given block state, and
 * optionally how deep into the block the box sits. The default depth of 15.5
 * hugs a full-cube face; blocks whose faces are recessed (the flanged
 * resistor and gate bodies stop at 12 of 16) pass a smaller depth so the box
 * sits on the actual surface. That matters beyond looks: Create only accepts
 * a click within a quarter block of the box, so a box buried at full-cube
 * depth behind a recessed face is nearly impossible to hit.
 */
public class SideValueBoxTransform extends CenteredSideValueBoxTransform {

    private final float depth;

    public SideValueBoxTransform(BiPredicate<BlockState, Direction> allowedDirections) {
        this(allowedDirections, 15.5f);
    }

    public SideValueBoxTransform(BiPredicate<BlockState, Direction> allowedDirections, float depth) {
        super(allowedDirections);
        this.depth = depth;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, depth);
    }
}
