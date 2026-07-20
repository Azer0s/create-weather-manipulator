package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.content.lightning.LightningMediumBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

/**
 * Draws the end crystal inside the Lightning Medium's glass shell, animated
 * exactly like the real entity: the three nested cubes tilt 60 degrees on
 * the diagonal, counter-rotate around Y and bob on the same eased sine the
 * vanilla {@code EndCrystalRenderer} uses. Everything is scaled down and
 * lifted so the crystal floats between the beacon base and the glass
 * ceiling. The animation clock is the level's game time, so every medium in
 * view bobs in phase, like a row of end pillars.
 */
public class LightningMediumRenderer implements BlockEntityRenderer<LightningMediumBlockEntity> {

    private static final ResourceLocation END_CRYSTAL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(END_CRYSTAL_TEXTURE);
    private static final float SIN_45 = (float) Math.sin(Math.PI / 4);
    private static final float TILT = (float) (Math.PI / 3);

    private final ModelPart cube;
    private final ModelPart glass;

    public LightningMediumRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart crystal = context.bakeLayer(ModelLayers.END_CRYSTAL);
        this.glass = crystal.getChild("glass");
        this.cube = crystal.getChild("cube");
    }

    @Override
    public void render(LightningMediumBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        float time = (level.getGameTime() % 24000L) + partialTick;

        // Vanilla's eased bob: 0 to 0.8, spending most of its time low.
        float bob = Mth.sin(time * 0.2f) / 2f + 0.5f;
        bob = (bob * bob + bob) * 0.4f;
        float spin = time * 3f;

        VertexConsumer buffer = buffers.getBuffer(RENDER_TYPE);
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        // Float above the obsidian pedestal (4px tall), below the glass roof.
        poseStack.translate(0.5f, 0.62f + bob * 0.13f, 0.5f);
        poseStack.scale(0.55f, 0.55f, 0.55f);

        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(new Quaternionf().setAngleAxis(TILT, SIN_45, 0.0f, SIN_45));
        glass.render(poseStack, buffer, packedLight, overlay);

        poseStack.scale(0.875f, 0.875f, 0.875f);
        poseStack.mulPose(new Quaternionf().setAngleAxis(TILT, SIN_45, 0.0f, SIN_45));
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        glass.render(poseStack, buffer, packedLight, overlay);

        poseStack.scale(0.875f, 0.875f, 0.875f);
        poseStack.mulPose(new Quaternionf().setAngleAxis(TILT, SIN_45, 0.0f, SIN_45));
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        cube.render(poseStack, buffer, packedLight, overlay);

        poseStack.popPose();
    }
}
