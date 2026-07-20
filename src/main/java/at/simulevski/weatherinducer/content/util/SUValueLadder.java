package at.simulevski.weatherinducer.content.util;

/**
 * The logarithmic 1-2.5-5 ladder of SU values the scroll boxes use (SU
 * Resistor cap, Stress Gate threshold). Scrolling linearly over 0..1,000,000
 * was hopeless, so the scroll behaviours store an index into this table and
 * their formatters show the SU value it stands for.
 */
public final class SUValueLadder {

    public static final int[] STEPS = {
            0, 100, 250, 500, 1_000, 2_500, 5_000, 10_000,
            25_000, 50_000, 100_000, 250_000, 500_000, 1_000_000,
    };

    private SUValueLadder() {
    }

    public static int clampIndex(int index) {
        return Math.max(0, Math.min(index, STEPS.length - 1));
    }

    public static int value(int index) {
        return STEPS[clampIndex(index)];
    }

    public static String format(int index) {
        return String.format("%,d", value(index));
    }
}
