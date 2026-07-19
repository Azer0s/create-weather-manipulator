package at.simulevski.weatherinducer.integration.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/**
 * KubeJS integration: exposes a {@code WeatherInducer} global to scripts. Listed
 * in {@code kubejs.plugins.txt} so KubeJS discovers it; only loaded when KubeJS
 * is installed.
 */
public class WeatherInducerKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("WeatherInducer", WeatherInducerKubeBindings.class);
    }
}
