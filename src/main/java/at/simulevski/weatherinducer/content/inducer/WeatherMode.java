package at.simulevski.weatherinducer.content.inducer;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

/**
 * The three effects the Weather Inducer can apply. The ordinal is what the
 * mode {@code ScrollOptionBehaviour} stores (0..2), so keep the order stable.
 * Implementing {@link INamedIconOptions} gives the mode selector Create's
 * option menu with an icon and label per entry.
 */
public enum WeatherMode implements INamedIconOptions {
    RAIN(AllIcons.I_FILL),
    CLEAR(AllIcons.I_CLEAR),
    LIGHTNING(AllIcons.I_TARGET);

    private final AllIcons icon;

    WeatherMode(AllIcons icon) {
        this.icon = icon;
    }

    public static WeatherMode fromIndex(int index) {
        WeatherMode[] values = values();
        if (index < 0) {
            index = 0;
        }
        if (index >= values.length) {
            index = values.length - 1;
        }
        return values[index];
    }

    /** Lower-case key used to build the translation key for the value-box label. */
    public String translationKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return "weatherinducer.mode." + translationKey();
    }
}
