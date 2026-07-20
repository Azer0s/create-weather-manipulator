package at.simulevski.weatherinducer.content.inducer;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Weather Inducer block. A horizontally-facing kinetic block: its shaft
 * enters along the facing axis (front/back), the two side faces host the
 * lightning-offset value boxes, and the top face hosts the mode selector and
 * must be able to see the sky for the inducer to fire.
 */
public class WeatherInducerBlock extends HorizontalKineticBlock
        implements IBE<WeatherInducerBlockEntity> {

    /**
     * Charge fill indicator in sixths, driven by the block entity. The bolt
     * emblem on the side textures lights up with it.
     */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 5);

    /** Casing base with the raised emitter cap; same for every facing. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 13, 16),
            Block.box(2, 13, 2, 14, 16, 14));

    public WeatherInducerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the shaft axis towards/away from the player who placed it.
        return defaultBlockState().setValue(HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos,
                                   BlockState state, Direction face) {
        // Shaft on the front and back faces (along the facing axis).
        return face.getAxis() == state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<WeatherInducerBlockEntity> getBlockEntityClass() {
        return WeatherInducerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WeatherInducerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.WEATHER_INDUCER.get();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator output scales 0..15 with charge fraction (0..2^20 SU). */
    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof WeatherInducerBlockEntity be) {
            return be.getComparatorOutput();
        }
        return 0;
    }
}
