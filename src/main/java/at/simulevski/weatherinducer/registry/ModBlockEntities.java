package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlockEntity;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WeatherInducerMod.MOD_ID);

    public static final Supplier<BlockEntityType<WeatherInducerBlockEntity>> WEATHER_INDUCER =
            BLOCK_ENTITIES.register("weather_inducer",
                    () -> BlockEntityType.Builder.of(
                            WeatherInducerBlockEntity::new,
                            ModBlocks.WEATHER_INDUCER.get()).build(null));

    public static final Supplier<BlockEntityType<SUResistorBlockEntity>> SU_RESISTOR =
            BLOCK_ENTITIES.register("su_resistor",
                    () -> BlockEntityType.Builder.of(
                            SUResistorBlockEntity::new,
                            ModBlocks.SU_RESISTOR.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
