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
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player,
                                                   HumanoidArm arm, ItemStack itemInHand,
                                                   float partialTick, float equipProcess,
                                                   float swingProcess) {
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                // The guard: the blade held at a forty five degree angle
                // in front of the face, leaning left, spun half a turn
                // about its own diagonal so the cutting edge faces the
                // threat instead of the wielder.
                if (player != null && player.isUsingItem()
                        && player.getUseItem().is(ModItems.LIGHTNING_SWORD.get())) {
                    poseStack.translate(side * 0.02f, -0.18f, -0.58f);
                    // The blade's natural pose runs about seventy degrees
                    // up-right; another sixty five of roll mirrors it onto
                    // the bottom-right to top-left diagonal.
                    poseStack.mulPose(Axis.ZP.rotationDegrees(side * 65));
                    poseStack.mulPose(Axis.YP.rotationDegrees(side * 10));
                    // Half a turn about the blade's own diagonal (now the
                    // up-left one) keeps the cutting edge facing forward.
                    poseStack.mulPose(new Quaternionf().rotationAxis(
                            (float) Math.PI, side * -0.7071f, 0.7071f, 0));
                    return true;
                }
                // Don't trust the passed swing value; read the attack
                // animation straight off the player so the slash always
                // has the real curve to work with.
                float swing = swingProcess;
                if (player != null) {
                    swing = Math.max(swing, player.getAttackAnim(partialTick));
                }
                // Rotate first, then move to the hand: the whole blade
                // orbits the view in a wide flat arc across the screen
                // instead of pivoting invisibly around its own grip.
                if (swing > 0) {
                    float sweep = Mth.sin(swing * (float) Math.PI);
                    float wind = Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
                    poseStack.mulPose(Axis.YP.rotationDegrees(side * wind * -50));
                    poseStack.mulPose(Axis.XP.rotationDegrees(sweep * 12));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(side * sweep * -55));
                }
                poseStack.translate(side * 0.42f, -0.48f + equipProcess * -0.6f, -0.86f);
                return true;
            }
        }, ModItems.LIGHTNING_SWORD.get());
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
