package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * The worn lightning armor, with actual geometry on top of the usual
 * inflated player boxes: a gold crest and brow plate plus swept-back side
 * wings on the helmet, shoulder pauldrons, a raised chest emblem, a belt
 * and a cape plate on the chestplate, knee plates on the leggings, and
 * cuffs and toe caps on the boots.
 *
 * <p>Two bakes exist, mirroring vanilla armor: the outer model (helmet,
 * chest, boots, 1px inflation, drawn with {@code lightning_layer_1}) and
 * the inner model (leggings, half-pixel inflation, {@code
 * lightning_layer_2}). Decorations live as children of the standard
 * humanoid parts, so they follow the pose and the per-slot visibility that
 * {@code HumanoidArmorLayer} applies without any extra code.
 */
public class LightningArmorModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation INNER = new ModelLayerLocation(
            WeatherInducerMod.asResource("lightning_armor"), "inner");
    public static final ModelLayerLocation OUTER = new ModelLayerLocation(
            WeatherInducerMod.asResource("lightning_armor"), "outer");

    public LightningArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createInnerLayer() {
        return create(new CubeDeformation(0.5f), true);
    }

    public static LayerDefinition createOuterLayer() {
        return create(new CubeDeformation(1.0f), false);
    }

    private static LayerDefinition create(CubeDeformation inflation, boolean inner) {
        MeshDefinition mesh = HumanoidModel.createMesh(inflation, 0);
        PartDefinition root = mesh.getRoot();

        if (inner) {
            addLeggingsDetail(root);
        } else {
            addHelmetDetail(root.getChild("head"));
            addChestDetail(root.getChild("body"));
            addPauldrons(root);
            addBootsDetail(root);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addHelmetDetail(PartDefinition head) {
        // Gold crest running front to back over the crown.
        head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-1.0f, -12.2f, -4.5f, 2, 3, 9), PartPose.ZERO);
        // Brow plate above the face opening.
        head.addOrReplaceChild("brow", CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-4.5f, -9.4f, -5.4f, 9, 2, 1), PartPose.ZERO);
        // Swept-back wings on the temples, Thor style.
        head.addOrReplaceChild("wing_right", CubeListBuilder.create()
                        .texOffs(24, 36)
                        .addBox(-1.0f, -2.5f, -3.0f, 1, 5, 6),
                PartPose.offsetAndRotation(-4.8f, -6.0f, 0.5f, 0.0f, 0.35f, 0.22f));
        head.addOrReplaceChild("wing_left", CubeListBuilder.create()
                        .texOffs(24, 36).mirror()
                        .addBox(0.0f, -2.5f, -3.0f, 1, 5, 6),
                PartPose.offsetAndRotation(4.8f, -6.0f, 0.5f, 0.0f, -0.35f, -0.22f));
    }

    private static void addChestDetail(PartDefinition body) {
        // Raised gold emblem plate over the sternum.
        body.addOrReplaceChild("emblem", CubeListBuilder.create()
                .texOffs(40, 32)
                .addBox(-2.5f, 1.2f, -3.7f, 5, 5, 1), PartPose.ZERO);
        // Belt wrapping the waist.
        body.addOrReplaceChild("belt", CubeListBuilder.create()
                .texOffs(0, 49)
                .addBox(-4.6f, 10.0f, -3.1f, 9, 2, 6), PartPose.ZERO);
        // Cape plate hanging off the back.
        body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(30, 49)
                .addBox(-4.5f, 0.6f, 3.1f, 9, 12, 1), PartPose.ZERO);
    }

    private static void addPauldrons(PartDefinition root) {
        root.getChild("right_arm").addOrReplaceChild("pauldron", CubeListBuilder.create()
                .texOffs(44, 39)
                .addBox(-3.8f, -3.4f, -2.5f, 5, 4, 5), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron", CubeListBuilder.create()
                .texOffs(44, 39).mirror()
                .addBox(-1.2f, -3.4f, -2.5f, 5, 4, 5), PartPose.ZERO);
    }

    private static void addBootsDetail(PartDefinition root) {
        for (String side : new String[]{"right_leg", "left_leg"}) {
            boolean mirror = side.startsWith("left");
            PartDefinition leg = root.getChild(side);
            CubeListBuilder cuff = CubeListBuilder.create().texOffs(0, 57);
            CubeListBuilder toe = CubeListBuilder.create().texOffs(20, 57);
            if (mirror) {
                cuff.mirror();
                toe.mirror();
            }
            leg.addOrReplaceChild("cuff",
                    cuff.addBox(-2.5f, 7.4f, -2.5f, 5, 2, 5), PartPose.ZERO);
            leg.addOrReplaceChild("toe",
                    toe.addBox(-2.5f, 10.2f, -3.6f, 5, 2, 2), PartPose.ZERO);
        }
    }

    private static void addLeggingsDetail(PartDefinition root) {
        // Hip wrap under the belt line.
        root.getChild("body").addOrReplaceChild("hip", CubeListBuilder.create()
                .texOffs(12, 34)
                .addBox(-4.5f, 10.6f, -2.6f, 9, 3, 5), PartPose.ZERO);
        for (String side : new String[]{"right_leg", "left_leg"}) {
            boolean mirror = side.startsWith("left");
            CubeListBuilder knee = CubeListBuilder.create().texOffs(0, 34);
            if (mirror) {
                knee.mirror();
            }
            root.getChild(side).addOrReplaceChild("knee",
                    knee.addBox(-2.0f, 5.6f, -3.0f, 4, 3, 1), PartPose.ZERO);
        }
    }
}
