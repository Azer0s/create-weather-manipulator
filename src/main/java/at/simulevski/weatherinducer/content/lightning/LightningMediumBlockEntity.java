package at.simulevski.weatherinducer.content.lightning;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds no data and does not tick; it exists so the client can attach the
 * renderer that draws the bobbing end crystal inside the glass. The
 * animation runs off the level's game time, so nothing needs saving.
 */
public class LightningMediumBlockEntity extends BlockEntity {

    public LightningMediumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIGHTNING_MEDIUM.get(), pos, state);
    }
}
