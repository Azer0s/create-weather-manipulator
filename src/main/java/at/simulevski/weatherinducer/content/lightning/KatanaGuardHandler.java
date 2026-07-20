package at.simulevski.weatherinducer.content.lightning;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.registry.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The katana's guard: while its wielder holds the block, incoming blows
 * lose most of their bite and whoever swung directly at the raised blade
 * catches fire off the arcs. Shield-piercing damage goes through
 * untouched.
 */
@EventBusSubscriber(modid = WeatherInducerMod.MOD_ID)
public final class KatanaGuardHandler {

    private KatanaGuardHandler() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !player.isUsingItem()
                || !player.getUseItem().is(ModItems.LIGHTNING_SWORD.get())) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return;
        }
        event.setAmount(event.getAmount() * 0.4f);
        if (source.getDirectEntity() instanceof LivingEntity attacker && attacker != player) {
            attacker.igniteForSeconds(4);
        }
    }
}
