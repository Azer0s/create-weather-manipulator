package at.simulevski.weatherinducer.content.lightning;

import at.simulevski.weatherinducer.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * Shared bits of the lightning gear set: the tool tier and the checks the
 * tick handler uses. Numbers follow the mod's power-of-two convention: 4096
 * durability, mining speed 16, and the sword lands at 1,024 attack damage,
 * which one-shots everything the game has (the warden's 500 HP included).
 */
public final class LightningGear {

    public static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 4096;
        }

        @Override
        public float getSpeed() {
            return 16.0f;
        }

        @Override
        public float getAttackDamageBonus() {
            return 4.0f;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 32;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.LIGHTNING_BOLT.get());
        }
    };

    private LightningGear() {
    }

    public static boolean isLightningTool(ItemStack stack) {
        return stack.is(ModItems.LIGHTNING_SWORD.get())
                || stack.is(ModItems.LIGHTNING_PICKAXE.get())
                || stack.is(ModItems.LIGHTNING_AXE.get())
                || stack.is(ModItems.LIGHTNING_SHOVEL.get())
                || stack.is(ModItems.LIGHTNING_HOE.get());
    }

    public static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LIGHTNING_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.LIGHTNING_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.LIGHTNING_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.LIGHTNING_BOOTS.get());
    }
}
