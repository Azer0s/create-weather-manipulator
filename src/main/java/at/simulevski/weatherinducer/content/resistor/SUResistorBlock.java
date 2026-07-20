package at.simulevski.weatherinducer.content.resistor;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

/**
 * The SU Resistor: an inline shaft block that caps how much SU whatever is
 * hooked up through it may draw. It enforces the cap twice over:
 * <ul>
 *   <li>the mod's own consumers (Weather Inducer, SU Charger) respect it in
 *       the SU draw walk (see
 *       {@code at.simulevski.weatherinducer.network.SUNetwork});</li>
 *   <li>for real Create machines it acts as a circuit breaker: when the
 *       stress demand downstream of it exceeds the cap, it trips and cuts
 *       rotation to that side like a clutch, retrying after a moment (see
 *       {@link SUResistorBlockEntity}).</li>
 * </ul>
 * The detach/re-attach plumbing below mirrors Create's own GearshiftBlock.
 */
public class SUResistorBlock extends RotatedPillarKineticBlock
        implements IBE<SUResistorBlockEntity> {

    /** Breaker state: tripped resistors do not pass rotation downstream. */
    public static final BooleanProperty TRIPPED = BooleanProperty.create("tripped");

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
        registerDefaultState(defaultBlockState().setValue(TRIPPED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TRIPPED);
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

    /**
     * Rips the block out of the rotation graph so it re-propagates with the
     * current TRIPPED state; same trick as Create's GearshiftBlock.
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
