package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WeatherInducerMod.MOD_ID);

    public static final DeferredItem<BlockItem> WEATHER_INDUCER = ITEMS.registerSimpleBlockItem(
            "weather_inducer", ModBlocks.WEATHER_INDUCER, new Item.Properties());

    public static final DeferredItem<BlockItem> SU_RESISTOR = ITEMS.registerSimpleBlockItem(
            "su_resistor", ModBlocks.SU_RESISTOR, new Item.Properties());

    public static final DeferredItem<BlockItem> WEATHER_SENSOR = ITEMS.registerSimpleBlockItem(
            "weather_sensor", ModBlocks.WEATHER_SENSOR, new Item.Properties());

    public static final DeferredItem<BlockItem> SU_CHARGER = ITEMS.registerSimpleBlockItem(
            "su_charger", ModBlocks.SU_CHARGER, new Item.Properties());

    public static final DeferredItem<BlockItem> STRESS_GATE = ITEMS.registerSimpleBlockItem(
            "stress_gate", ModBlocks.STRESS_GATE, new Item.Properties());

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
