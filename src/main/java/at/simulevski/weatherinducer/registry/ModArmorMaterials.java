package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

public final class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, WeatherInducerMod.MOD_ID);

    /**
     * The lightning set. Power-of-two protection per piece, netherite-plus
     * toughness, full knockback resistance; the worn model renders from
     * textures/models/armor/lightning_layer_{1,2}.png.
     */
    public static final Holder<ArmorMaterial> LIGHTNING = ARMOR_MATERIALS.register("lightning",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 8,
                            ArmorItem.Type.LEGGINGS, 8,
                            ArmorItem.Type.CHESTPLATE, 16,
                            ArmorItem.Type.HELMET, 8),
                    32,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.LIGHTNING_BOLT.get()),
                    List.of(new ArmorMaterial.Layer(WeatherInducerMod.asResource("lightning"))),
                    16.0f,
                    1.0f));

    private ModArmorMaterials() {
    }

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }
}
