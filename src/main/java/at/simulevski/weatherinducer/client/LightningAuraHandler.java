package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.lightning.LightningGear;
import at.simulevski.weatherinducer.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The storm aura around a full lightning set: electric sparks crawl over
 * the wearer, denser while sprinting or airborne, with a slow white
 * shimmer on top. Runs purely client-side so each viewer decides what to
 * draw; in particular, the local player sees no aura in first person
 * (it would just flicker in front of the camera), while everyone else,
 * and the third-person camera, sees the wearer crackle.
 */
@EventBusSubscriber(modid = WeatherInducerMod.MOD_ID, value = Dist.CLIENT)
public final class LightningAuraHandler {

    private LightningAuraHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        long time = mc.level.getGameTime();
        RandomSource random = mc.level.random;
        for (Player player : mc.level.players()) {
            // Boot jets: while flying under a clear sky, harmless little
            // lightning streaks trail out of the lightning boots. Just
            // particles along a jagged line; no animation library needed.
            if (time % 2 == 0
                    && player.getItemBySlot(EquipmentSlot.FEET)
                            .is(ModItems.LIGHTNING_BOOTS.get())
                    && (player.isFallFlying() || player.getAbilities().flying)
                    && mc.level.canSeeSky(player.blockPosition())
                    && !mc.level.isRaining()) {
                Vec3 motion = player.getDeltaMovement();
                Vec3 dir = motion.lengthSqr() > 1.0e-4
                        ? motion.normalize().scale(-1) : new Vec3(0, -1, 0);
                double rightYaw = Math.toRadians(player.getYRot() + 90);
                Vec3 right = new Vec3(-Math.sin(rightYaw), 0, Math.cos(rightYaw));
                for (int foot = -1; foot <= 1; foot += 2) {
                    Vec3 p = player.position()
                            .add(right.scale(foot * 0.15)).add(0, 0.05, 0);
                    for (int k = 0; k < 5; k++) {
                        p = p.add(dir.scale(0.22)).add(
                                (random.nextDouble() - 0.5) * 0.12,
                                (random.nextDouble() - 0.5) * 0.12,
                                (random.nextDouble() - 0.5) * 0.12);
                        mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                                p.x, p.y, p.z, 0, 0, 0);
                    }
                }
            }
            if (!LightningGear.hasFullSet(player)) {
                continue;
            }
            if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }
            boolean charged = player.isSprinting() || !player.onGround();
            if (time % (charged ? 2 : 5) == 0) {
                int sparks = 1 + random.nextInt(charged ? 3 : 2);
                for (int i = 0; i < sparks; i++) {
                    mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                            player.getX() + (random.nextDouble() - 0.5) * 0.9,
                            player.getY() + random.nextDouble() * 1.9,
                            player.getZ() + (random.nextDouble() - 0.5) * 0.9,
                            0, 0.02, 0);
                }
            }
            if (time % 16 == 0) {
                mc.level.addParticle(ParticleTypes.END_ROD,
                        player.getX() + (random.nextDouble() - 0.5) * 0.6,
                        player.getY() + 0.8 + random.nextDouble() * 0.8,
                        player.getZ() + (random.nextDouble() - 0.5) * 0.6,
                        0, 0.01, 0);
            }
        }
    }
}
