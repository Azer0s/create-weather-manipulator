package at.simulevski.weatherinducer.content.charger;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The SU Charger: a kinetic capacitor. Rotation passes straight through along
 * the facing axis, but SU never does; the charger is always a barrier in the
 * SU graph. While the shaft turns it fills an internal buffer from the
 * network on its input face (the back); once the input stops, consumers on
 * its output face (the front) may draw from that buffer. The block also emits
 * a redstone signal proportional to how full the buffer is.
 */
public class SUChargerBlock extends HorizontalKineticBlock
        implements IBE<SUChargerBlockEntity> {

    /** Redstone output, kept in step with the buffer fill by the block entity. */
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public SUChargerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Dropper-style: the output face points away from the player.
        return defaultBlockState().setValue(HORIZONTAL_FACING,
                context.getHorizontalDirection());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos,
                                   BlockState state, Direction face) {
        return face.getAxis() == state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<SUChargerBlockEntity> getBlockEntityClass() {
        return SUChargerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SUChargerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SU_CHARGER.get();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator output scales 0..15 with the buffer fill fraction. */
    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SUChargerBlockEntity be) {
            return be.getComparatorOutput();
        }
        return 0;
    }
}
