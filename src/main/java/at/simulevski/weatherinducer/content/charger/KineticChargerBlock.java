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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Kinetic Charger: a kinetic battery with two working sides. The front
 * (teal ring) is the I/O face: an external source charges the battery
 * through it, and once that source stops, the charger drives the same face
 * from its buffer. The back is the flywheel face: the wheels banked there
 * set the capacity and keep spinning as long as the battery holds energy.
 * Both faces stay connected at all times; who powers whom is decided by
 * the block entity from the network's source list. The block also emits a
 * redstone signal proportional to the buffer fill.
 */
public class KineticChargerBlock extends HorizontalKineticBlock
        implements IBE<KineticChargerBlockEntity> {

    /** Redstone output, kept in step with the buffer fill by the block entity. */
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    /** Battery mode: generating into the output face, input disconnected. */
    public static final BooleanProperty DISCHARGING = BooleanProperty.create("discharging");

    public KineticChargerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(POWER, 0)
                .setValue(DISCHARGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER, DISCHARGING);
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
        // Dropper-style: the I/O face points away from the player, the
        // flywheel face towards them.
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
    public Class<KineticChargerBlockEntity> getBlockEntityClass() {
        return KineticChargerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends KineticChargerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.KINETIC_CHARGER.get();
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
        if (level.getBlockEntity(pos) instanceof KineticChargerBlockEntity be) {
            return be.getComparatorOutput();
        }
        return 0;
    }
}
