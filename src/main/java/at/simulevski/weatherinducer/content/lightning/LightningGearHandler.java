package at.simulevski.weatherinducer.content.lightning;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.registry.ModBlocks;
import at.simulevski.weatherinducer.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Everything the lightning gear does at runtime:
 * <ul>
 *   <li>holding any lightning tool grants Speed II;</li>
 *   <li>a full armor set grants water breathing, fire resistance,
 *       Resistance IV, Strength II, Speed II, Regeneration, creative flight
 *       (via NeoForge's flight attribute) and negates fall damage. The
 *       crackling aura around the set is client-side, see
 *       {@code client/LightningAuraHandler}. Thorns comes baked onto the
 *       armor pieces by their crafting recipes;</li>
 *   <li>a lightning strike on a Lightning Medium consumes the block and pops
 *       out a Bottle o' Lightning.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WeatherInducerMod.MOD_ID)
public final class LightningGearHandler {

    private static final ResourceLocation FLIGHT_ID =
            WeatherInducerMod.asResource("lightning_armor_flight");

    /** Effects are refreshed every tick with this duration, so they never run dry. */
    private static final int EFFECT_TICKS = 32;

    private LightningGearHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        if (LightningGear.isLightningTool(player.getMainHandItem())
                || LightningGear.isLightningTool(player.getOffhandItem())) {
            apply(player, MobEffects.MOVEMENT_SPEED, 1);
        }

        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (LightningGear.hasFullSet(player)) {
            apply(player, MobEffects.WATER_BREATHING, 0);
            apply(player, MobEffects.FIRE_RESISTANCE, 0);
            apply(player, MobEffects.DAMAGE_RESISTANCE, 3);
            apply(player, MobEffects.DAMAGE_BOOST, 1);
            apply(player, MobEffects.MOVEMENT_SPEED, 1);
            apply(player, MobEffects.REGENERATION, 1);
            if (flight != null && !flight.hasModifier(FLIGHT_ID)) {
                flight.addTransientModifier(new AttributeModifier(FLIGHT_ID, 1,
                        AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (flight != null && flight.hasModifier(FLIGHT_ID)) {
            flight.removeModifier(FLIGHT_ID);
        }
    }

    private static void apply(Player player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, amplifier, true, false, false));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && LightningGear.hasFullSet(player)) {
            event.setDamageMultiplier(0);
        }
    }

    /**
     * Bottling: a lightning strike on (or directly above) a Lightning Medium
     * consumes the block and drops a Bottle o' Lightning. Works for natural
     * storms, channeling tridents, and the Weather Inducer's aimed strikes.
     */
    @SubscribeEvent
    public static void onLightning(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || event.getLevel().isClientSide) {
            return;
        }
        // The armor's cosmetic bolts must not bottle (or eat) a medium.
        if (bolt.getTags().contains(LightningArmorEvents.COSMETIC_TAG)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos base = bolt.blockPosition();
        for (BlockPos pos : new BlockPos[]{base, base.below()}) {
            if (level.getBlockState(pos).is(ModBlocks.LIGHTNING_MEDIUM.get())) {
                level.destroyBlock(pos, false);
                // A strike bottles one to three: usually one, sometimes
                // two, now and then a lucky third.
                float roll = level.random.nextFloat();
                int count = roll < 0.6f ? 1 : roll < 0.9f ? 2 : 3;
                // The drop lands right at the strike point, so it must be
                // invulnerable or the very bolt that bottled it burns it up.
                ItemEntity drop = new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(ModItems.BOTTLE_O_LIGHTNING.get(), count));
                drop.setInvulnerable(true);
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
                break;
            }
        }
    }
}
