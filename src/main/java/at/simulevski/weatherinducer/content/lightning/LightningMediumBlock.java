package at.simulevski.weatherinducer.content.lightning;

import net.minecraft.world.level.block.Block;

/**
 * The Lightning Medium: a beacon and an end crystal encased in glass. It does
 * nothing on its own; strike it with lightning and it shatters, dropping a
 * Bottle o' Lightning (see
 * {@link LightningGearHandler#onLightning}). A Weather Inducer set to
 * lightning mode with a matching offset makes a tidy bottling machine.
 */
public class LightningMediumBlock extends Block {

    public LightningMediumBlock(Properties properties) {
        super(properties);
    }
}
