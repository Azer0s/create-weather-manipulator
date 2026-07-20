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
 * The Charger Link's heartbeat, rendered the way the display link does
 * it: the block entity's pulse drives a lerped glow value, and while it
 * runs, the inflated bulb partial is drawn fullbright into the additive
 * layer with the display link's own brightness curve. Every link of one
 * network pulses in unison; separate networks beat to their own rhythm.
 */
public class ChargerLinkRenderer extends SafeBlockEntityRenderer<ChargerLinkBlockEntity> {

    public ChargerLinkRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ChargerLinkBlockEntity link, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        float glow = link.getGlow(partialTicks);
        if (glow < 0.125f) {
            return;
        }
        float brightness = Mth.clamp(
                (float) (1 - 2 * Math.pow(glow - 0.75f, 2)), -1, 1);
        int value = (int) (200 * brightness);
        if (value <= 0) {
            return;
        }

        BlockState state = link.getBlockState();
        Direction facing = state.getValue(ChargerLinkBlock.FACING);
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        // Mirror the blockstate rotations the directional model uses, so
        // the glow sits over the bulb on every mounting face.
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
        CachedBuffers.partial(ModPartialModels.CHARGER_LINK_GLOW, state)
                .light(LightTexture.FULL_BRIGHT)
                .color(value, value, value, 255)
                .disableDiffuse()
                .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
        ms.popPose();
    }
}
