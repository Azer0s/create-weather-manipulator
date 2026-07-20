package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.SUChargerBlock;
import at.simulevski.weatherinducer.content.gate.StressGateBlock;
import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlock;
import at.simulevski.weatherinducer.content.lightning.LightningMediumBlock;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlock;
import at.simulevski.weatherinducer.content.sensor.WeatherSensorBlock;
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
                    // Flanged resistor silhouette, not a full cube.
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<WeatherSensorBlock> WEATHER_SENSOR = BLOCKS.register(
            "weather_sensor",
            () -> new WeatherSensorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<SUChargerBlock> SU_CHARGER = BLOCKS.register(
            "su_charger",
            () -> new SUChargerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StressGateBlock> STRESS_GATE = BLOCKS.register(
            "stress_gate",
            () -> new StressGateBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f, 4.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<LightningMediumBlock> LIGHTNING_MEDIUM = BLOCKS.register(
            "lightning_medium",
            () -> new LightningMediumBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.5f)
                    .sound(SoundType.GLASS)));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
