package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, WeatherInducerMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + WeatherInducerMod.MOD_ID, "Weather Inducer");

        add(ModBlocks.WEATHER_INDUCER.get(), "Weather Inducer");
        add(ModBlocks.SU_RESISTOR.get(), "SU Resistor");
        add(ModBlocks.WEATHER_SENSOR.get(), "Weather Sensor");
        add(ModBlocks.SU_CHARGER.get(), "SU Charger");

        add("weatherinducer.value.mode", "Weather Mode");
        add("weatherinducer.value.offset_x", "Lightning Offset X");
        add("weatherinducer.value.offset_z", "Lightning Offset Z");
        add("weatherinducer.value.su_limit", "Max SU Draw");

        add("weatherinducer.mode.rain", "Rain");
        add("weatherinducer.mode.clear", "Clear");
        add("weatherinducer.mode.lightning", "Lightning");

        add("weatherinducer.tooltip.title", "Weather Inducer");
        add("weatherinducer.tooltip.charge", "Charge: %s / %s SU (%s%%)");
        add("weatherinducer.tooltip.mode", "Mode: %s");
        add("weatherinducer.tooltip.su_resistor", "SU Resistor");
        add("weatherinducer.tooltip.su_limit", "Limit: %s SU");
        add("weatherinducer.tooltip.su_charger", "SU Charger");
        add("weatherinducer.tooltip.buffer", "Buffer: %s / %s SU (%s%%)");
        add("weatherinducer.tooltip.charger_mode", "Mode: %s");
        add("weatherinducer.charger_mode.charging", "Charging");
        add("weatherinducer.charger_mode.discharging", "Discharging");
        add("weatherinducer.charger_mode.drained", "Powered, empty");

        // Ponder
        add("weatherinducer.ponder.weather_inducer.header", "Controlling the weather with the Weather Inducer");
        add("weatherinducer.ponder.su_resistor.header", "Throttling SU with the SU Resistor");

        // JEI / EMI information pages
        add("weatherinducer.info.weather_inducer",
                "The Weather Inducer charges from the Stress Units flowing through its shaft, up to 1,000,000 SU. "
                        + "It draws whatever the network offers, at most 100,000 SU per tick. While it can see the "
                        + "sky, a redstone pulse then applies the selected weather - rain, clear, or a lightning "
                        + "strike at a configurable offset - and discharges it.");
        add("weatherinducer.info.su_resistor",
                "The SU Resistor sits inline on a shaft and caps how much SU whatever is hooked up through it may "
                        + "draw from the network. Set the limit with a value box. A Weather Inducer behind a "
                        + "resistor charges no faster than the cap allows.");
        add("weatherinducer.info.weather_sensor",
                "The Weather Sensor reads the sky like a daylight detector reads the sun: it emits redstone "
                        + "signal 0 under clear skies, 7 in rain and 15 during a thunderstorm. It needs to see "
                        + "the sky; covered, it reads 0.");
        add("weatherinducer.info.su_charger",
                "The SU Charger is a kinetic capacitor. Rotation passes through it, SU never does. Unpowered, "
                        + "it fills a 1,000,000 SU buffer from its input side at up to 100,000 SU per tick. "
                        + "Give it a redstone signal and machines on its output side may drain the buffer "
                        + "instead. A comparator reads the fill level.");
    }
}
