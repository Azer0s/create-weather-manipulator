package at.simulevski.weatherinducer.content.charger;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/**
 * The Charger Link's item. A stack that has been bound to a charger
 * network (by right-clicking a placed link) shimmers with the enchantment
 * glint in the inventory, so bound and fresh links are told apart at a
 * glance.
 */
public class ChargerLinkItem extends BlockItem {

    public ChargerLinkItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().hasUUID(ChargerLinkBlock.NETWORK_KEY);
    }
}
