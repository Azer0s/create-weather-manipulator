package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.content.charger.ChargerLinkBlock;
import at.simulevski.weatherinducer.content.charger.ChargerLinkBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Charger Link's two lights. The big bulb is the status lamp: it
 * glows steadily in the host charger's state color (blue charging, red
 * discharging, green full, yellow idle). The little LED on the housing
 * is the sync indicator: it flashes red with the network heartbeat on
 * the display link's own pulse machinery, so every link of one network
 * blinks in unison and separate networks beat to their own rhythm.
 */
public class ChargerLinkRenderer extends SafeBlockEntityRenderer<ChargerLinkBlockEntity> {

    public ChargerLinkRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ChargerLinkBlockEntity link, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        BlockState state = link.getBlockState();
        Direction facing = state.getValue(ChargerLinkBlock.FACING);
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        // Mirror the blockstate rotations the directional model uses, so
        // the glows sit over their lenses on every mounting face.
        switch (facing) {
            case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(-180));
            case NORTH -> ms.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> {
                ms.mulPose(Axis.YP.rotationDegrees(-180));
                ms.mulPose(Axis.XP.rotationDegrees(-90));
            }
            case EAST -> {
                ms.mulPose(Axis.YP.rotationDegrees(-90));
                ms.mulPose(Axis.XP.rotationDegrees(-90));
            }
            case WEST -> {
                ms.mulPose(Axis.YP.rotationDegrees(-270));
                ms.mulPose(Axis.XP.rotationDegrees(-90));
            }
            default -> {
                // UP: the authored pose.
            }
        }
        ms.translate(-0.5, -0.5, -0.5);

        // The status lamp: steady, host-state tinted.
        int tint = link.getLampColor();
        float steady = 0.55f;
        CachedBuffers.partial(ModPartialModels.CHARGER_LINK_GLOW, state)
                .light(LightTexture.FULL_BRIGHT)
                .color((int) (((tint >> 16) & 0xFF) * steady),
                        (int) (((tint >> 8) & 0xFF) * steady),
                        (int) ((tint & 0xFF) * steady), 255)
                .disableDiffuse()
                .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));

        // The sync LED: the display link's brightness curve, in red.
        float glow = link.getGlow(partialTicks);
        if (glow >= 0.125f) {
            float brightness = Mth.clamp(
                    (float) (1 - 2 * Math.pow(glow - 0.75f, 2)), 0, 1);
            if (brightness > 0) {
                float scale = brightness * 200f / 255f;
                CachedBuffers.partial(ModPartialModels.CHARGER_LINK_SYNC_GLOW, state)
                        .light(LightTexture.FULL_BRIGHT)
                        .color((int) (255 * scale), (int) (70 * scale), (int) (60 * scale), 255)
                        .disableDiffuse()
                        .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
            }
        }
        ms.popPose();
    }
}
