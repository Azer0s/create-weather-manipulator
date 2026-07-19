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
        // Item models simply inherit the block model.
        withExistingParent("weather_inducer", modLoc("block/weather_inducer"));
        withExistingParent("su_resistor", modLoc("block/su_resistor"));
        withExistingParent("weather_sensor", modLoc("block/weather_sensor"));
        withExistingParent("su_charger", modLoc("block/su_charger"));
    }
}
