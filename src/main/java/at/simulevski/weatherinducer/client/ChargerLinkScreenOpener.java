package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.content.charger.ChargerLinkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client-only bridge: the block's right click handler calls this behind
 * an isClientSide check, so the screen classes never load on a server.
 */
public final class ChargerLinkScreenOpener {

    private ChargerLinkScreenOpener() {
    }

    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null
                && mc.level.getBlockEntity(pos) instanceof ChargerLinkBlockEntity link) {
            mc.setScreen(new ChargerLinkScreen(pos, link.getDisplayName()));
        }
    }
}
