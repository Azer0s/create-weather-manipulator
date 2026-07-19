package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlock;
import at.simulevski.weatherinducer.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, WeatherInducerMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Weather Inducer: brass casing body with a copper top; the shaft is
        // drawn by the block entity renderer, so the model is casing only.
        ModelFile inducer = models().cubeBottomTop("weather_inducer",
                ResourceLocation.parse("create:block/brass_casing"),
                ResourceLocation.parse("create:block/brass_casing"),
                ResourceLocation.parse("minecraft:block/copper_block"));
        horizontalBlock(ModBlocks.WEATHER_INDUCER.get(), inducer);

        // SU Resistor: andesite casing column with brass ends, oriented on AXIS.
        ModelFile resistor = models().cubeColumn("su_resistor",
                ResourceLocation.parse("create:block/andesite_casing"),
                ResourceLocation.parse("create:block/brass_casing"));
        getVariantBuilder(ModBlocks.SU_RESISTOR.get()).forAllStates(state -> {
            Direction.Axis axis = state.getValue(SUResistorBlock.AXIS);
            int x = 0;
            int y = 0;
            switch (axis) {
                case Z -> x = 90;
                case X -> {
                    x = 90;
                    y = 90;
                }
                default -> {
                    // Y: default orientation.
                }
            }
            return ConfiguredModel.builder().modelFile(resistor).rotationX(x).rotationY(y).build();
        });
    }
}
