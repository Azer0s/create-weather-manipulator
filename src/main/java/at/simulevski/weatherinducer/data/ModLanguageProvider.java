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

        add("weatherinducer.value.mode", "Weather Mode");
        add("weatherinducer.value.offset_x", "Lightning Offset X");
        add("weatherinducer.value.offset_z", "Lightning Offset Z");
        add("weatherinducer.value.su_limit", "SU per Tick");

        add("weatherinducer.mode.rain", "Rain");
        add("weatherinducer.mode.clear", "Clear");
        add("weatherinducer.mode.lightning", "Lightning");

        add("weatherinducer.tooltip.title", "Weather Inducer");
        add("weatherinducer.tooltip.charge", "Charge: %s / %s SU (%s%%)");
        add("weatherinducer.tooltip.mode", "Mode: %s");
        add("weatherinducer.tooltip.su_resistor", "SU Resistor");
        add("weatherinducer.tooltip.su_limit", "Limit: %s SU/t");

        // Ponder
        add("weatherinducer.ponder.weather_inducer.header", "Controlling the weather with the Weather Inducer");
        add("weatherinducer.ponder.su_resistor.header", "Throttling SU with the SU Resistor");

        // JEI / EMI information pages
        add("weatherinducer.info.weather_inducer",
                "The Weather Inducer charges from the Stress Units flowing through its shaft, up to 100,000 SU. "
                        + "While it can see the sky, a redstone pulse then applies the selected weather - rain, clear, "
                        + "or a lightning strike at a configurable offset - and discharges it. Use SU Resistors to slow "
                        + "how fast it charges.");
        add("weatherinducer.info.su_resistor",
                "The SU Resistor sits inline on a shaft and caps how many Stress Units per tick may pass through it "
                        + "toward a Weather Inducer. Set the limit with a value box. Without a resistor, an Inducer "
                        + "drains the whole network in a single tick.");
    }
}
