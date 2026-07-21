package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.ChargerDisplaySource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the charger's display sources with Create and attaches them
 * to the Kinetic Charger's block entity type, so a display link placed
 * on a charger offers them for selection.
 */
public final class ModDisplaySources {

    public static final ChargerDisplaySource.Charge CHARGER_CHARGE =
            new ChargerDisplaySource.Charge();
    public static final ChargerDisplaySource.Status CHARGER_STATUS =
            new ChargerDisplaySource.Status();
    public static final ChargerDisplaySource.Network CHARGER_NETWORK =
            new ChargerDisplaySource.Network();

    private ModDisplaySources() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModDisplaySources::registerSources);
        modEventBus.addListener(ModDisplaySources::attachSources);
    }

    private static void registerSources(RegisterEvent event) {
        event.register(CreateRegistries.DISPLAY_SOURCE, helper -> {
            helper.register(WeatherInducerMod.asResource("charger_charge"), CHARGER_CHARGE);
            helper.register(WeatherInducerMod.asResource("charger_status"), CHARGER_STATUS);
            helper.register(WeatherInducerMod.asResource("charger_network"), CHARGER_NETWORK);
        });
    }

    private static void attachSources(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.KINETIC_CHARGER.get(),
                    CHARGER_CHARGE);
            DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.KINETIC_CHARGER.get(),
                    CHARGER_STATUS);
            DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.KINETIC_CHARGER.get(),
                    CHARGER_NETWORK);
        });
    }
}
