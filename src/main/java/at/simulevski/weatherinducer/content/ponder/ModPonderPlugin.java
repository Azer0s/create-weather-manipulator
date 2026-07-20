package at.simulevski.weatherinducer.content.ponder;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers this mod's Ponder scenes. Added to the Ponder index during client
 * setup (see {@link at.simulevski.weatherinducer.client.WeatherInducerClient}).
 */
public class ModPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return WeatherInducerMod.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Second argument -> assets/weatherinducer/ponder/<name>.nbt
        helper.addStoryBoard(
                WeatherInducerMod.asResource("weather_inducer"),
                WeatherInducerMod.asResource("weather_inducer"),
                ModPonderScenes::weatherInducer);

        helper.addStoryBoard(
                WeatherInducerMod.asResource("kinetic_charger"),
                WeatherInducerMod.asResource("kinetic_charger"),
                ModPonderScenes::kineticCharger);
    }
}
