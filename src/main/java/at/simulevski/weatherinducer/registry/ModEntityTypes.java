package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.lightning.ThrownBottleOLightning;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, WeatherInducerMod.MOD_ID);

    public static final Supplier<EntityType<ThrownBottleOLightning>> THROWN_BOTTLE_O_LIGHTNING =
            ENTITY_TYPES.register("thrown_bottle_o_lightning",
                    () -> EntityType.Builder.<ThrownBottleOLightning>of(
                                    ThrownBottleOLightning::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("thrown_bottle_o_lightning"));

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
