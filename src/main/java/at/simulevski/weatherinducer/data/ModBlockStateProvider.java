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
        // Weather Inducer: brass casing with a copper emitter plate on top and
        // shaft bearings on the two faces along the facing axis. The spinning
        // shaft itself is drawn by the block entity renderer, so the model is
        // casing only.
        ResourceLocation inducerSide = modLoc("block/weather_inducer_side");
        ResourceLocation inducerEnd = modLoc("block/weather_inducer_end");
        ModelFile inducer = models().cube("weather_inducer",
                        modLoc("block/weather_inducer_bottom"),
                        modLoc("block/weather_inducer_top"),
                        inducerEnd, inducerEnd,
                        inducerSide, inducerSide)
                .texture("particle", inducerSide);
        horizontalBlock(ModBlocks.WEATHER_INDUCER.get(), inducer);

        // SU Resistor: andesite frame with a banded ceramic resistor body along
        // the shaft, bearings on the two ends, oriented on AXIS.
        ModelFile resistor = models().cubeColumn("su_resistor",
                modLoc("block/su_resistor_side"),
                modLoc("block/su_resistor_end"));
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
