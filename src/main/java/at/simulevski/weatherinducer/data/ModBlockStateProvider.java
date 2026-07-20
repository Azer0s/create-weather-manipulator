package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.KineticChargerBlock;
import at.simulevski.weatherinducer.content.gate.StressGateBlock;
import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlock;
import at.simulevski.weatherinducer.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Models and blockstates. Some conventions shared by everything below:
 *
 * <ul>
 *   <li>Textures map 1:1 to world pixels, so faces mostly ride on vanilla's
 *       default UV projection; explicit UVs only appear where a texture is a
 *       small atlas (gate side, inducer cap/rod) or where a face samples
 *       the dark hole crop.</li>
 *   <li>Every shaft connection sits in a 2px deep socket (an 8x8 hole framed
 *       by four rim boxes), so the spinning shaft drawn by the block entity
 *       renderer is visible, like on other Create machines.</li>
 *   <li>The Weather Inducer and Kinetic Charger bake their fill level into the
 *       block: six models each, selected by the CHARGE property (inducer) or
 *       the POWER signal (charger), pointing at per-level side textures.</li>
 * </ul>
 */
public class ModBlockStateProvider extends BlockStateProvider {

    /**
     * The bolt emblem's silhouette as {texture row, first column, last column}.
     * Must stay in sync with the bolt pixels in the weather_inducer_side_N
     * textures; the embossed geometry samples exactly these pixels so it
     * always matches the flat art underneath.
     */
    private static final int[][] BOLT_ROWS = {
            {5, 7, 10}, {6, 7, 9}, {7, 6, 9}, {8, 6, 10}, {9, 5, 8},
            {10, 5, 7}, {11, 4, 6}, {12, 4, 5}, {13, 4, 4},
    };

