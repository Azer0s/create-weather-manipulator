package at.simulevski.weatherinducer.integration.emi;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.registry.ModItems;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * EMI integration: adds an information page for each block, mirroring the JEI
 * plugin. Discovered automatically by EMI via {@link EmiEntrypoint}; only loaded
 * when EMI is installed.
 */
@EmiEntrypoint
public class WeatherInducerEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipe(new EmiInfoRecipe(
                List.<EmiIngredient>of(EmiStack.of(ModItems.WEATHER_INDUCER.get())),
                List.of(Component.translatable("weatherinducer.info.weather_inducer")),
                WeatherInducerMod.asResource("info/weather_inducer")));

        registry.addRecipe(new EmiInfoRecipe(
                List.<EmiIngredient>of(EmiStack.of(ModItems.SU_RESISTOR.get())),
                List.of(Component.translatable("weatherinducer.info.su_resistor")),
                WeatherInducerMod.asResource("info/su_resistor")));

        registry.addRecipe(new EmiInfoRecipe(
                List.<EmiIngredient>of(EmiStack.of(ModItems.WEATHER_SENSOR.get())),
                List.of(Component.translatable("weatherinducer.info.weather_sensor")),
                WeatherInducerMod.asResource("info/weather_sensor")));

        registry.addRecipe(new EmiInfoRecipe(
                List.<EmiIngredient>of(EmiStack.of(ModItems.SU_CHARGER.get())),
                List.of(Component.translatable("weatherinducer.info.su_charger")),
                WeatherInducerMod.asResource("info/su_charger")));

        registry.addRecipe(new EmiInfoRecipe(
                List.<EmiIngredient>of(EmiStack.of(ModItems.STRESS_GATE.get())),
                List.of(Component.translatable("weatherinducer.info.stress_gate")),
                WeatherInducerMod.asResource("info/stress_gate")));
    }
}
