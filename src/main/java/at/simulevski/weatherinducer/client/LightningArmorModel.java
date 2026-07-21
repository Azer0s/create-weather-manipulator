package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.NoSuchElementException;

/**
 * The worn lightning armor: a winged silver helm with a tall gold crest,
 * cheek guards and double wing feathers, six raised discs across the
 * chest, layered pauldrons, ridged vambraces, a gold belt with hanging
 * tassets, a fluttering red cape, thigh plates and knees on the
 * leggings, and cuffed boots with toe caps and swept Hermes wings at the
 * ankles.
 *
 * <p>The base humanoid boxes are inflated (a full pixel on the outer
 * bake, half on the inner), so every decoration carries its own
 * {@link CubeDeformation} sized to clear that surface; decorations
 * placed at raw coordinates end up buried inside the inflated base or
 * coplanar with it, which is where z-fighting comes from.
 *
 * <p>Two bakes exist, mirroring vanilla armor: the outer model (helmet,
 * chest, boots, {@code lightning_layer_1}) and the inner model
 * (leggings, {@code lightning_layer_2}). Decorations live as children of
 * the standard humanoid parts, so they follow the pose and per-slot
 * visibility for free; the cape gets an extra sway in
 * {@link #setupAnim}.
 */
public class LightningArmorModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation INNER = new ModelLayerLocation(
            WeatherInducerMod.asResource("lightning_armor"), "inner");
    public static final ModelLayerLocation OUTER = new ModelLayerLocation(
            WeatherInducerMod.asResource("lightning_armor"), "outer");

    private final ModelPart cape;

    public LightningArmorModel(ModelPart root) {
        super(root);
        ModelPart foundCape;
        try {
            foundCape = root.getChild("body").getChild("cape");
        } catch (NoSuchElementException e) {
            foundCape = null;
        }
        this.cape = foundCape;
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (cape == null) {
            return;
        }
        // The genuine Minecraft cape: driven by how far the wearer has
        // actually moved and turned since last tick, exactly like
        // CapeLayer, so it trails and settles instead of pivoting on a
        // sine wave. Only players carry the cloak-lag fields; anything
        // else just gets the resting hang.
        if (entity instanceof AbstractClientPlayer player) {
            float partial = Mth.clamp(ageInTicks - player.tickCount, 0.0f, 1.0f);
            double dx = Mth.lerp(partial, player.xCloakO, player.xCloak)
                    - Mth.lerp(partial, player.xo, player.getX());
            double dy = Mth.lerp(partial, player.yCloakO, player.yCloak)
                    - Mth.lerp(partial, player.yo, player.getY());
            double dz = Mth.lerp(partial, player.zCloakO, player.zCloak)
                    - Mth.lerp(partial, player.zo, player.getZ());
            float bodyRot = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
            double sin = Mth.sin(bodyRot * ((float) Math.PI / 180f));
            double cos = -Mth.cos(bodyRot * ((float) Math.PI / 180f));
            float lift = (float) dy * 10f;
            lift = Mth.clamp(lift, -6f, 32f);
            float swing = (float) (dx * sin + dz * cos) * 100f;
            swing = Mth.clamp(swing, 0f, 150f);
            float sway = (float) (dx * cos - dz * sin) * 100f;
            sway = Mth.clamp(sway, -20f, 20f);
            float bob = Mth.lerp(partial, player.oBob, player.bob);
            lift += Mth.sin(Mth.lerp(partial, player.walkDistO, player.walkDist) * 6f) * 32f * bob;
            if (player.isCrouching()) {
                lift += 25f;
            }
            cape.xRot = (6f + swing / 2f + lift) * ((float) Math.PI / 180f);
            cape.zRot = (sway / 2f) * ((float) Math.PI / 180f);
            cape.yRot = 0f;
        } else {
            cape.xRot = 6f * ((float) Math.PI / 180f);
            cape.zRot = 0f;
            cape.yRot = 0f;
        }
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
        // Tall gold crest with a second ridge overlapping into it, so the
        // stack shows no coplanar seam.
        head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-1.0f, -12.6f, -4.5f, 2, 3, 9), PartPose.ZERO);
        head.addOrReplaceChild("crest_top", CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-0.5f, -13.6f, -3.5f, 1, 1.2f, 7), PartPose.ZERO);
        head.addOrReplaceChild("brow", CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-4.5f, -9.4f, -5.4f, 9, 2, 1, new CubeDeformation(0.6f)),
                PartPose.ZERO);
        head.addOrReplaceChild("cheek_right", CubeListBuilder.create()
                .texOffs(48, 32)
                .addBox(-5.4f, -7.4f, -4.9f, 1, 3, 3, new CubeDeformation(0.3f)),
                PartPose.ZERO);
        head.addOrReplaceChild("cheek_left", CubeListBuilder.create()
                .texOffs(48, 32).mirror()
                .addBox(4.4f, -7.4f, -4.9f, 1, 3, 3, new CubeDeformation(0.3f)),
                PartPose.ZERO);
        head.addOrReplaceChild("wing_right", CubeListBuilder.create()
                        .texOffs(24, 36)
                        .addBox(-1.0f, -2.5f, -3.0f, 1, 5, 6),
                PartPose.offsetAndRotation(-5.2f, -6.0f, 0.5f, 0.0f, 0.35f, 0.22f));
        head.addOrReplaceChild("wing_left", CubeListBuilder.create()
                        .texOffs(24, 36).mirror()
                        .addBox(0.0f, -2.5f, -3.0f, 1, 5, 6),
                PartPose.offsetAndRotation(5.2f, -6.0f, 0.5f, 0.0f, -0.35f, -0.22f));
        head.addOrReplaceChild("feather_right", CubeListBuilder.create()
                        .texOffs(56, 16)
                        .addBox(-1.0f, -3.4f, -1.2f, 1, 3, 3),
                PartPose.offsetAndRotation(-5.3f, -6.2f, 1.6f, 0.0f, 0.5f, 0.35f));
        head.addOrReplaceChild("feather_left", CubeListBuilder.create()
                        .texOffs(56, 16).mirror()
                        .addBox(0.0f, -3.4f, -1.2f, 1, 3, 3),
                PartPose.offsetAndRotation(5.3f, -6.2f, 1.6f, 0.0f, -0.5f, -0.35f));
    }

    private static void addChestDetail(PartDefinition body) {
        // Six raised discs, two shrinking columns.
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
        // Belt grown clear of the inflated torso, tassets in front of it.
        body.addOrReplaceChild("belt", CubeListBuilder.create()
                .texOffs(0, 49)
                .addBox(-4.6f, 10.0f, -3.1f, 9, 2, 6, new CubeDeformation(0.7f)),
                PartPose.ZERO);
        float[] tassetX = {-3.6f, -1.0f, 1.6f};
        for (int i = 0; i < tassetX.length; i++) {
            body.addOrReplaceChild("tasset_" + i, CubeListBuilder.create()
                    .texOffs(56, 24)
                    .addBox(tassetX[i], 12.4f, -4.2f, 2, 3, 1), PartPose.ZERO);
        }
        // A vanilla cape: the game's own cape geometry (10 wide, 16 tall)
        // pivoted at the shoulders. The offset must clear the inflated
        // body box (whose back face sits near z 3.5) or the cape renders
        // buried inside the torso and shows nothing, so it hangs a little
        // further back.
        body.addOrReplaceChild("cape", CubeListBuilder.create()
                        .texOffs(30, 42)
                        .addBox(-5.0f, 0, 0.0f, 10, 16, 1),
                PartPose.offset(0, 0.0f, 3.6f));
    }

    private static void addArmDetail(PartDefinition root) {
        // Grown clear of the arm's one pixel inflation.
        CubeDeformation pad = new CubeDeformation(0.9f);
        root.getChild("right_arm").addOrReplaceChild("pauldron", CubeListBuilder.create()
                .texOffs(44, 39)
                .addBox(-3.8f, -3.4f, -2.5f, 5, 4, 5, pad), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("pauldron_ridge", CubeListBuilder.create()
                .texOffs(44, 39)
                .addBox(-3.4f, -4.6f, -2.0f, 4, 1, 4, new CubeDeformation(0.4f)),
                PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron", CubeListBuilder.create()
                .texOffs(44, 39).mirror()
                .addBox(-1.2f, -3.4f, -2.5f, 5, 4, 5, pad), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron_ridge", CubeListBuilder.create()
                .texOffs(44, 39).mirror()
                .addBox(-0.6f, -4.6f, -2.0f, 4, 1, 4, new CubeDeformation(0.4f)),
                PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("vambrace", CubeListBuilder.create()
                .texOffs(34, 57)
                .addBox(-3.6f, 7.6f, -2.4f, 5, 2, 5, pad), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("vambrace", CubeListBuilder.create()
                .texOffs(34, 57).mirror()
                .addBox(-1.4f, 7.6f, -2.4f, 5, 2, 5, pad), PartPose.ZERO);
    }

    private static void addBootsDetail(PartDefinition root) {
        CubeDeformation pad = new CubeDeformation(0.9f);
        for (String side : new String[]{"right_leg", "left_leg"}) {
            boolean mirror = side.startsWith("left");
            PartDefinition leg = root.getChild(side);
            CubeListBuilder cuff = CubeListBuilder.create().texOffs(0, 57);
            CubeListBuilder toe = CubeListBuilder.create().texOffs(20, 57);
            CubeListBuilder wing = CubeListBuilder.create().texOffs(24, 36);
            CubeListBuilder feather = CubeListBuilder.create().texOffs(56, 16);
            if (mirror) {
                cuff.mirror();
                toe.mirror();
                wing.mirror();
                feather.mirror();
            }
            leg.addOrReplaceChild("cuff",
                    cuff.addBox(-2.5f, 7.4f, -2.5f, 5, 2, 5, pad), PartPose.ZERO);
            leg.addOrReplaceChild("toe",
                    toe.addBox(-2.5f, 10.2f, -3.6f, 5, 2, 2, new CubeDeformation(0.6f)),
                    PartPose.ZERO);
            // Hermes wings: a swept feather fan off the outer ankle,
            // angled up and back, with a smaller trailing feather.
            float out = mirror ? 3.1f : -4.1f;
            float yaw = mirror ? -0.35f : 0.35f;
            leg.addOrReplaceChild("wing",
                    wing.addBox(0, -5, 0, 1, 5, 6),
                    PartPose.offsetAndRotation(out, 10.0f, 0.5f, -0.85f, yaw, 0));
            leg.addOrReplaceChild("wing_feather",
                    feather.addBox(0, -3, 0, 1, 3, 3),
                    PartPose.offsetAndRotation(out, 9.4f, 1.6f, -1.1f, yaw, 0));
        }
    }

    private static void addLeggingsDetail(PartDefinition root) {
        root.getChild("body").addOrReplaceChild("hip", CubeListBuilder.create()
                .texOffs(12, 34)
                .addBox(-4.5f, 10.6f, -2.6f, 9, 3, 5, new CubeDeformation(0.6f)),
                PartPose.ZERO);
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
                    knee.addBox(-2.0f, 5.6f, -3.0f, 4, 3, 1, new CubeDeformation(0.3f)),
                    PartPose.ZERO);
            leg.addOrReplaceChild("thigh",
                    thigh.addBox(-2.0f, 1.0f, -3.2f, 4, 4, 1, new CubeDeformation(0.3f)),
                    PartPose.ZERO);
            leg.addOrReplaceChild("shin",
                    shin.addBox(-1.0f, 9.0f, -3.0f, 2, 3, 1, new CubeDeformation(0.3f)),
                    PartPose.ZERO);
        }
    }
}
