package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.ponder.ModPonderPlugin;
import at.simulevski.weatherinducer.registry.ModBlockEntities;
import at.simulevski.weatherinducer.registry.ModEntityTypes;
import at.simulevski.weatherinducer.registry.ModItems;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

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
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PonderIndex.addPlugin(new ModPonderPlugin());

            SimpleBlockEntityVisualizer.builder(ModBlockEntities.WEATHER_INDUCER.get())
                    .factory(SingleAxisRotatingVisual::shaft)
                    .apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.KINETIC_CHARGER.get())
                    .factory(SingleAxisRotatingVisual::shaft)
                    .apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.STRESS_GATE.get())
                    .factory(SplitShaftVisual::new)
                    .apply();
        });
    }
}
