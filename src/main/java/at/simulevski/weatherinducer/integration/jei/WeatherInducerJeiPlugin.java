package at.simulevski.weatherinducer.integration.jei;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI integration: adds an information page for each block explaining its
 * mechanics. Discovered automatically by JEI via the {@link JeiPlugin}
 * annotation; the class is only loaded when JEI is installed.
 */
@JeiPlugin
public class WeatherInducerJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return WeatherInducerMod.asResource("jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(ModItems.WEATHER_INDUCER.get(),
                Component.translatable("weatherinducer.info.weather_inducer"));
        registration.addIngredientInfo(ModItems.WEATHER_SENSOR.get(),
                Component.translatable("weatherinducer.info.weather_sensor"));
        registration.addIngredientInfo(ModItems.KINETIC_CHARGER.get(),
                Component.translatable("weatherinducer.info.kinetic_charger"));
        registration.addIngredientInfo(ModItems.STRESS_GATE.get(),
                Component.translatable("weatherinducer.info.stress_gate"));
        registration.addIngredientInfo(ModItems.LIGHTNING_MEDIUM.get(),
                Component.translatable("weatherinducer.info.lightning_medium"));
    }
}
