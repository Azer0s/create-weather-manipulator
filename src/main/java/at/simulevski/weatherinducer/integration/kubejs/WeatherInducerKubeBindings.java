package at.simulevski.weatherinducer.integration.kubejs;

import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlockEntity;
import at.simulevski.weatherinducer.content.inducer.WeatherMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * The {@code WeatherInducer} binding exposed to KubeJS scripts. Lets scripts
 * inspect and drive a Weather Inducer at a position, e.g.:
 *
 * <pre>
 * // in a server script
 * BlockEvents.rightClicked(event =&gt; {
 *   let pos = event.block.pos
 *   if (WeatherInducer.isCharged(event.level, pos)) WeatherInducer.fire(event.level, pos)
 * })
 * </pre>
 */
public final class WeatherInducerKubeBindings {

    public static final double MAX_CHARGE = WeatherInducerBlockEntity.MAX_CHARGE;

    private WeatherInducerKubeBindings() {
    }

    private static WeatherInducerBlockEntity at(Level level, BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof WeatherInducerBlockEntity be ? be : null;
    }

    /** Current stored SU, or -1 if there is no Weather Inducer at {@code pos}. */
    public static double getCharge(Level level, BlockPos pos) {
        WeatherInducerBlockEntity be = at(level, pos);
        return be != null ? be.getCharge() : -1;
    }

    public static boolean isCharged(Level level, BlockPos pos) {
        WeatherInducerBlockEntity be = at(level, pos);
        return be != null && be.isCharged();
    }

    public static String getMode(Level level, BlockPos pos) {
        WeatherInducerBlockEntity be = at(level, pos);
        return be != null ? be.getMode().translationKey() : null;
    }

    /** Sets the mode ("rain", "clear" or "lightning"); returns false if invalid or no inducer. */
    public static boolean setMode(Level level, BlockPos pos, String mode) {
        WeatherInducerBlockEntity be = at(level, pos);
        if (be == null) {
            return false;
        }
        try {
            be.setMode(WeatherMode.valueOf(mode.toUpperCase(Locale.ROOT)));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void setLightningOffset(Level level, BlockPos pos, int x, int z) {
        WeatherInducerBlockEntity be = at(level, pos);
        if (be != null) {
            be.setLightningOffset(x, z);
        }
    }

    /** Attempts to fire the inducer; returns true if it fired. */
    public static boolean fire(Level level, BlockPos pos) {
        WeatherInducerBlockEntity be = at(level, pos);
        return be != null && be.fire();
    }
}
