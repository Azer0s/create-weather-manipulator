package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.SUChargerBlock;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlock;
import at.simulevski.weatherinducer.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    /**
     * The bolt emblem's silhouette as {texture row, first column, last column}.
     * Must stay in sync with the bolt pixels in weather_inducer_side.png; the
     * embossed geometry samples exactly these pixels so it always matches the
     * flat art underneath.
     */
    private static final int[][] BOLT_ROWS = {
            {5, 7, 10}, {6, 7, 9}, {7, 6, 9}, {8, 6, 10}, {9, 5, 8},
            {10, 5, 7}, {11, 4, 6}, {12, 4, 5}, {13, 4, 4},
    };

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, WeatherInducerMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerWeatherInducer();
        registerSuResistor();
        registerWeatherSensor();
        registerSuCharger();
    }

    /**
     * The Weather Inducer is a stepped machine. A 13px casing base carries the
     * shaft bearings on the two faces along the facing axis, a raised 12x12
     * copper emitter cap sits on top, and the bolt emblem is embossed half a
     * pixel proud of both side faces. The spinning shaft itself is drawn by
     * the block entity renderer, so the model is casing only.
     *
     * <p>UVs are mapped 1:1 to world pixels: the base faces are 13px tall, so
     * they sample texture rows 3..16 (the textures are drawn for that crop).
     * The cap texture is a tiny atlas: rows 0..3 hold the side band, rows
     * 4..16 hold the 12x12 top face.
     */
    private void registerWeatherInducer() {
        BlockModelBuilder inducer = models().getBuilder("weather_inducer")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("side", modLoc("block/weather_inducer_side"))
                .texture("end", modLoc("block/weather_inducer_end"))
                .texture("bottom", modLoc("block/weather_inducer_bottom"))
                .texture("cap", modLoc("block/weather_inducer_cap"))
                .texture("rod", modLoc("block/weather_inducer_rod"))
                .texture("particle", modLoc("block/weather_inducer_side"));
        inducer.element()
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
                .end();
        inducer.element()
                .from(2, 13, 2).to(14, 16, 14)
                .face(Direction.UP).texture("#cap").uvs(2, 4, 14, 16).end()
                .face(Direction.NORTH).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.SOUTH).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.EAST).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.WEST).texture("#cap").uvs(2, 0, 14, 3).end()
                .end();

        // Lightning rod, centered on the cap and plugged into the aperture:
        // a 2x2 copper pole with the vanilla rod's thicker 4x4 tip.
        inducer.element()
                .from(7, 16, 7).to(9, 20, 9)
                .face(Direction.NORTH).texture("#rod").uvs(6, 0, 8, 4).end()
                .face(Direction.SOUTH).texture("#rod").uvs(6, 0, 8, 4).end()
                .face(Direction.EAST).texture("#rod").uvs(6, 0, 8, 4).end()
                .face(Direction.WEST).texture("#rod").uvs(6, 0, 8, 4).end()
                .end();
        inducer.element()
                .from(6, 20, 6).to(10, 23, 10)
                .face(Direction.UP).texture("#rod").uvs(0, 4, 4, 8).end()
                .face(Direction.DOWN).texture("#rod").uvs(0, 4, 4, 8).end()
                .face(Direction.NORTH).texture("#rod").uvs(0, 0, 4, 3).end()
                .face(Direction.SOUTH).texture("#rod").uvs(0, 0, 4, 3).end()
                .face(Direction.EAST).texture("#rod").uvs(0, 0, 4, 3).end()
                .face(Direction.WEST).texture("#rod").uvs(0, 0, 4, 3).end()
                .end();

        // Embossed bolt: one thin box per texture row, on both side faces.
        // The west face maps texture u along +Z, the east face along -Z, so
        // the east boxes are placed mirrored to cover the same flat pixels.
        for (int[] row : BOLT_ROWS) {
            int v = row[0];
            int u1 = row[1];
            int u2 = row[2] + 1;
            float y1 = 15 - v;
            float y2 = 16 - v;
            emboss(inducer, Direction.WEST, -0.5f, 0, u1, u2, y1, y2, v, u1, u2);
            emboss(inducer, Direction.EAST, 16, 16.5f, 16 - u2, 16 - u1, y1, y2, v, u1, u2);
        }

        horizontalBlock(ModBlocks.WEATHER_INDUCER.get(), inducer);
    }

    /** One embossed bolt row: an outer face plus its four thin rims. */
    private void emboss(BlockModelBuilder builder, Direction out, float x1, float x2,
                        float z1, float z2, float y1, float y2, int v, int u1, int u2) {
        BlockModelBuilder.ElementBuilder element = builder.element()
                .from(x1, y1, z1).to(x2, y2, z2)
                .face(out).texture("#side").uvs(u1, v, u2, v + 1).end();
        for (Direction rim : new Direction[]{Direction.UP, Direction.DOWN,
                Direction.NORTH, Direction.SOUTH}) {
            element.face(rim).texture("#side").uvs(u1, v, u1 + 1, v + 1).end();
        }
        element.end();
    }

    /**
     * The SU Resistor looks like its namesake: two andesite collar flanges at
     * the shaft ends and the banded ceramic body suspended between them. The
     * side texture is an atlas (rows 0..3 the collar band, the 8x10 patch at
     * 4,4 the body side); the end texture's middle 12x12 covers the collar
     * ends, keeping the bearing centered.
     */
    private void registerSuResistor() {
        BlockModelBuilder resistor = models().getBuilder("su_resistor")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("side", modLoc("block/su_resistor_side"))
                .texture("end", modLoc("block/su_resistor_end"))
                .texture("particle", modLoc("block/su_resistor_end"));
        collar(resistor, 0, 3, Direction.DOWN);
        collar(resistor, 13, 16, Direction.UP);
        resistor.element()
                .from(4, 3, 4).to(12, 13, 12)
                .face(Direction.NORTH).texture("#side").uvs(4, 4, 12, 14).end()
                .face(Direction.SOUTH).texture("#side").uvs(4, 4, 12, 14).end()
                .face(Direction.EAST).texture("#side").uvs(4, 4, 12, 14).end()
                .face(Direction.WEST).texture("#side").uvs(4, 4, 12, 14).end()
                .end();

        ModelFile model = resistor;
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
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        });
    }

    /**
     * The Weather Sensor borrows the daylight detector's form factor: a 6px
     * slab whose sensor face is the top. The side texture carries its art in
     * rows 10..16 so UVs stay 1:1 with world pixels. One model serves all 16
     * power values.
     */
    private void registerWeatherSensor() {
        BlockModelBuilder sensor = models().getBuilder("weather_sensor")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("top", modLoc("block/weather_sensor_top"))
                .texture("side", modLoc("block/weather_sensor_side"))
                .texture("bottom", modLoc("block/weather_sensor_bottom"))
                .texture("particle", modLoc("block/weather_sensor_side"));
        sensor.element()
                .from(0, 0, 0).to(16, 6, 16)
                .face(Direction.UP).texture("#top").uvs(0, 0, 16, 16).end()
                .face(Direction.DOWN).texture("#bottom").uvs(0, 0, 16, 16)
                .cullface(Direction.DOWN).end()
                .face(Direction.NORTH).texture("#side").uvs(0, 10, 16, 16)
                .cullface(Direction.NORTH).end()
                .face(Direction.SOUTH).texture("#side").uvs(0, 10, 16, 16)
                .cullface(Direction.SOUTH).end()
                .face(Direction.EAST).texture("#side").uvs(0, 10, 16, 16)
                .cullface(Direction.EAST).end()
                .face(Direction.WEST).texture("#side").uvs(0, 10, 16, 16)
                .cullface(Direction.WEST).end()
                .end();
        getVariantBuilder(ModBlocks.WEATHER_SENSOR.get())
                .partialState().setModels(new ConfiguredModel(sensor));
    }

    /**
     * The SU Charger is a full cube in the encased-block family. The model's
     * north face is the output (marked with a teal discharge ring), south the
     * input; the top and bottom reuse the shared brass plate texture. The
     * POWER property (its redstone output) is visual-neutral, so the variants
     * only follow the facing.
     */
    private void registerSuCharger() {
        ResourceLocation side = modLoc("block/su_charger_side");
        ResourceLocation plate = modLoc("block/weather_inducer_bottom");
        ModelFile charger = models().cube("su_charger",
                        plate, plate,
                        modLoc("block/su_charger_out"),
                        modLoc("block/su_charger_in"),
                        side, side)
                .texture("particle", side);
        getVariantBuilder(ModBlocks.SU_CHARGER.get()).forAllStatesExcept(state ->
                        ConfiguredModel.builder()
                                .modelFile(charger)
                                .rotationY(((int) state.getValue(SUChargerBlock.HORIZONTAL_FACING)
                                        .toYRot() + 180) % 360)
                                .build(),
                SUChargerBlock.POWER);
    }

    /** One collar flange; the face towards the block edge gets the cullface. */
    private void collar(BlockModelBuilder builder, float y1, float y2, Direction end) {
        builder.element()
                .from(2, y1, 2).to(14, y2, 14)
                .face(Direction.UP).texture("#end").uvs(2, 2, 14, 14)
                .cullface(end == Direction.UP ? Direction.UP : null).end()
                .face(Direction.DOWN).texture("#end").uvs(2, 2, 14, 14)
                .cullface(end == Direction.DOWN ? Direction.DOWN : null).end()
                .face(Direction.NORTH).texture("#side").uvs(2, 0, 14, 3).end()
                .face(Direction.SOUTH).texture("#side").uvs(2, 0, 14, 3).end()
                .face(Direction.EAST).texture("#side").uvs(2, 0, 14, 3).end()
                .face(Direction.WEST).texture("#side").uvs(2, 0, 14, 3).end()
                .end();
    }
}
