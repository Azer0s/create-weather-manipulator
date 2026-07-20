package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.ponder.ModPonderPlugin;
import at.simulevski.weatherinducer.registry.ModBlocks;
import at.simulevski.weatherinducer.registry.ModItems;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.registration.PonderLocalization;
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
        add(ModBlocks.STRESS_GATE.get(), "Stress Gate");
        add(ModBlocks.LIGHTNING_MEDIUM.get(), "Lightning Medium");

        add(ModItems.BOTTLE_O_LIGHTNING.get(), "Bottle o' Lightning");
        add(ModItems.LIGHTNING_BOLT.get(), "Lightning Bolt");
        add(ModItems.LIGHTNING_SWORD.get(), "Lightning Sword");
        add(ModItems.LIGHTNING_PICKAXE.get(), "Lightning Pickaxe");
        add(ModItems.LIGHTNING_AXE.get(), "Lightning Axe");
        add(ModItems.LIGHTNING_SHOVEL.get(), "Lightning Shovel");
        add(ModItems.LIGHTNING_HOE.get(), "Lightning Hoe");
        add(ModItems.LIGHTNING_HELMET.get(), "Lightning Helmet");
        add(ModItems.LIGHTNING_CHESTPLATE.get(), "Lightning Chestplate");
        add(ModItems.LIGHTNING_LEGGINGS.get(), "Lightning Leggings");
        add(ModItems.LIGHTNING_BOOTS.get(), "Lightning Boots");

        add("weatherinducer.value.mode", "Weather Mode");
        add("weatherinducer.value.offset_x", "Lightning Offset X");
        add("weatherinducer.value.offset_z", "Lightning Offset Z");
        add("weatherinducer.value.su_limit", "Max SU Draw");
        add("weatherinducer.value.threshold", "SU Threshold");

        add("weatherinducer.mode.rain", "Rain");
        add("weatherinducer.mode.clear", "Clear");
        add("weatherinducer.mode.lightning", "Lightning");

        add("weatherinducer.tooltip.title", "Weather Inducer");
        add("weatherinducer.tooltip.charge", "Charge: %s / %s SU (%s%%)");
        add("weatherinducer.tooltip.mode", "Mode: %s");
        add("weatherinducer.tooltip.su_resistor", "SU Resistor");
        add("weatherinducer.tooltip.su_limit", "Limit: %s SU");
        add("weatherinducer.tooltip.demand", "Downstream draw: %s SU");
        add("weatherinducer.tooltip.tripped", "Tripped at %s SU, raise the limit to reset");
        add("weatherinducer.tooltip.stress_gate", "Stress Gate");
        add("weatherinducer.tooltip.threshold", "Unlocks at: %s SU provided");
        add("weatherinducer.tooltip.provided", "Network provides: %s SU");
        add("weatherinducer.tooltip.locked", "Locked: the network provides too little SU");
        add("weatherinducer.tooltip.su_charger", "SU Charger");
        add("weatherinducer.tooltip.buffer", "Buffer: %s / %s SU (%s%%)");
        add("weatherinducer.tooltip.charger_mode", "Mode: %s");
        add("weatherinducer.charger_mode.charging", "Charging");
        add("weatherinducer.charger_mode.discharging", "Discharging");
        add("weatherinducer.charger_mode.idle", "Idle");

        // Ponder scene text is authored inline in ModPonderScenes; Ponder looks
        // it up from the lang file at runtime, so run its registration here and
        // dump every generated entry (headers and text steps) into en_us.json.
        // Without this the scenes show raw translation keys.
        PonderIndex.addPlugin(new ModPonderPlugin());
        PonderIndex.registerAll();
        PonderLocalization ponderLang = (PonderLocalization) PonderIndex.getLangAccess();
        ponderLang.generateSceneLang();
        ponderLang.provideLang(WeatherInducerMod.MOD_ID, this::add);

        // JEI / EMI information pages
        add("weatherinducer.info.weather_inducer",
                "The Weather Inducer charges from the network's spare Stress Units (whatever the sources provide "
                        + "beyond what the machines use), up to 1,048,576 SU at 131,072 SU per tick at most. While "
                        + "it can see the sky, a redstone pulse then applies the selected weather - rain, clear, or "
                        + "a lightning strike at a configurable offset - and discharges it.");
        add("weatherinducer.info.su_resistor",
                "The SU Resistor is a circuit breaker for kinetic stress. If the machines downstream of it "
                        + "demand more SU than its limit, it trips and cuts rotation to that side, remembering "
                        + "the demand that broke it. It closes again on its own once the limit is raised to "
                        + "cover that demand.");
        add("weatherinducer.info.weather_sensor",
                "The Weather Sensor reads the sky like a daylight detector reads the sun: it emits redstone "
                        + "signal 0 under clear skies, 7 in rain and 15 during a thunderstorm. It needs to see "
                        + "the sky; covered, it reads 0.");
        add("weatherinducer.info.su_charger",
                "The SU Charger is a kinetic capacitor. While the shaft turns it soaks the network's spare SU "
                        + "into a 1,048,576 SU buffer, at up to 131,072 SU per tick. Stop the input, and machines "
                        + "on its output side may drain the buffer instead; raw network SU never passes through. "
                        + "It emits a redstone signal proportional to its fill level.");
        add("weatherinducer.info.lightning_medium",
                "A beacon and an end crystal encased in glass. Strike it with lightning (a Weather Inducer "
                        + "in lightning mode aims for you) and the block shatters into a Bottle o' Lightning, "
                        + "the raw ingredient of the lightning gear.");
        add("weatherinducer.info.stress_gate",
                "The Stress Gate stays locked, passing no rotation downstream, until its kinetic network "
                        + "provides at least the set amount of total SU. Use it to keep a contraption dormant "
                        + "until the power plant behind it is big enough.");
    }
}
