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
        // Weather Inducer: a stepped machine. A 13px casing base carries the
        // shaft bearings on the two faces along the facing axis, with a raised
        // 12x12 copper emitter cap on top. The spinning shaft itself is drawn
        // by the block entity renderer, so the model is casing only.
        //
        // UVs are mapped 1:1 to world pixels: the base faces are 13px tall, so
        // they sample texture rows 3..16 (the textures are drawn for that
        // crop). The cap texture is a tiny atlas: rows 0..3 hold the side
        // band, rows 4..16 hold the 12x12 top face.
        ModelFile inducer = models().getBuilder("weather_inducer")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("side", modLoc("block/weather_inducer_side"))
                .texture("end", modLoc("block/weather_inducer_end"))
                .texture("bottom", modLoc("block/weather_inducer_bottom"))
                .texture("cap", modLoc("block/weather_inducer_cap"))
                .texture("particle", modLoc("block/weather_inducer_side"))
                .element()
                .from(0, 0, 0).to(16, 13, 16)
                .face(Direction.DOWN).texture("#bottom").uvs(0, 0, 16, 16)
                .cullface(Direction.DOWN).end()
                .face(Direction.UP).texture("#bottom").uvs(0, 0, 16, 16).end()
                .face(Direction.NORTH).texture("#end").uvs(0, 3, 16, 16)
                .cullface(Direction.NORTH).end()
                .face(Direction.SOUTH).texture("#end").uvs(0, 3, 16, 16)
                .cullface(Direction.SOUTH).end()
                .face(Direction.EAST).texture("#side").uvs(0, 3, 16, 16)
                .cullface(Direction.EAST).end()
                .face(Direction.WEST).texture("#side").uvs(0, 3, 16, 16)
                .cullface(Direction.WEST).end()
                .end()
                .element()
                .from(2, 13, 2).to(14, 16, 14)
                .face(Direction.UP).texture("#cap").uvs(2, 4, 14, 16).end()
                .face(Direction.NORTH).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.SOUTH).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.EAST).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.WEST).texture("#cap").uvs(2, 0, 14, 3).end()
                .end();
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
