package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.ChargerDisplaySource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the charger's two display sources with Create and attaches
 * them to the Kinetic Charger's block entity type. Each source carries
 * its own "Displayed Info" dropdown (progress bar, percentage, stored
 * SU, capacity, remaining), just like the Stressometer, so a display
 * link on a charger reads exactly the value it wants for the charger
 * itself or for its whole link network.
 */
public final class ModDisplaySources {

    public static final ChargerDisplaySource.ThisCharger CHARGER =
            new ChargerDisplaySource.ThisCharger();
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
            helper.register(WeatherInducerMod.asResource("charger"), CHARGER);
            helper.register(WeatherInducerMod.asResource("charger_network"), CHARGER_NETWORK);
        });
    }

    private static void attachSources(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.KINETIC_CHARGER.get(), CHARGER);
            DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.KINETIC_CHARGER.get(),
                    CHARGER_NETWORK);
        });
    }
}
