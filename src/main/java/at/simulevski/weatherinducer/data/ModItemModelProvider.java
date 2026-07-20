package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, WeatherInducerMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Item models simply inherit the block model; the inducer and charger
        // use their empty (level 0) variant.
        withExistingParent("weather_inducer", modLoc("block/weather_inducer_0"));
        withExistingParent("su_resistor", modLoc("block/su_resistor"));
        withExistingParent("weather_sensor", modLoc("block/weather_sensor"));
        withExistingParent("su_charger", modLoc("block/su_charger_0"));
    }
}
