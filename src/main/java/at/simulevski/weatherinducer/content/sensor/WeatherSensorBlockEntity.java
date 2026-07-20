package at.simulevski.weatherinducer.content.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * State-less block entity for the Weather Sensor; it exists only so the block
 * can tick and refresh its POWER property, the same trick the vanilla
 * daylight detector uses.
 */
public class WeatherSensorBlockEntity extends BlockEntity {

    /** How often the weather is re-read, in ticks. */
    private static final int UPDATE_INTERVAL = 8;

    public WeatherSensorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % UPDATE_INTERVAL != 0) {
            return;
        }
        int power = 0;
        if (level.canSeeSky(pos.above())) {
            if (level.isThundering()) {
                power = 15;
            } else if (level.isRaining()) {
                power = 7;
            }
        }
        if (state.getValue(WeatherSensorBlock.POWER) != power) {
            level.setBlock(pos, state.setValue(WeatherSensorBlock.POWER, power), Block.UPDATE_ALL);
        }
    }
}
