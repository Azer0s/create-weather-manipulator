package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WeatherInducerMod.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + WeatherInducerMod.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.WEATHER_INDUCER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.WEATHER_INDUCER.get());
                        output.accept(ModItems.SU_RESISTOR.get());
                        output.accept(ModItems.SU_CHARGER.get());
                        output.accept(ModItems.STRESS_GATE.get());
                        output.accept(ModItems.WEATHER_SENSOR.get());
                        output.accept(ModItems.LIGHTNING_MEDIUM.get());
                        output.accept(ModItems.BOTTLE_O_LIGHTNING.get());
                        output.accept(ModItems.LIGHTNING_BOLT.get());
                        output.accept(ModItems.LIGHTNING_SWORD.get());
                        output.accept(ModItems.LIGHTNING_PICKAXE.get());
                        output.accept(ModItems.LIGHTNING_AXE.get());
                        output.accept(ModItems.LIGHTNING_SHOVEL.get());
                        output.accept(ModItems.LIGHTNING_HOE.get());
                        output.accept(ModItems.LIGHTNING_HELMET.get());
                        output.accept(ModItems.LIGHTNING_CHESTPLATE.get());
                        output.accept(ModItems.LIGHTNING_LEGGINGS.get());
                        output.accept(ModItems.LIGHTNING_BOOTS.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
