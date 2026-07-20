package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.SUChargerBlockEntity;
import at.simulevski.weatherinducer.content.gate.StressGateBlockEntity;
import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlockEntity;
import at.simulevski.weatherinducer.content.lightning.LightningMediumBlockEntity;
import at.simulevski.weatherinducer.content.sensor.WeatherSensorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WeatherInducerMod.MOD_ID);

    // Create's KineticBlockEntity uses a 3-arg constructor
    // (BlockEntityType, BlockPos, BlockState), so we wrap it in the 2-arg
    // BlockEntitySupplier the vanilla builder expects, passing the (deferred)
    // block entity type in. The lambda runs lazily, after registration, so the
    // self-reference to the type holder is safe.
    public static final Supplier<BlockEntityType<WeatherInducerBlockEntity>> WEATHER_INDUCER =
            BLOCK_ENTITIES.register("weather_inducer",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new WeatherInducerBlockEntity(
                                    ModBlockEntities.WEATHER_INDUCER.get(), pos, state),
                            ModBlocks.WEATHER_INDUCER.get()).build(null));

    public static final Supplier<BlockEntityType<WeatherSensorBlockEntity>> WEATHER_SENSOR =
            BLOCK_ENTITIES.register("weather_sensor",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new WeatherSensorBlockEntity(
                                    ModBlockEntities.WEATHER_SENSOR.get(), pos, state),
                            ModBlocks.WEATHER_SENSOR.get()).build(null));

    public static final Supplier<BlockEntityType<SUChargerBlockEntity>> SU_CHARGER =
            BLOCK_ENTITIES.register("su_charger",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new SUChargerBlockEntity(
                                    ModBlockEntities.SU_CHARGER.get(), pos, state),
                            ModBlocks.SU_CHARGER.get()).build(null));

    public static final Supplier<BlockEntityType<LightningMediumBlockEntity>> LIGHTNING_MEDIUM =
            BLOCK_ENTITIES.register("lightning_medium",
                    () -> BlockEntityType.Builder.of(
                            LightningMediumBlockEntity::new,
                            ModBlocks.LIGHTNING_MEDIUM.get()).build(null));

    public static final Supplier<BlockEntityType<StressGateBlockEntity>> STRESS_GATE =
            BLOCK_ENTITIES.register("stress_gate",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new StressGateBlockEntity(
                                    ModBlockEntities.STRESS_GATE.get(), pos, state),
                            ModBlocks.STRESS_GATE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