    private static final int CHARGE_LEVELS = 6;

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, WeatherInducerMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerWeatherInducer();
        registerWeatherSensor();
        registerSuCharger();
        registerStressGate();
        registerLightningMedium();
        registerChargerLink();
        registerInnerShaft();
    }

    /**
     * The shaft partial the inducer and charger draw in their sockets:
     * Create's own shaft geometry and textures, pulled in 0.02px at both
     * ends. The full-length shaft puts its end caps exactly on the block
     * boundary where an attached shaft block puts its own; the coplanar
     * quads z-fight and the junction shimmers dark. The inset breaks the
     * tie invisibly. No blockstate: this model is only ever baked as a
     * Flywheel partial.
     */
    private void registerInnerShaft() {
        models().getBuilder("inner_shaft")
                .texture("axis", "create:block/axis")
                .texture("axis_top", "create:block/axis_top")
                .texture("particle", "create:block/axis")
                .element()
                .from(6, 0.02f, 6).to(10, 15.98f, 10)
                .face(Direction.NORTH).texture("#axis").uvs(6, 0, 10, 16).end()
                .face(Direction.SOUTH).texture("#axis").uvs(6, 0, 10, 16).end()
                .face(Direction.EAST).texture("#axis").uvs(6, 0, 10, 16).end()
                .face(Direction.WEST).texture("#axis").uvs(6, 0, 10, 16).end()
                .face(Direction.UP).texture("#axis_top").uvs(6, 6, 10, 10).end()
                .face(Direction.DOWN).texture("#axis_top").uvs(6, 6, 10, 10).end()
                .end();
    }

    // ------------------------------------------------------------------
    // Weather Inducer
    // ------------------------------------------------------------------

    /**
     * A stepped machine: 13px casing base (with shaft sockets on the facing
     * axis), raised copper emitter cap, lightning rod, and the bolt emblem
     * embossed on both side faces. One model per charge level; the side
     * texture variants light the bolt up as the block fills.
     */
    private void registerWeatherInducer() {
        ModelFile[] byLevel = new ModelFile[CHARGE_LEVELS];
        for (int lvl = 0; lvl < CHARGE_LEVELS; lvl++) {
            byLevel[lvl] = inducerModel(lvl);
        }
        getVariantBuilder(ModBlocks.WEATHER_INDUCER.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(byLevel[state.getValue(WeatherInducerBlock.CHARGE)])
                        .rotationY(((int) state.getValue(WeatherInducerBlock.HORIZONTAL_FACING)
                                .toYRot() + 180) % 360)
                        .build());
    }

    private BlockModelBuilder inducerModel(int chargeLevel) {
        BlockModelBuilder b = models().getBuilder("weather_inducer_" + chargeLevel)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("side", modLoc("block/weather_inducer_side_" + chargeLevel))
                .texture("end", modLoc("block/weather_inducer_end"))
                .texture("bottom", modLoc("block/weather_inducer_bottom"))
                .texture("cap", modLoc("block/weather_inducer_cap"))
                .texture("rod", modLoc("block/weather_inducer_rod"))
                .texture("particle", modLoc("block/weather_inducer_side_" + chargeLevel));

        // Casing base, pulled in 2px on the shaft axis; the end faces become
        // the socket floors.
        b.element()
                .from(0, 0, 2).to(16, 13, 14)
                .face(Direction.DOWN).texture("#bottom").cullface(Direction.DOWN).end()
                .face(Direction.UP).texture("#bottom").end()
                .face(Direction.NORTH).texture("#end").end()
                .face(Direction.SOUTH).texture("#end").end()
                .face(Direction.EAST).texture("#side").cullface(Direction.EAST).end()
                .face(Direction.WEST).texture("#side").cullface(Direction.WEST).end()
                .end();
        zSocket(b, 13, true, "end", "bottom");
        zSocket(b, 13, false, "end", "bottom");

        // Raised emitter cap. The cap texture is a tiny atlas: rows 0..3 hold
        // the side band, rows 4..16 the 12x12 top face.
        b.element()
                .from(2, 13, 2).to(14, 16, 14)
                .face(Direction.UP).texture("#cap").uvs(2, 4, 14, 16).end()
                .face(Direction.NORTH).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.SOUTH).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.EAST).texture("#cap").uvs(2, 0, 14, 3).end()
                .face(Direction.WEST).texture("#cap").uvs(2, 0, 14, 3).end()
                .end();

        // Lightning rod, centered and plugged into the aperture.
        b.element()
                .from(7, 16, 7).to(9, 20, 9)
                .face(Direction.NORTH).texture("#rod").uvs(6, 0, 8, 4).end()
                .face(Direction.SOUTH).texture("#rod").uvs(6, 0, 8, 4).end()
                .face(Direction.EAST).texture("#rod").uvs(6, 0, 8, 4).end()
                .face(Direction.WEST).texture("#rod").uvs(6, 0, 8, 4).end()
                .end();
        b.element()
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
            emboss(b, Direction.WEST, -0.5f, 0, u1, u2, y1, y2, v, u1, u2);
            emboss(b, Direction.EAST, 16, 16.5f, 16 - u2, 16 - u1, y1, y2, v, u1, u2);
        }
        return b;
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
     * Four rim boxes forming a 2px deep shaft socket on one Z end of a block
     * spanning the full 16x16 footprint, with an 8x8 hole around the shaft.
     * Textures map 1:1 to world pixels, so the rims' default UVs land on the
     * same art the flush face used; the hole walls sample the dark bearing
     * crop.
     */
    private void zSocket(BlockModelBuilder b, float yMax, boolean north,
                         String end, String plate) {
        float z0 = north ? 0 : 14;
        float z1 = north ? 2 : 16;
        Direction outer = north ? Direction.NORTH : Direction.SOUTH;

        b.element() // below the hole
                .from(0, 0, z0).to(16, 4, z1)
                .face(outer).texture("#" + end).cullface(outer).end()
                .face(Direction.DOWN).texture("#" + plate).cullface(Direction.DOWN).end()
                .face(Direction.UP).texture("#" + end).uvs(6, 6, 10, 10).end()
                .face(Direction.EAST).texture("#side").cullface(Direction.EAST).end()
                .face(Direction.WEST).texture("#side").cullface(Direction.WEST).end()
                .end();
        b.element() // above the hole
                .from(0, 12, z0).to(16, yMax, z1)
                .face(outer).texture("#" + end).cullface(outer).end()
                .face(Direction.UP).texture("#" + plate)
                .cullface(yMax == 16 ? Direction.UP : null).end()
                .face(Direction.DOWN).texture("#" + end).uvs(6, 6, 10, 10).end()
                .face(Direction.EAST).texture("#side").cullface(Direction.EAST).end()
                .face(Direction.WEST).texture("#side").cullface(Direction.WEST).end()
                .end();
        b.element() // left of the hole
                .from(0, 4, z0).to(4, 12, z1)
                .face(outer).texture("#" + end).cullface(outer).end()
                .face(Direction.WEST).texture("#side").cullface(Direction.WEST).end()
                .face(Direction.EAST).texture("#" + end).uvs(6, 6, 10, 10).end()
                .end();
        b.element() // right of the hole
                .from(12, 4, z0).to(16, 12, z1)
                .face(outer).texture("#" + end).cullface(outer).end()
                .face(Direction.EAST).texture("#side").cullface(Direction.EAST).end()
                .face(Direction.WEST).texture("#" + end).uvs(6, 6, 10, 10).end()
                .end();
    }

    // ------------------------------------------------------------------
    // Stress Gate
    // ------------------------------------------------------------------

    /**
     * The Stress Gate's flanged inline-shaft silhouette: two socketed
     * collars and an 8x10 body between them. The side
     * texture is an atlas (rows 0..3 the collar band, the patch at 4,4 the
     * body), so every face sets its UVs explicitly.
     */
    private BlockModelBuilder flangedModel(String name, String sideTexture, String endTexture) {
        BlockModelBuilder flanged = models().getBuilder(name)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("side", modLoc("block/" + sideTexture))
                .texture("end", modLoc("block/" + endTexture))
                .texture("particle", modLoc("block/" + endTexture));
        collar(flanged, true);
        collar(flanged, false);
        flanged.element()
                .from(4, 3, 4).to(12, 13, 12)
                .face(Direction.NORTH).texture("#side").uvs(4, 4, 12, 14).end()
                .face(Direction.SOUTH).texture("#side").uvs(4, 4, 12, 14).end()
                .face(Direction.EAST).texture("#side").uvs(4, 4, 12, 14).end()
                .face(Direction.WEST).texture("#side").uvs(4, 4, 12, 14).end()
                .end();
        return flanged;
    }

    /**
     * One collar flange with its shaft socket: a 1px ring slab (whose inner
     * face is the socket floor with the bearing art) and four 2px rim boxes
     * around the 8x8 hole.
     */
    private void collar(BlockModelBuilder b, boolean bottom) {
        float slabY1 = bottom ? 2 : 13;
        float slabY2 = bottom ? 3 : 14;
        float rimY1 = bottom ? 0 : 14;
        float rimY2 = bottom ? 2 : 16;
        Direction outer = bottom ? Direction.DOWN : Direction.UP;
        Direction floor = bottom ? Direction.DOWN : Direction.UP;
        Direction back = bottom ? Direction.UP : Direction.DOWN;
        // band crops: the lit row 0 sits at the collar's outer edge
        float[] slabBand = bottom ? new float[]{0, 1} : new float[]{2, 3};
        float[] rimBand = bottom ? new float[]{1, 3} : new float[]{0, 2};

        // ring slab
        BlockModelBuilder.ElementBuilder slab = b.element()
                .from(2, slabY1, 2).to(14, slabY2, 14)
                .face(floor).texture("#end").uvs(2, 2, 14, 14).end()
                .face(back).texture("#end").uvs(2, 2, 14, 14).end();
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST}) {
            slab.face(d).texture("#side").uvs(2, slabBand[0], 14, slabBand[1]).end();
        }
        slab.end();

        // rim boxes around the hole
        b.element() // north side
                .from(2, rimY1, 2).to(14, rimY2, 4)
                .face(outer).texture("#end").uvs(2, 2, 14, 4).cullface(outer).end()
                .face(Direction.NORTH).texture("#side").uvs(2, rimBand[0], 14, rimBand[1]).end()
                .face(Direction.SOUTH).texture("#end").uvs(6, 6, 10, 10).end()
                .face(Direction.EAST).texture("#side").uvs(2, rimBand[0], 4, rimBand[1]).end()
                .face(Direction.WEST).texture("#side").uvs(2, rimBand[0], 4, rimBand[1]).end()
                .end();
        b.element() // south side
                .from(2, rimY1, 12).to(14, rimY2, 14)
                .face(outer).texture("#end").uvs(2, 12, 14, 14).cullface(outer).end()
                .face(Direction.SOUTH).texture("#side").uvs(2, rimBand[0], 14, rimBand[1]).end()
                .face(Direction.NORTH).texture("#end").uvs(6, 6, 10, 10).end()
                .face(Direction.EAST).texture("#side").uvs(2, rimBand[0], 4, rimBand[1]).end()
                .face(Direction.WEST).texture("#side").uvs(2, rimBand[0], 4, rimBand[1]).end()
                .end();
        b.element() // west side
                .from(2, rimY1, 4).to(4, rimY2, 12)
                .face(outer).texture("#end").uvs(2, 4, 4, 12).cullface(outer).end()
                .face(Direction.WEST).texture("#side").uvs(4, rimBand[0], 12, rimBand[1]).end()
                .face(Direction.EAST).texture("#end").uvs(6, 6, 10, 10).end()
                .end();
        b.element() // east side
                .from(12, rimY1, 4).to(14, rimY2, 12)
                .face(outer).texture("#end").uvs(12, 4, 14, 12).cullface(outer).end()
                .face(Direction.EAST).texture("#side").uvs(4, rimBand[0], 12, rimBand[1]).end()
                .face(Direction.WEST).texture("#end").uvs(6, 6, 10, 10).end()
                .end();
    }

    // ------------------------------------------------------------------
    // Weather Sensor
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Kinetic Charger
    // ------------------------------------------------------------------

    /**
     * The Kinetic Charger is a full cube in the encased-block family, with shaft
     * sockets on both ends of the facing axis. The model's north face is the
     * output (marked with a teal discharge ring), south the input; the top
     * and bottom reuse the shared brass plate texture. One model per fill
     * level, selected from the POWER signal; the capacitor gap on the side
     * texture fills up with it.
     */
    private void registerSuCharger() {
        ModelFile[] byLevel = new ModelFile[CHARGE_LEVELS];
        for (int lvl = 0; lvl < CHARGE_LEVELS; lvl++) {
            byLevel[lvl] = chargerModel(lvl);
        }
        // DISCHARGING only changes behaviour (battery mode), not the model.
        getVariantBuilder(ModBlocks.KINETIC_CHARGER.get()).forAllStatesExcept(state ->
                // ceil-map 0..15 onto 0..5 so any non-empty buffer shows a pip
                ConfiguredModel.builder()
                        .modelFile(byLevel[(state.getValue(KineticChargerBlock.POWER)
                                * (CHARGE_LEVELS - 1) + 14) / 15])
                        .rotationY(((int) state.getValue(KineticChargerBlock.HORIZONTAL_FACING)
                                .toYRot() + 180) % 360)
                        .build(),
                KineticChargerBlock.DISCHARGING);
    }

    private BlockModelBuilder chargerModel(int fillLevel) {
        BlockModelBuilder b = models().getBuilder("kinetic_charger_" + fillLevel)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("side", modLoc("block/kinetic_charger_side_" + fillLevel))
                .texture("out", modLoc("block/kinetic_charger_out"))
                .texture("in", modLoc("block/kinetic_charger_in"))
                .texture("plate", modLoc("block/weather_inducer_bottom"))
                .texture("particle", modLoc("block/kinetic_charger_side_" + fillLevel));
        // A battery drum between two end plates. The plates carry the shaft
        // sockets and the I/O ring (north face, the front) and flywheel
        // mount (south); the drum between them is recessed and shows the
        // fill gauge, with a proud copper hoop around the middle. Nothing
        // overlaps, so no faces fight.
        for (boolean front : new boolean[]{true, false}) {
            float z0 = front ? 2 : 12;
            float z1 = front ? 4 : 14;
            b.element()
                    .from(0, 0, z0).to(16, 16, z1)
                    .face(Direction.DOWN).texture("#plate").end()
                    .face(Direction.UP).texture("#plate").end()
                    .face(Direction.NORTH).texture(front ? "#out" : "#plate").end()
                    .face(Direction.SOUTH).texture(front ? "#plate" : "#in").end()
                    .face(Direction.EAST).texture("#plate").uvs(z0, 0, z1, 16).end()
                    .face(Direction.WEST).texture("#plate").uvs(z0, 0, z1, 16).end()
                    .end();
        }
        b.element()
                .from(1.5f, 1.5f, 4).to(14.5f, 14.5f, 12)
                .face(Direction.DOWN).texture("#side").uvs(4, 1.5f, 12, 14.5f).end()
                .face(Direction.UP).texture("#side").uvs(4, 1.5f, 12, 14.5f).end()
                .face(Direction.EAST).texture("#side").uvs(4, 1.5f, 12, 14.5f).end()
                .face(Direction.WEST).texture("#side").uvs(4, 1.5f, 12, 14.5f).end()
                .end();
        b.element()
                .from(0.5f, 0.5f, 7).to(15.5f, 15.5f, 9)
                .face(Direction.DOWN).texture("#band").uvs(0, 7, 16, 9).end()
                .face(Direction.UP).texture("#band").uvs(0, 7, 16, 9).end()
                .face(Direction.EAST).texture("#band").uvs(7, 0, 9, 16).end()
                .face(Direction.WEST).texture("#band").uvs(7, 0, 9, 16).end()
                .face(Direction.NORTH).texture("#band").end()
                .face(Direction.SOUTH).texture("#band").end()
                .end();
        b.texture("band", mcLoc("block/copper_block"));
        zSocket(b, 16, true, "out", "plate");
        zSocket(b, 16, false, "in", "plate");
        return b;
    }
    /**
     * The Stress Gate wears the flanged inline silhouette; its padlock
     * body swaps between the locked (red pip) and open (teal pip) art.
     */
    private void registerStressGate() {
        ModelFile locked = flangedModel("stress_gate_locked", "stress_gate_side_locked", "stress_gate_end");
        ModelFile open = flangedModel("stress_gate_open", "stress_gate_side_open", "stress_gate_end");
        getVariantBuilder(ModBlocks.STRESS_GATE.get()).forAllStates(state -> {
            Direction.Axis axis = state.getValue(StressGateBlock.AXIS);
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
            ModelFile model = state.getValue(StressGateBlock.LOCKED) ? locked : open;
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        });
    }

    /**
     * The Charger Link, styled after Create's display link without
     * borrowing its files: the same plate-and-antenna anatomy and the same
     * atlas layout, but the two sheets are this mod's own art. Dark smoked
     * wood and brass on the plate with a teal stripe where the display
     * link wears its red one, copper coils, a steel housing with a teal
     * gauge, a little brass lightning bolt for an antenna and a teal glass
     * bulb. The model is authored in the display link's native
     * floor-mounted pose (host charger below, feet poking into it), which
     * is exactly the facing=up identity that directionalBlock rotates onto
     * all six faces.
     *
     * <p>Like the display link this is a composite: plate and antenna
     * render cutout while the glass bulb renders translucent.
     */
    private void registerChargerLink() {
        BlockModelBuilder base = models().nested()
                .renderType("cutout_mipped")
                .texture("0", modLoc("block/charger_link_base"))
                .texture("1", modLoc("block/charger_link_details"));
        // The mounting plate.
        base.element()
                .from(1, 1, 1).to(15, 5, 15)
                .face(Direction.NORTH).texture("#0").uvs(15.5f, 8.5f, 8.5f, 10.5f).end()
                .face(Direction.EAST).texture("#0").uvs(8.5f, 11, 15.5f, 13).end()
                .face(Direction.SOUTH).texture("#0").uvs(8.5f, 8.5f, 15.5f, 10.5f).end()
                .face(Direction.WEST).texture("#0").uvs(8.5f, 11, 15.5f, 13).end()
                .face(Direction.UP).texture("#0").uvs(0.5f, 8.5f, 7.5f, 15.5f).end()
                .face(Direction.DOWN).texture("#0").uvs(0.5f, 8.5f, 7.5f, 15.5f).end()
                .end();
        // The feet, sunk into the host charger's face.
        base.element()
                .from(2, -1, 2).to(14, 1, 14)
                .face(Direction.NORTH).texture("#0").uvs(9, 13, 15, 14).end()
                .face(Direction.EAST).texture("#0").uvs(9, 13, 15, 14).end()
                .face(Direction.SOUTH).texture("#0").uvs(9, 13, 15, 14).end()
                .face(Direction.WEST).texture("#0").uvs(9, 13, 15, 14).end()
                .face(Direction.DOWN).texture("#1").uvs(1, 9, 7, 15).end()
                .end();
        // The long instrument housing.
        base.element()
                .from(3, 5, 3.5f).to(7, 8, 12.5f)
                .face(Direction.NORTH).texture("#1").uvs(8, 0, 10, 1.5f).end()
                .face(Direction.EAST).texture("#1").uvs(10, 1.5f, 11.5f, 6)
                .rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .face(Direction.SOUTH).texture("#1").uvs(8, 0, 10, 1.5f).end()
                .face(Direction.WEST).texture("#1").uvs(10, 1.5f, 11.5f, 6)
                .rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .face(Direction.UP).texture("#1").uvs(8, 1.5f, 10, 6).end()
                .end();
        // The two copper coils.
        base.element()
                .from(9, 5, 3).to(13, 7, 7)
                .face(Direction.NORTH).texture("#1").uvs(6, 7, 8, 8).end()
                .face(Direction.EAST).texture("#1").uvs(6, 7, 8, 8).end()
                .face(Direction.SOUTH).texture("#1").uvs(6, 7, 8, 8).end()
                .face(Direction.WEST).texture("#1").uvs(6, 7, 8, 8).end()
                .face(Direction.UP).texture("#1").uvs(6, 0, 8, 2).end()
                .end();
        base.element()
                .from(9, 5, 9).to(13, 7, 13)
                .face(Direction.NORTH).texture("#1").uvs(6, 7, 8, 8).end()
                .face(Direction.EAST).texture("#1").uvs(6, 7, 8, 8).end()
                .face(Direction.SOUTH).texture("#1").uvs(6, 7, 8, 8).end()
                .face(Direction.WEST).texture("#1").uvs(6, 7, 8, 8).end()
                .face(Direction.UP).texture("#1").uvs(6, 2.5f, 8, 4.5f).end()
                .end();
        // The forked antenna, a flat plane above the front coil.
        base.element()
                .from(9.5f, 7, 5).to(12.5f, 11, 5)
                .face(Direction.NORTH).texture("#1").uvs(11.5f, 6, 13, 8).end()
                .face(Direction.SOUTH).texture("#1").uvs(11.5f, 6, 13, 8).end()
                .end();

        // The teal glass bulb, rendered translucent like the display
        // link's.
        BlockModelBuilder bulb = models().nested()
                .renderType("translucent")
                .texture("1", modLoc("block/charger_link_details"));
        bulb.element()
                .from(8.5f, 7, 2.5f).to(13.5f, 12, 7.5f)
                .face(Direction.NORTH).texture("#1").uvs(16, 2.5f, 13.5f, 5).end()
                .face(Direction.EAST).texture("#1").uvs(16, 2.5f, 13.5f, 5).end()
                .face(Direction.SOUTH).texture("#1").uvs(13.5f, 2.5f, 16, 5).end()
                .face(Direction.WEST).texture("#1").uvs(13.5f, 2.5f, 16, 5).end()
                .face(Direction.UP).texture("#1").uvs(13.5f, 0, 16, 2.5f).end()
                .face(Direction.DOWN).texture("#1").uvs(13.5f, 5, 16, 7.5f).end()
                .end();

        BlockModelBuilder link = models().getBuilder("charger_link")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/charger_link_base"))
                .customLoader(CompositeModelBuilder::begin)
                .child("base", base)
                .child("bulb", bulb)
                .end();
        // The display link's own item transforms, so the item reads the
        // same in hand and inventory.
        link.transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .rotation(0, 45, 0).scale(0.4f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(0, 225, 0).scale(0.4f).end()
                .transform(ItemDisplayContext.GROUND)
                .translation(0, 3, 0).scale(0.25f).end()
                .transform(ItemDisplayContext.GUI)
                .rotation(30, 225, 0).translation(0, 1.5f, 0).scale(0.625f).end()
                .transform(ItemDisplayContext.FIXED)
                .rotation(270, 0, 0).translation(0, 0, -4).scale(0.5f).end();
        directionalBlock(ModBlocks.CHARGER_LINK.get(), link);

        // The heartbeat glow: the bulb inflated half a pixel, drawn
        // fullbright into the additive layer by the link renderer while a
        // pulse runs. Baked as a Flywheel partial, never as a blockstate.
        models().getBuilder("charger_link_glow")
                .texture("glass", modLoc("block/charger_link_details"))
                .texture("particle", modLoc("block/charger_link_details"))
                .element()
                .from(8, 6.5f, 2).to(14, 12.5f, 8)
                .face(Direction.NORTH).texture("#glass").uvs(16, 2.5f, 13.5f, 5).end()
                .face(Direction.EAST).texture("#glass").uvs(16, 2.5f, 13.5f, 5).end()
                .face(Direction.SOUTH).texture("#glass").uvs(13.5f, 2.5f, 16, 5).end()
                .face(Direction.WEST).texture("#glass").uvs(13.5f, 2.5f, 16, 5).end()
                .face(Direction.UP).texture("#glass").uvs(13.5f, 0, 16, 2.5f).end()
                .face(Direction.DOWN).texture("#glass").uvs(13.5f, 5, 16, 7.5f).end()
                .end();
    }

    /** The Lightning Medium: beacon and end crystal encased in glass. */
    private void registerLightningMedium() {
        // End crystal architecture: an obsidian pedestal slab with the glass
        // shell sitting inset on top of it. The crystal inside is drawn by
        // the block entity renderer.
        BlockModelBuilder medium = models().getBuilder("lightning_medium")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("glass", modLoc("block/lightning_medium_side"))
                .texture("glass_top", modLoc("block/lightning_medium_top"))
                .texture("base", modLoc("block/lightning_medium_base"))
                .texture("bottom", modLoc("block/lightning_medium_bottom"))
                .texture("particle", modLoc("block/lightning_medium_base"))
                // The glass shell is mostly transparent pixels.
                .renderType("cutout");
        medium.element()
                .from(0, 0, 0).to(16, 4, 16)
                .face(Direction.UP).texture("#base").end()
                .face(Direction.DOWN).texture("#bottom").end()
                .face(Direction.NORTH).texture("#base").uvs(0, 0, 16, 4).end()
                .face(Direction.SOUTH).texture("#base").uvs(0, 0, 16, 4).end()
                .face(Direction.EAST).texture("#base").uvs(0, 0, 16, 4).end()
                .face(Direction.WEST).texture("#base").uvs(0, 0, 16, 4).end()
                .end();
        medium.element()
                .from(1, 4, 1).to(15, 16, 15)
                .face(Direction.UP).texture("#glass_top").uvs(1, 1, 15, 15).end()
                .face(Direction.NORTH).texture("#glass").uvs(1, 0, 15, 12).end()
                .face(Direction.SOUTH).texture("#glass").uvs(1, 0, 15, 12).end()
                .face(Direction.EAST).texture("#glass").uvs(1, 0, 15, 12).end()
                .face(Direction.WEST).texture("#glass").uvs(1, 0, 15, 12).end()
                .end();
        simpleBlock(ModBlocks.LIGHTNING_MEDIUM.get(), medium);
    }

}
