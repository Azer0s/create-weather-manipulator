package at.simulevski.weatherinducer.client;

import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Client-only holder for the charger display source's configuration
 * widgets. All the Create GUI widget types (which chain down to
 * client-only classes) live here, so the common display source never
 * names them and can be constructed on a dedicated server without the
 * dist cleaner tripping over a client class.
 */
public final class ChargerDisplayConfig {

    private ChargerDisplayConfig() {
    }

    /** The "Displayed Info" dropdown, in the Stressometer's style. */
    public static void addModeDropdown(ModularGuiLineBuilder builder) {
        builder.addSelectionScrollInput(0, 120, (input, label) -> {
            input.forOptions(List.of(
                    Component.translatable("weatherinducer.display.charger.progress_bar"),
                    Component.translatable("weatherinducer.display.charger.percent"),
                    Component.translatable("weatherinducer.display.charger.current"),
                    Component.translatable("weatherinducer.display.charger.max"),
                    Component.translatable("weatherinducer.display.charger.remaining")))
                    .titled(Component.translatable("weatherinducer.display.charger.display"));
            input.writingTo(label);
        }, "Mode");
    }
}
