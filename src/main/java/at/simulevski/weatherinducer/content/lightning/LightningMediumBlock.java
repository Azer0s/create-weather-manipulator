package at.simulevski.weatherinducer.content.lightning;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Lightning Medium: a beacon and an end crystal encased in glass. It does
 * nothing on its own; strike it with lightning and it shatters, dropping a
 * Bottle o' Lightning (see
 * {@link LightningGearHandler#onLightning}). A Weather Inducer set to
 * lightning mode with a matching offset makes a tidy bottling machine.
 *
 * <p>The block entity exists purely for the client: its renderer draws the
 * end crystal bobbing and spinning inside the glass shell.
 */
public class LightningMediumBlock extends Block implements EntityBlock {

    public LightningMediumBlock(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LightningMediumBlockEntity(pos, state);
    }
}
