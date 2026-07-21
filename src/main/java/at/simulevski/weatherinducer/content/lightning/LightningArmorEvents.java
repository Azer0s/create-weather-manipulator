package at.simulevski.weatherinducer.content.lightning;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The armor's storm theatrics, real Minecraft lightning marked
 * visual-only: vanilla gates both fire spawning and the thunder-hit
 * damage on that flag, so the bolts look and sound the part without
 * hurting anything or lighting anything up.
 *
 * <p>Completing the full set under a clear open sky calls down a short
 * roll of strikes around the wearer.
 */
@EventBusSubscriber(modid = WeatherInducerMod.MOD_ID)
public final class LightningArmorEvents {

    /** Marks the armor's cosmetic bolts so nothing treats them as real. */
    public static final String COSMETIC_TAG = "weatherinducer.cosmetic";

    /** Strikes still owed to a freshly suited-up wearer. */
    private static final Map<UUID, Integer> PENDING_SALUTE = new HashMap<>();
    private static final Map<UUID, Boolean> HAD_FULL_SET = new HashMap<>();

    private LightningArmorEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean clearSky = level.canSeeSky(player.blockPosition()) && !level.isRaining();
        RandomSource random = level.random;

        boolean full = LightningGear.hasFullSet(player);
        Boolean had = HAD_FULL_SET.put(player.getUUID(), full);
        if (full && (had == null || !had) && clearSky) {
            PENDING_SALUTE.put(player.getUUID(), 7);
        }
        if (!full) {
            PENDING_SALUTE.remove(player.getUUID());
        }

        // The suit-up salute: one strike every few ticks, the first right
        // on the wearer, the rest in a loose ring.
        Integer pending = PENDING_SALUTE.get(player.getUUID());
        if (pending != null && pending > 0 && level.getGameTime() % 4 == 0) {
            if (pending == 7) {
                spawnVisualBolt(level, player.getX(), player.getY(), player.getZ());
            } else {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 2.5 + random.nextDouble() * 2.5;
                spawnVisualBolt(level,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY(),
                        player.getZ() + Math.sin(angle) * radius);
            }
            if (pending <= 1) {
                PENDING_SALUTE.remove(player.getUUID());
            } else {
                PENDING_SALUTE.put(player.getUUID(), pending - 1);
            }
        }

    }

    private static void spawnVisualBolt(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.setVisualOnly(true);
        bolt.addTag(COSMETIC_TAG);
        bolt.moveTo(x, y, z);
        level.addFreshEntity(bolt);
    }
}
