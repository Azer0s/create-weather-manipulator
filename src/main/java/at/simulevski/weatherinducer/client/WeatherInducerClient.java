package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.ponder.ModPonderPlugin;
import at.simulevski.weatherinducer.registry.ModBlockEntities;
import at.simulevski.weatherinducer.registry.ModEntityTypes;
import at.simulevski.weatherinducer.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Quaternionf;

/**
 * Client-only setup for the kinetic blocks' spinning shafts.
 *
 * <p>Two render paths exist and both need registering. With Flywheel's
 * backend active (Create's default), kinetic block entities render through
 * Flywheel visuals and {@code KineticBlockEntityRenderer} bails out early,
 * so each block registers a visual: the plain rotating shaft for the
 * inducer and charger, the two-half split shaft for the gate (its halves
 * can spin at different speeds while locked). With
 * Flywheel off, the classic block entity renderers below take over.
 */
@EventBusSubscriber(modid = WeatherInducerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeatherInducerClient {

    private WeatherInducerClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WEATHER_INDUCER.get(),
                context -> new ShaftRenderer<>(context));
        // The gate is a split shaft: its two halves can turn at different
        // speeds while locked, so it gets the matching renderer.
        event.registerBlockEntityRenderer(ModBlockEntities.KINETIC_CHARGER.get(),
                context -> new ShaftRenderer<>(context));
        event.registerBlockEntityRenderer(ModBlockEntities.STRESS_GATE.get(),
                context -> new SplitShaftRenderer(context));
        event.registerBlockEntityRenderer(ModBlockEntities.LIGHTNING_MEDIUM.get(),
                LightningMediumRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGER_LINK.get(),
                ChargerLinkRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.THROWN_BOTTLE_O_LIGHTNING.get(),
                ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LightningArmorModel.INNER,
                LightningArmorModel::createInnerLayer);
        event.registerLayerDefinition(LightningArmorModel.OUTER,
                LightningArmorModel::createOuterLayer);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        // Swap the flat generated armor for the modelled one: the leggings
        // use the inner bake, everything else the outer.
        event.registerItem(new IClientItemExtensions() {
            private LightningArmorModel inner;
            private LightningArmorModel outer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> original) {
                EntityModelSet models = Minecraft.getInstance().getEntityModels();
                if (slot == EquipmentSlot.LEGS) {
                    if (inner == null) {
                        inner = new LightningArmorModel(models.bakeLayer(LightningArmorModel.INNER));
                    }
                    return inner;
                }
                if (outer == null) {
                    outer = new LightningArmorModel(models.bakeLayer(LightningArmorModel.OUTER));
                }
                return outer;
            }
        }, ModItems.LIGHTNING_HELMET.get(), ModItems.LIGHTNING_CHESTPLATE.get(),
                ModItems.LIGHTNING_LEGGINGS.get(), ModItems.LIGHTNING_BOOTS.get());

        // The katana swings its own way: a flat horizontal slash across
        // the view instead of vanilla's diagonal chop. Returning true
        // takes over the base hand transform and the swing, so both are
        // rebuilt here; the blocking pose still runs through vanilla's
        // separate use-animation path.
        event.registerItem(new IClientItemExtensions() {
            // The slash runs on its own clock so it can be slower than the
            // six-tick attack swing, which flicked past too fast. A new
            // swing (re)starts it; it then plays over SLASH_TICKS.
            private static final float SLASH_TICKS = 10f;
            private float slashStart = -1f;
            private int prevSwingTime = -1;
            private boolean prevSwinging = false;

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player,
                                                   HumanoidArm arm, ItemStack itemInHand,
                                                   float partialTick, float equipProcess,
                                                   float swingProcess) {
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                // The guard: the blade raised across the face on the
                // bottom-right to top-left diagonal, tip up. No end-over
                // flip (that turned it upside down); the edge sits forward
                // from the diagonal alone. The stance is alive: it snaps
                // up into guard over a few ticks, then breathes with a slow
                // idle sway and a faster ready-tremor so it never freezes.
                if (player != null && player.isUsingItem()
                        && player.getUseItem().is(ModItems.LIGHTNING_SWORD.get())) {
                    float held = player.getTicksUsingItem() + partialTick;
                    float raise = smooth(clamp01(held / 4f));
                    float time = player.tickCount + partialTick;
                    float breathe = Mth.sin(time * 0.12f) * 2.5f;
                    float tremor = Mth.sin(time * 0.55f) * 0.9f;
                    float sway = breathe + tremor;

                    // Parry recoil: when a blow actually lands on the
                    // guard the client flags it with hurtTime, so the blade
                    // kicks back and shudders, a distinct second animation
                    // on top of the idle stance.
                    float parry = player.hurtTime > 0
                            ? player.hurtTime / (float) Math.max(1, player.hurtDuration) : 0;
                    float kick = smooth(parry);
                    float shake = Mth.sin(time * 1.6f) * kick * 6f;

                    poseStack.translate(
                            side * (0.02f - kick * 0.12f),
                            -0.18f - (1f - raise) * 0.5f + kick * 0.06f,
                            -0.58f + (1f - raise) * 0.3f + kick * 0.14f);
                    // Roll onto the diagonal, tip up-left (grip bottom
                    // right), easing in with the raise, then the living
                    // sway and any parry shake.
                    poseStack.mulPose(Axis.ZP.rotationDegrees(
                            side * (58f * raise + sway + kick * 24f + shake)));
                    poseStack.mulPose(Axis.XP.rotationDegrees(
                            -10f * raise + sway * 0.4f - kick * 18f));
                    poseStack.mulPose(Axis.YP.rotationDegrees(side * (18f * raise + shake)));
                    return true;
                }
                // The slash runs on its own clock. A fresh swing (rising
                // edge of swinging, or swingTime resetting during rapid
                // clicks) starts it; the renderer's swingProcess comes back
                // zero here, so the entity swing fields drive detection.
                float now = player == null ? 0 : player.tickCount + partialTick;
                if (player != null) {
                    boolean newSwing = player.swinging
                            && (!prevSwinging || player.swingTime < prevSwingTime);
                    if (newSwing && (slashStart < 0 || now - slashStart >= SLASH_TICKS)) {
                        slashStart = now;
                    }
                    prevSwinging = player.swinging;
                    prevSwingTime = player.swinging ? player.swingTime : -1;
                }
                float swing = slashStart < 0 ? 1f
                        : clamp01((now - slashStart) / SLASH_TICKS);

                // Three eased phases. The blade is long and already sits
                // at the right edge at rest, so the motion only ever moves
                // it INWARD (never further out, which is what threw the tip
                // off screen) and keeps the vertical travel tiny:
                //   wind-up (0 .. 0.3) cocks back with a small rotation
                //   sweep   (0.3 .. 0.75) draws inward across the view
                //   recover (0.75 .. 1) eases back to rest
                float wind = smooth(clamp01(swing / 0.3f));
                float sweep = smooth(clamp01((swing - 0.3f) / 0.45f));
                float recover = smooth(clamp01((swing - 0.75f) / 0.25f));
                float active = 1f - recover;

                // acrossX is never positive: rest x = 0.42, so it can only
                // shrink toward and past centre, staying in frame.
                float acrossX = (-wind * 0.04f - sweep * 0.44f) * active;
                float riseY = (wind * 0.03f + sweep * 0.04f) * active;
                poseStack.translate(
                        side * (0.42f + acrossX),
                        -0.48f + equipProcess * -0.6f + riseY,
                        -0.86f - sweep * active * 0.06f);
                float yaw = (wind * 12f - sweep * 42f) * active;
                float roll = (-wind * 14f - sweep * 30f) * active;
                poseStack.mulPose(Axis.YP.rotationDegrees(side * yaw));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * roll));
                return true;
            }
        }, ModItems.LIGHTNING_SWORD.get());
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    /** Smoothstep easing, so each phase accelerates in and out of rest. */
    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PonderIndex.addPlugin(new ModPonderPlugin());

            // The inset shaft partial, not Create's full-length one: its
            // boundary end caps z-fight with attached shafts' own caps and
            // the junctions shimmer dark (see ModPartialModels.INNER_SHAFT).
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.WEATHER_INDUCER.get())
                    .factory(SingleAxisRotatingVisual.of(ModPartialModels.INNER_SHAFT))
                    .apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.KINETIC_CHARGER.get())
                    .factory(SingleAxisRotatingVisual.of(ModPartialModels.INNER_SHAFT))
                    .apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.STRESS_GATE.get())
                    .factory(SplitShaftVisual::new)
                    .apply();
        });
    }
}
