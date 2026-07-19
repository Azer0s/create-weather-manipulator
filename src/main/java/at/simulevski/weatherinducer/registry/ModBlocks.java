package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlock;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WeatherInducerMod.MOD_ID);

    public static final DeferredBlock<WeatherInducerBlock> WEATHER_INDUCER = BLOCKS.register(
            "weather_inducer",
            () -> new WeatherInducerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    // The model is a stepped machine (casing base + raised
                    // emitter cap), not a full cube.
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<SUResistorBlock> SU_RESISTOR = BLOCKS.register(
            "su_resistor",
            () -> new SUResistorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f, 4.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
