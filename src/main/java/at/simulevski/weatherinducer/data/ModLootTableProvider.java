package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends LootTableProvider {

    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(),
                List.of(new SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)),
                registries);
    }

    /** All blocks simply drop themselves. */
    public static class ModBlockLoot extends BlockLootSubProvider {
        public ModBlockLoot(HolderLookup.Provider registries) {
            super(Set.<Item>of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(ModBlocks.WEATHER_INDUCER.get());
            dropSelf(ModBlocks.WEATHER_SENSOR.get());
            dropSelf(ModBlocks.KINETIC_CHARGER.get());
            dropSelf(ModBlocks.CHARGER_LINK.get());
            dropSelf(ModBlocks.STRESS_GATE.get());
            dropSelf(ModBlocks.LIGHTNING_MEDIUM.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(ModBlocks.WEATHER_INDUCER.get(), ModBlocks.CHARGER_LINK.get(),
                    ModBlocks.WEATHER_SENSOR.get(), ModBlocks.KINETIC_CHARGER.get(),
                    ModBlocks.STRESS_GATE.get(), ModBlocks.LIGHTNING_MEDIUM.get());
        }
    }
}
