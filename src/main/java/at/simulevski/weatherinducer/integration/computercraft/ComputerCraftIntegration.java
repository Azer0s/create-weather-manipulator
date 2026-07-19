package at.simulevski.weatherinducer.integration.computercraft;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Isolated ComputerCraft hookup. This class is only ever loaded when
 * ComputerCraft is present (see the guard in {@code ModCapabilities}), so it is
 * safe to reference CC classes directly here.
 */
public final class ComputerCraftIntegration {

    private ComputerCraftIntegration() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                PeripheralCapability.get(),
                ModBlockEntities.WEATHER_INDUCER.get(),
                (blockEntity, side) -> new WeatherInducerPeripheral(blockEntity));
    }
}
