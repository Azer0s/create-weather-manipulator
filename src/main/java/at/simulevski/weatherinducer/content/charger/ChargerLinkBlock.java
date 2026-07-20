package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import at.simulevski.weatherinducer.registry.ModBlocks;
import at.simulevski.weatherinducer.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The Charger Link: a small panel bolted onto a Kinetic Charger, in the
 * spirit of Create's display link. The first link placed from a fresh item
 * founds a new charger network; right-clicking any placed link with more
 * link items in hand binds that stack to its network, and every link
 * placed from a bound stack joins the same one. Chargers wearing links of
 * one network balance their buffers and read as a single big battery.
 */
public class ChargerLinkBlock extends Block implements EntityBlock {

    /** Points away from the charger the link is bolted to. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    /** The item tag key carrying a bound stack's network id. */
    public static final String NETWORK_KEY = "ChargerNetwork";

    // Indexed by Direction ordinal; the mounting plate hugs the host
    // charger's face, which sits behind (opposite) the FACING direction.
    // Covers the display-link style plate; the thin antenna above it is
    // left out of the outline on purpose.
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(1, 10, 1, 15, 16, 15),   // DOWN: host above
            Block.box(1, 0, 1, 15, 6, 15),     // UP: host below
            Block.box(1, 1, 10, 15, 15, 16),   // NORTH: host south
            Block.box(1, 1, 0, 15, 15, 6),     // SOUTH: host north
            Block.box(10, 1, 1, 16, 15, 15),   // WEST: host east
            Block.box(0, 1, 1, 6, 15, 15),     // EAST: host west
    };

    public ChargerLinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, face);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos host = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(host).is(ModBlocks.KINETIC_CHARGER.get());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide
                || !(level.getBlockEntity(pos) instanceof ChargerLinkBlockEntity link)) {
            return;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        UUID network = null;
        if (data != null && data.copyTag().hasUUID(NETWORK_KEY)) {
            network = data.copyTag().getUUID(NETWORK_KEY);
        }
        // A fresh, unbound item founds a brand new charger network.
        link.setNetwork(network != null ? network : UUID.randomUUID());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(ModItems.CHARGER_LINK.get())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof ChargerLinkBlockEntity link
                && link.getNetwork() != null) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(NETWORK_KEY, link.getNetwork());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.displayClientMessage(
                    Component.translatable("weatherinducer.message.link_bound"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargerLinkBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHARGER_LINK.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ChargerLinkBlockEntity.tick(lvl, pos, st,
                (ChargerLinkBlockEntity) be);
    }
}
