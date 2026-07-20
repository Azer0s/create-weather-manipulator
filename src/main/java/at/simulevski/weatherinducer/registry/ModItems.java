package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.lightning.BottleOLightningItem;
import at.simulevski.weatherinducer.content.lightning.LightningGear;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
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

    public static final DeferredItem<BlockItem> CHARGER_LINK = ITEMS.registerSimpleBlockItem(
            "charger_link", ModBlocks.CHARGER_LINK, new Item.Properties());

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

    // The sword's attributes land at 1,024 attack damage, which one-shots
    // everything; the other tools hit like their netherite cousins and get
    // Efficiency V baked in by their crafting recipes.
    public static final DeferredItem<Item> LIGHTNING_SWORD = ITEMS.registerItem(
            "lightning_sword",
            props -> new SwordItem(LightningGear.TIER, props
                    .attributes(SwordItem.createAttributes(LightningGear.TIER, 1019, -2.4f))),
            new Item.Properties());

    public static final DeferredItem<Item> LIGHTNING_PICKAXE = ITEMS.registerItem(
            "lightning_pickaxe",
            props -> new PickaxeItem(LightningGear.TIER, props
                    .attributes(PickaxeItem.createAttributes(LightningGear.TIER, 1.0f, -2.8f))),
            new Item.Properties());

    public static final DeferredItem<Item> LIGHTNING_AXE = ITEMS.registerItem(
            "lightning_axe",
            props -> new AxeItem(LightningGear.TIER, props
                    .attributes(AxeItem.createAttributes(LightningGear.TIER, 5.0f, -3.0f))),
            new Item.Properties());

    public static final DeferredItem<Item> LIGHTNING_SHOVEL = ITEMS.registerItem(
            "lightning_shovel",
            props -> new ShovelItem(LightningGear.TIER, props
                    .attributes(ShovelItem.createAttributes(LightningGear.TIER, 1.5f, -3.0f))),
            new Item.Properties());

    public static final DeferredItem<Item> LIGHTNING_HOE = ITEMS.registerItem(
            "lightning_hoe",
            props -> new HoeItem(LightningGear.TIER, props
                    .attributes(HoeItem.createAttributes(LightningGear.TIER, -2.0f, 0.0f))),
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
