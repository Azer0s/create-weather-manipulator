package at.simulevski.weatherinducer.content.charger;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Display sources for the Kinetic Charger, so Create's display link can
 * be bolted onto one and report it to any display target: the charge
 * level as numbers, or the running status in words.
 */
public final class ChargerDisplaySource {

    private ChargerDisplaySource() {
    }

    /** One line of stored / capacity SU-seconds with the fill percent. */
    public static class Charge extends SingleLineDisplaySource {

        @Override
        protected MutableComponent provideLine(DisplayLinkContext context,
                                               DisplayTargetStats stats) {
            if (!(context.getSourceBlockEntity() instanceof KineticChargerBlockEntity charger)) {
                return EMPTY_LINE;
            }
            int percent = (int) Math.floor(100.0
                    * Math.min(1.0, charger.getBuffer() / charger.getMaxBuffer()));
            return Component.literal(String.format("%,.0f / %,.0f SU-s (%d%%)",
                    charger.getBuffer(), charger.getMaxBuffer(), percent));
        }

        @Override
        protected boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }
    }

    /** The whole link network: pooled fill and member count. */
    public static class Network extends SingleLineDisplaySource {

        @Override
        protected MutableComponent provideLine(DisplayLinkContext context,
                                               DisplayTargetStats stats) {
            if (!(context.getSourceBlockEntity() instanceof KineticChargerBlockEntity charger)
                    || charger.getGroupSize() <= 0) {
                return EMPTY_LINE;
            }
            double capacity = Math.max(1, charger.getGroupCapacity());
            int percent = (int) Math.floor(100.0
                    * Math.min(1.0, charger.getGroupEnergy() / capacity));
            return Component.translatable("weatherinducer.display.charger_network",
                    String.format("%,.0f", charger.getGroupEnergy()),
                    String.format("%,.0f", charger.getGroupCapacity()),
                    percent, charger.getGroupSize());
        }

        @Override
        protected boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }
    }

    /** The charger's mode and its flywheel bank on one line. */
    public static class Status extends SingleLineDisplaySource {

        @Override
        protected MutableComponent provideLine(DisplayLinkContext context,
                                               DisplayTargetStats stats) {
            if (!(context.getSourceBlockEntity() instanceof KineticChargerBlockEntity charger)) {
                return EMPTY_LINE;
            }
            return Component.translatable("weatherinducer.display.charger_status",
                    Component.translatable("weatherinducer.display.charger_mode."
                            + charger.modeKey()),
                    charger.getFlywheels());
        }

        @Override
        protected boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }
    }
}
