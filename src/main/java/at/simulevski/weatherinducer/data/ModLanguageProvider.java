package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.ponder.ModPonderPlugin;
import at.simulevski.weatherinducer.registry.ModBlocks;
import at.simulevski.weatherinducer.registry.ModEntityTypes;
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
        add(ModBlocks.WEATHER_SENSOR.get(), "Weather Sensor");
        add(ModBlocks.KINETIC_CHARGER.get(), "Kinetic Charger");
        add(ModBlocks.STRESS_GATE.get(), "Stress Gate");
        add(ModBlocks.LIGHTNING_MEDIUM.get(), "Lightning Medium");

        add(ModItems.BOTTLE_O_LIGHTNING.get(), "Bottle o' Lightning");
        add(ModEntityTypes.THROWN_BOTTLE_O_LIGHTNING.get(), "Bottle o' Lightning");
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
        add("weatherinducer.value.threshold", "SU Threshold");
        add("weatherinducer.value.su_row", "SU");
        add("weatherinducer.value.charge_time", "Charge Time");
        add("weatherinducer.value.charge_time_row", "Seconds");

        add("weatherinducer.mode.rain", "Rain");
        add("weatherinducer.mode.clear", "Clear");
        add("weatherinducer.mode.lightning", "Lightning");

        add("weatherinducer.tooltip.title", "Weather Inducer");
        add("weatherinducer.tooltip.charge", "Charge: %s / %s SU (%s%%)");
        add("weatherinducer.tooltip.charge_time", "Full charge in %s s (loads the network with %s SU)");
        add("weatherinducer.tooltip.mode", "Mode: %s");
        add("weatherinducer.tooltip.stress_gate", "Stress Gate");
        add("weatherinducer.tooltip.threshold", "Unlocks at: %s SU provided");
        add("weatherinducer.tooltip.provided", "Network provides: %s SU");
        add("weatherinducer.tooltip.locked", "Locked: the network provides too little SU");
        add("weatherinducer.tooltip.kinetic_charger", "Kinetic Charger");
        add("weatherinducer.tooltip.buffer", "Buffer: %s / %s SU (%s%%)");
        add("weatherinducer.tooltip.flywheels", "Flywheels: %s (up to %s add capacity)");
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
                "The Weather Inducer charges while its shaft turns, loading the kinetic network with real "
                        + "stress: the 1,048,576 SU it needs divided by its charge time (10 seconds at the "
                        + "fastest, so it fires at most once every 10 seconds). The charge time locks while any "
                        + "charge is in the block. While it can see the sky, a redstone pulse applies the "
                        + "selected weather - rain, clear, or a lightning strike at a configurable offset - and "
                        + "discharges it.");
        add("weatherinducer.info.weather_sensor",
                "The Weather Sensor reads the sky like a daylight detector reads the sun: it emits redstone "
                        + "signal 0 under clear skies, 7 in rain and 15 during a thunderstorm. It needs to see "
                        + "the sky; covered, it reads 0.");
        add("weatherinducer.info.kinetic_charger",
                "The Kinetic Charger is a kinetic battery. While the shaft turns it charges like the Weather "
                        + "Inducer does, loading the network with its buffer divided by its charge time (10 "
                        + "seconds at the fastest; the slider locks while the buffer holds anything). Flywheels "
                        + "attached on the input side set the capacity: 2,048 SU bare, about 104,858 more per "
                        + "flywheel, up to ten of them. More than ten jams the charger and overstresses the "
                        + "network. Stop the input and it takes over: it drives its output side at the speed it "
                        + "charged with, providing 131,072 SU and draining the buffer by what the machines use. "
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
