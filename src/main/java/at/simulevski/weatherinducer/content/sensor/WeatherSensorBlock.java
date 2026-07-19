package at.simulevski.weatherinducer.content.sensor;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The Weather Sensor: a daylight-detector-shaped slab that emits a redstone
 * signal for the current weather instead of the sun. Clear skies read 0, rain
 * reads 7 and a thunderstorm reads 15. Like its daylight cousin it needs to
 * see the sky; covered, it reads 0.
 */
public class WeatherSensorBlock extends Block implements EntityBlock {

    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public WeatherSensorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeatherSensorBlockEntity(ModBlockEntities.WEATHER_SENSOR.get(), pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.WEATHER_SENSOR.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> WeatherSensorBlockEntity.tick(lvl, pos, st);
    }
}
