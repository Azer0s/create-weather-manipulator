package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.WeatherInducerMod;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/**
 * Partial models used by the Flywheel visuals. Must be created during mod
 * construction, before the client bakes models, so {@link #init()} is
 * called from the mod constructor on the client dist.
 */
public final class ModPartialModels {

    /**
     * Create's shaft, shrunk by a fiftieth of a pixel at both ends. The
     * full-length shaft partial puts its end cap quads exactly on the
     * block boundary, coplanar with the end caps of any attached shaft
     * block; the two fight over the depth buffer and the junction
     * shimmers dark. Pulling our internal shaft's ends in a hair lets the
     * attached shaft's caps win cleanly, with no visible shortening.
     */
    public static final PartialModel INNER_SHAFT =
            PartialModel.of(WeatherInducerMod.asResource("block/inner_shaft"));

    /**
     * The Charger Link's bulb, inflated half a pixel, drawn fullbright by
     * the link renderer for the periodic heartbeat flash.
     */
    public static final PartialModel CHARGER_LINK_GLOW =
            PartialModel.of(WeatherInducerMod.asResource("block/charger_link_glow"));

    private ModPartialModels() {
    }

    public static void init() {
        // Classloading this class registers the partials.
    }
}
