package at.simulevski.weatherinducer.content.inducer;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Weather Inducer block. A horizontally-facing kinetic block: its shaft
 * enters along the facing axis (front/back), the two side faces host the
 * lightning-offset value boxes, and the top face hosts the mode selector and
 * must be able to see the sky for the inducer to fire.
 */
public class WeatherInducerBlock extends HorizontalKineticBlock
        implements IBE<WeatherInducerBlockEntity> {

    public WeatherInducerBlock(Properties properties) {
        super(properties);
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

    /** Comparator output scales 0..15 with charge fraction (0..100k SU). */
    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof WeatherInducerBlockEntity be) {
            return be.getComparatorOutput();
        }
        return 0;
    }
}
