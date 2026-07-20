package at.simulevski.weatherinducer.content.util;

/**
 * The ladder of SU values the scroll boxes use (the Stress Gate
 * threshold): zero, then every power of two from 64 up to 1,048,576
 * (2^20, one full inducer charge). Scrolling linearly over that range was
 * hopeless, so the scroll behaviours store an index into this table and
 * their formatters show the SU value it stands for.
 */
public final class SUValueLadder {

    public static final int[] STEPS = {
            0, 64, 128, 256, 512, 1_024, 2_048, 4_096, 8_192,
            16_384, 32_768, 65_536, 131_072, 262_144, 524_288, 1_048_576,
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
