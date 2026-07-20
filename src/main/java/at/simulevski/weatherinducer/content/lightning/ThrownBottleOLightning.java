package at.simulevski.weatherinducer.content.lightning;

import at.simulevski.weatherinducer.registry.ModEntityTypes;
import at.simulevski.weatherinducer.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * A Bottle o' Lightning in flight. Thrown like a snowball; whatever it hits,
 * block or creature, gets a lightning strike on the spot. Throwing one at
 * another Lightning Medium bottles the strike right back.
 */
public class ThrownBottleOLightning extends ThrowableItemProjectile {

    public ThrownBottleOLightning(EntityType<? extends ThrownBottleOLightning> type, Level level) {
        super(type, level);
    }

    public ThrownBottleOLightning(Level level, LivingEntity shooter) {
        super(ModEntityTypes.THROWN_BOTTLE_O_LIGHTNING.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BOTTLE_O_LIGHTNING.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
        if (bolt != null) {
            bolt.moveTo(result.getLocation());
            level().addFreshEntity(bolt);
        }
        level().broadcastEntityEvent(this, (byte) 3); // shatter particles
        discard();
    }
}
