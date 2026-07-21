package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.ChargerLinkItem;
import at.simulevski.weatherinducer.content.lightning.BottleOLightningItem;
import at.simulevski.weatherinducer.content.lightning.LightningGear;
import at.simulevski.weatherinducer.content.lightning.LightningKatanaItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WeatherInducerMod.MOD_ID);

    public static final DeferredItem<BlockItem> WEATHER_INDUCER = ITEMS.registerSimpleBlockItem(
            "weather_inducer", ModBlocks.WEATHER_INDUCER, new Item.Properties());

    public static final DeferredItem<BlockItem> WEATHER_SENSOR = ITEMS.registerSimpleBlockItem(
            "weather_sensor", ModBlocks.WEATHER_SENSOR, new Item.Properties());

    public static final DeferredItem<BlockItem> KINETIC_CHARGER = ITEMS.registerSimpleBlockItem(
            "kinetic_charger", ModBlocks.KINETIC_CHARGER, new Item.Properties());

    // Registered with its own item class so a network-bound stack shimmers
    // like an enchanted item.
    public static final DeferredItem<BlockItem> CHARGER_LINK = ITEMS.register(
            "charger_link",
            () -> new ChargerLinkItem(ModBlocks.CHARGER_LINK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> STRESS_GATE = ITEMS.registerSimpleBlockItem(
            "stress_gate", ModBlocks.STRESS_GATE, new Item.Properties());

    public static final DeferredItem<BlockItem> LIGHTNING_MEDIUM = ITEMS.registerSimpleBlockItem(
            "lightning_medium", ModBlocks.LIGHTNING_MEDIUM, new Item.Properties());

    // --- The lightning gear chain ---------------------------------------

    public static final DeferredItem<Item> BOTTLE_O_LIGHTNING = ITEMS.registerItem(
            "bottle_o_lightning", BottleOLightningItem::new,
            new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> LIGHTNING_BOLT = ITEMS.registerSimpleItem(
            "lightning_bolt", new Item.Properties());

    // The katana's attributes land at 1,024 attack damage, which one-shots
    // everything up to and including the warden.
    public static final DeferredItem<Item> LIGHTNING_SWORD = ITEMS.registerItem(
            "lightning_sword",
            props -> new LightningKatanaItem(LightningGear.TIER, props
                    .attributes(SwordItem.createAttributes(LightningGear.TIER, 1019, -2.4f))),
            new Item.Properties());

    public static final DeferredItem<Item> LIGHTNING_HELMET = ITEMS.registerItem(
            "lightning_helmet",
            props -> new ArmorItem(ModArmorMaterials.LIGHTNING, ArmorItem.Type.HELMET,
                    props.durability(4096)),
            new Item.Properties());

    public static final DeferredItem<Item> LIGHTNING_CHESTPLATE = ITEMS.registerItem(
            "lightning_chestplate",
            props -> new ArmorItem(ModArmorMaterials.LIGHTNING, ArmorItem.Type.CHESTPLATE,
                    props.durability(4096)),
            new Item.Properties());

    public static final DeferredItem<Item> LIGHTNING_LEGGINGS = ITEMS.registerItem(
            "lightning_leggings",
            props -> new ArmorItem(ModArmorMaterials.LIGHTNING, ArmorItem.Type.LEGGINGS,
                    props.durability(4096)),
            new Item.Properties());

    public static final DeferredItem<Item> LIGHTNING_BOOTS = ITEMS.registerItem(
            "lightning_boots",
            props -> new ArmorItem(ModArmorMaterials.LIGHTNING, ArmorItem.Type.BOOTS,
                    props.durability(4096)),
            new Item.Properties());

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
