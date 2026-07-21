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
 * The worn lightning armor, ornate in the way the big screen dresses its
 * thunder god: a winged silver helm with a tall gold crest, cheek guards
 * and double wing feathers, six raised discs across the chest, layered
 * pauldrons, ridged vambraces, a gold belt with hanging tassets, a red
 * cape, thigh plates and knees on the leggings, and cuffed boots with
 * toe caps and heel fins.
 *
 * <p>Two bakes exist, mirroring vanilla armor: the outer model (helmet,
 * chest, boots, 1px inflation, drawn with {@code lightning_layer_1}) and
 * the inner model (leggings, half-pixel inflation, {@code
 * lightning_layer_2}). Decorations live as children of the standard
 * humanoid parts, so they follow the pose and the per-slot visibility
 * that {@code HumanoidArmorLayer} applies without any extra code.
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
            addArmDetail(root);
            addBootsDetail(root);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addHelmetDetail(PartDefinition head) {
        // Tall gold crest running front to back over the crown, with a
        // second lower ridge so it reads layered from the side.
        head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-1.0f, -12.6f, -4.5f, 2, 3, 9), PartPose.ZERO);
        head.addOrReplaceChild("crest_top", CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-0.5f, -13.6f, -3.5f, 1, 1, 7), PartPose.ZERO);
        // Brow plate above the face opening.
        head.addOrReplaceChild("brow", CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-4.5f, -9.4f, -5.4f, 9, 2, 1), PartPose.ZERO);
        // Cheek guards framing the face.
        head.addOrReplaceChild("cheek_right", CubeListBuilder.create()
                .texOffs(48, 32)
                .addBox(-5.4f, -7.4f, -4.9f, 1, 3, 3), PartPose.ZERO);
        head.addOrReplaceChild("cheek_left", CubeListBuilder.create()
                .texOffs(48, 32).mirror()
                .addBox(4.4f, -7.4f, -4.9f, 1, 3, 3), PartPose.ZERO);
        // Swept-back wings on the temples, two feathers each side.
        head.addOrReplaceChild("wing_right", CubeListBuilder.create()
                        .texOffs(24, 36)
                        .addBox(-1.0f, -2.5f, -3.0f, 1, 5, 6),
                PartPose.offsetAndRotation(-4.8f, -6.0f, 0.5f, 0.0f, 0.35f, 0.22f));
        head.addOrReplaceChild("wing_left", CubeListBuilder.create()
                        .texOffs(24, 36).mirror()
                        .addBox(0.0f, -2.5f, -3.0f, 1, 5, 6),
                PartPose.offsetAndRotation(4.8f, -6.0f, 0.5f, 0.0f, -0.35f, -0.22f));
        head.addOrReplaceChild("feather_right", CubeListBuilder.create()
                        .texOffs(56, 16)
                        .addBox(-1.0f, -3.4f, -1.2f, 1, 3, 3),
                PartPose.offsetAndRotation(-4.9f, -6.2f, 1.6f, 0.0f, 0.5f, 0.35f));
        head.addOrReplaceChild("feather_left", CubeListBuilder.create()
                        .texOffs(56, 16).mirror()
                        .addBox(0.0f, -3.4f, -1.2f, 1, 3, 3),
                PartPose.offsetAndRotation(4.9f, -6.2f, 1.6f, 0.0f, -0.5f, -0.35f));
    }

    private static void addChestDetail(PartDefinition body) {
        // Six raised silver discs across the chest, the thunder god's
        // signature, two columns of three, shrinking downward.
        float[][] discs = {
                {-3.4f, 0.6f}, {1.4f, 0.6f},
                {-3.0f, 3.4f}, {1.0f, 3.4f},
                {-2.6f, 6.0f}, {0.6f, 6.0f},
        };
        for (int i = 0; i < discs.length; i++) {
            body.addOrReplaceChild("disc_" + i, CubeListBuilder.create()
                    .texOffs(40, 32)
                    .addBox(discs[i][0], discs[i][1], -3.7f, 2, 2, 1), PartPose.ZERO);
        }
        // Belt wrapping the waist, with three hanging tassets.
        body.addOrReplaceChild("belt", CubeListBuilder.create()
                .texOffs(0, 49)
                .addBox(-4.6f, 10.0f, -3.1f, 9, 2, 6), PartPose.ZERO);
        float[] tassetX = {-3.6f, -1.0f, 1.6f};
        for (int i = 0; i < tassetX.length; i++) {
            body.addOrReplaceChild("tasset_" + i, CubeListBuilder.create()
                    .texOffs(56, 24)
                    .addBox(tassetX[i], 12.0f, -3.4f, 2, 3, 1), PartPose.ZERO);
        }
        // The red cape hanging off the back.
        body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(30, 49)
                .addBox(-4.5f, 0.6f, 3.1f, 9, 12, 1), PartPose.ZERO);
    }

    private static void addArmDetail(PartDefinition root) {
        // Layered pauldrons: the main plate plus a raised ridge on top.
        root.getChild("right_arm").addOrReplaceChild("pauldron", CubeListBuilder.create()
                .texOffs(44, 39)
                .addBox(-3.8f, -3.4f, -2.5f, 5, 4, 5), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("pauldron_ridge", CubeListBuilder.create()
                .texOffs(44, 39)
                .addBox(-3.4f, -4.2f, -2.0f, 4, 1, 4), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron", CubeListBuilder.create()
                .texOffs(44, 39).mirror()
                .addBox(-1.2f, -3.4f, -2.5f, 5, 4, 5), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron_ridge", CubeListBuilder.create()
                .texOffs(44, 39).mirror()
                .addBox(-0.6f, -4.2f, -2.0f, 4, 1, 4), PartPose.ZERO);
        // Ridged vambraces at the wrists.
        root.getChild("right_arm").addOrReplaceChild("vambrace", CubeListBuilder.create()
                .texOffs(34, 57)
                .addBox(-3.6f, 7.6f, -2.4f, 5, 2, 5), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("vambrace", CubeListBuilder.create()
                .texOffs(34, 57).mirror()
                .addBox(-1.4f, 7.6f, -2.4f, 5, 2, 5), PartPose.ZERO);
    }

    private static void addBootsDetail(PartDefinition root) {
        for (String side : new String[]{"right_leg", "left_leg"}) {
            boolean mirror = side.startsWith("left");
            PartDefinition leg = root.getChild(side);
            CubeListBuilder cuff = CubeListBuilder.create().texOffs(0, 57);
            CubeListBuilder toe = CubeListBuilder.create().texOffs(20, 57);
            CubeListBuilder heel = CubeListBuilder.create().texOffs(56, 29);
            if (mirror) {
                cuff.mirror();
                toe.mirror();
                heel.mirror();
            }
            leg.addOrReplaceChild("cuff",
                    cuff.addBox(-2.5f, 7.4f, -2.5f, 5, 2, 5), PartPose.ZERO);
            leg.addOrReplaceChild("toe",
                    toe.addBox(-2.5f, 10.2f, -3.6f, 5, 2, 2), PartPose.ZERO);
            // A little winged fin off the heel.
            leg.addOrReplaceChild("heel",
                    heel.addBox(-0.5f, 8.6f, 2.4f, 1, 2, 3), PartPose.ZERO);
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
            CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 40);
            CubeListBuilder shin = CubeListBuilder.create().texOffs(12, 44);
            if (mirror) {
                knee.mirror();
                thigh.mirror();
                shin.mirror();
            }
            PartDefinition leg = root.getChild(side);
            leg.addOrReplaceChild("knee",
                    knee.addBox(-2.0f, 5.6f, -3.0f, 4, 3, 1), PartPose.ZERO);
            leg.addOrReplaceChild("thigh",
                    thigh.addBox(-2.0f, 1.0f, -3.2f, 4, 4, 1), PartPose.ZERO);
            leg.addOrReplaceChild("shin",
                    shin.addBox(-1.0f, 9.0f, -3.0f, 2, 3, 1), PartPose.ZERO);
        }
    }
}
