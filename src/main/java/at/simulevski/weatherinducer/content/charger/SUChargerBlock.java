package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.ticks.TickPriority;

/**
 * The SU Charger: a kinetic battery. While driven, rotation passes straight
 * through along the facing axis and the internal buffer fills from the
 * network's spare SU; raw SU never crosses the block. Once the input stops,
 * the charger flips to discharge: it disconnects its input face, becomes a
 * kinetic source itself, and drives the output side at the speed it was
 * charged with, draining the buffer by whatever stress the machines use.
 * The block also emits a redstone signal proportional to the buffer fill.
 */
public class SUChargerBlock extends HorizontalKineticBlock
        implements IBE<SUChargerBlockEntity> {

    /** Redstone output, kept in step with the buffer fill by the block entity. */
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    /** Battery mode: generating into the output face, input disconnected. */
    public static final BooleanProperty DISCHARGING = BooleanProperty.create("discharging");

    public SUChargerBlock(Properties properties) {
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

    /**
     * Rips the block out of the rotation graph so it re-propagates with the
     * current DISCHARGING connections; same trick as Create's GearshiftBlock
     * (and our SU Resistor).
     */
    public void detachKinetics(Level level, BlockPos pos, boolean reAttachNextTick) {
        if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity be)) {
            return;
        }
        RotationPropagator.handleRemoved(level, pos, be);
        if (reAttachNextTick) {
            level.scheduleTick(pos, this, 1, TickPriority.EXTREMELY_HIGH);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity be) {
            RotationPropagator.handleAdded(level, pos, be);
        }
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
        if (face.getAxis() != state.getValue(HORIZONTAL_FACING).getAxis()) {
            return false;
        }
        // While discharging the battery only feeds its output face; the
        // input side is disconnected so the buffer-driven rotation cannot
        // leak back and so a restarting input cannot fight our source.
        if (state.getValue(DISCHARGING)) {
            return face == state.getValue(HORIZONTAL_FACING);
        }
        return true;
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
