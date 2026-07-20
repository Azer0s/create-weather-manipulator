package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Registers all data providers. Run with {@code ./gradlew runData}; output goes
 * to {@code src/generated/resources} (added to the main resources in
 * build.gradle). Crafting recipes are intentionally kept as hand-written JSON
 * because they reference Create items.
 */
@EventBusSubscriber(modid = WeatherInducerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModDataGenerators {

    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        generator.addProvider(event.includeClient(),
                new ModBlockStateProvider(output, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(),
                new ModLanguageProvider(output));

        generator.addProvider(event.includeServer(),
                new ModLootTableProvider(output, event.getLookupProvider()));
        ModBlockTagsProvider blockTags = generator.addProvider(event.includeServer(),
                new ModBlockTagsProvider(output, event.getLookupProvider(), event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, event.getLookupProvider(),
                        blockTags.contentsGetter(), event.getExistingFileHelper()));
    }
}
