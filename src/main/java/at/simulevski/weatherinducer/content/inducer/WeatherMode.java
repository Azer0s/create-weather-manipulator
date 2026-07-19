package at.simulevski.weatherinducer.content.inducer;

/**
 * The three effects the Weather Inducer can apply. The ordinal is what the
 * mode {@code ScrollValueBehaviour} stores (0..2), so keep the order stable.
 */
public enum WeatherMode {
    RAIN,
    CLEAR,
    LIGHTNING;

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
}
