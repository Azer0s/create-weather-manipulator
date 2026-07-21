package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.client.ChargerDisplayConfig;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * The charger's display sources, built exactly like Create's own
 * Stressometer source ({@code KineticStressDisplaySource}): a single
 * source with a "Displayed Info" dropdown, so one display link offers a
 * progress bar, a percentage, the stored SU, the total capacity or the
 * remaining time, plus the attached-label text box the base class adds.
 * One source reads this charger, the other reads its whole link network.
 */
public abstract class ChargerDisplaySource extends PercentOrProgressBarDisplaySource {

    protected static final int PROGRESS = 0;
    protected static final int PERCENT = 1;
    protected static final int STORED = 2;
    protected static final int CAPACITY = 3;
    protected static final int REMAINING = 4;

    protected int getMode(DisplayLinkContext context) {
        return context.sourceConfig().getInt("Mode");
    }

    protected KineticChargerBlockEntity charger(DisplayLinkContext context) {
        return context.getSourceBlockEntity() instanceof KineticChargerBlockEntity c ? c : null;
    }

    /** SU-seconds stored right now for this source's scope. */
    protected abstract double stored(KineticChargerBlockEntity charger);

    /** Capacity in SU-seconds for this source's scope. */
    protected abstract double capacity(KineticChargerBlockEntity charger);

    /** SU-seconds leaving per second, for the remaining-time readout. */
    protected abstract double drain(KineticChargerBlockEntity charger);

    /** False when the scope has nothing to show (e.g. no link network). */
    protected boolean present(KineticChargerBlockEntity charger) {
        return charger != null;
    }

    @Override
    protected Float getProgress(DisplayLinkContext context) {
        KineticChargerBlockEntity charger = charger(context);
        if (!present(charger)) {
            return 0f;
        }
        double cap = Math.max(1, capacity(charger));
        return (float) Math.min(1.0, stored(charger) / cap);
    }

    @Override
    protected boolean progressBarActive(DisplayLinkContext context) {
        return getMode(context) == PROGRESS;
    }

    @Override
    protected MutableComponent formatNumeric(DisplayLinkContext context, Float progress) {
        KineticChargerBlockEntity charger = charger(context);
        if (!present(charger)) {
            return Component.literal("-");
        }
        int mode = getMode(context);
        return switch (mode) {
            case STORED -> suSeconds(stored(charger));
            case CAPACITY -> suSeconds(capacity(charger));
            case REMAINING -> {
                double drain = drain(charger);
                yield drain > 0 && stored(charger) > 0
                        ? Component.literal(duration(stored(charger) / drain))
                        : Component.literal("-");
            }
            // PERCENT and anything else fall through to the base percentage.
            default -> super.formatNumeric(context, progress);
        };
    }

    @Override
    public void initConfigurationWidgets(DisplayLinkContext context,
                                         ModularGuiLineBuilder builder, boolean isFirstLine) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if (isFirstLine) {
            return;
        }
        // Only ever runs on the client (the config screen); the widget
        // classes live in a client-only helper so this common class stays
        // server-safe.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ChargerDisplayConfig.addModeDropdown(builder);
        }
    }

    protected static MutableComponent suSeconds(double value) {
        return Component.literal(String.format("%,.0f SU-s", value));
    }

    protected static String duration(double seconds) {
        long s = (long) Math.floor(seconds);
        if (s < 120) {
            return s + " s";
        }
        if (s < 7200) {
            return (s / 60) + " min " + (s % 60) + " s";
        }
        return (s / 3600) + " h " + (s % 3600 / 60) + " min";
    }

    // --- The two concrete scopes ---------------------------------------

    /** Reads the charger the link is bolted to. */
    public static class ThisCharger extends ChargerDisplaySource {
        @Override
        protected double stored(KineticChargerBlockEntity c) {
            return c.getBuffer();
        }

        @Override
        protected double capacity(KineticChargerBlockEntity c) {
            return c.getMaxBuffer();
        }

        @Override
        protected double drain(KineticChargerBlockEntity c) {
            return c.isDischarging() ? c.getDrainPerSecond() : 0;
        }

        @Override
        protected boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }

        @Override
        protected String getTranslationKey() {
            return "charger";
        }
    }

    /** Reads the whole link network the charger belongs to. */
    public static class Network extends ChargerDisplaySource {
        @Override
        protected boolean present(KineticChargerBlockEntity c) {
            return c != null && c.getGroupSize() > 0;
        }

        @Override
        protected double stored(KineticChargerBlockEntity c) {
            return c.getGroupEnergy();
        }

        @Override
        protected double capacity(KineticChargerBlockEntity c) {
            return c.getGroupCapacity();
        }

        @Override
        protected double drain(KineticChargerBlockEntity c) {
            return c.getGroupDrain();
        }

        @Override
        protected boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }

        @Override
        protected String getTranslationKey() {
            return "charger_network";
        }
    }
}
