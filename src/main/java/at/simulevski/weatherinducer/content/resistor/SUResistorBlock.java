package at.simulevski.weatherinducer.content.resistor;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The SU Resistor: an inline shaft block (rotation passes straight through
 * along its axis, exactly like a shaft) that carries a configurable SU/tick
 * cap. It does not itself alter Create's kinetics; instead a Weather Inducer
 * that sits downstream reads any resistor found inline upstream of it and uses
 * the tightest cap to throttle how fast it charges (see
 * {@code at.simulevski.weatherinducer.network.SUNetwork}).
 */
public class SUResistorBlock extends RotatedPillarKineticBlock
        implements IBE<SUResistorBlockEntity> {

    /** Two collar flanges at the shaft ends with the ceramic body between. */
    private static final VoxelShape SHAPE_Y = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(2, 13, 2, 14, 16, 14),
            Block.box(4, 3, 4, 12, 13, 12));
    private static final VoxelShape SHAPE_X = Shapes.or(
            Block.box(0, 2, 2, 3, 14, 14),
            Block.box(13, 2, 2, 16, 14, 14),
            Block.box(3, 4, 4, 13, 12, 12));
    private static final VoxelShape SHAPE_Z = Shapes.or(
            Block.box(2, 2, 0, 14, 14, 3),
            Block.box(2, 2, 13, 14, 14, 16),
            Block.box(4, 4, 3, 12, 12, 13));

    public SUResistorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> SHAPE_Y;
        };
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Class<SUResistorBlockEntity> getBlockEntityClass() {
        return SUResistorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SUResistorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SU_RESISTOR.get();
    }
}
