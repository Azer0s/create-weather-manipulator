package at.simulevski.weatherinducer;

import at.simulevski.weatherinducer.registry.ModArmorMaterials;
import at.simulevski.weatherinducer.registry.ModBlockEntities;
import at.simulevski.weatherinducer.registry.ModBlocks;
import at.simulevski.weatherinducer.registry.ModCreativeTabs;
import at.simulevski.weatherinducer.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Entry point for the Weather Inducer Create addon.
 *
 * <p>Adds five blocks:
 * <ul>
 *   <li><b>Weather Inducer</b> &mdash; charges from the connected kinetic
 *       network's spare stress units (SU) up to 1,048,576 SU (2^20),
 *       drawing at most 131,072 SU (2^17) per tick. When fully charged and pulsed with redstone it
 *       applies the selected weather effect (rain / clear / lightning at a
 *       configurable offset), provided it has line of sight to the sky.</li>
 *   <li><b>SU Resistor</b> &mdash; a latching circuit breaker: trips when
 *       the machines downstream of it draw more SU than its limit.</li>
 *   <li><b>Stress Gate</b> &mdash; stays locked until the network provides
 *       at least a set amount of total SU.</li>
 *   <li><b>SU Charger</b> &mdash; a kinetic capacitor: passes rotation but
 *       never SU, fills a buffer from its input side while the shaft turns,
 *       discharges it to consumers on its output side once the input stops,
 *       and emits a redstone signal proportional to its fill level.</li>
 *   <li><b>Weather Sensor</b> &mdash; a daylight-detector-shaped slab whose
 *       redstone signal reflects the weather (0 clear, 7 rain, 15
 *       thunder).</li>
 * </ul>
 */
@Mod(WeatherInducerMod.MOD_ID)
public class WeatherInducerMod {

    public static final String MOD_ID = "weatherinducer";

    public WeatherInducerMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
