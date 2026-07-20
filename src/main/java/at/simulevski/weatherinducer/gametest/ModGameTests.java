package at.simulevski.weatherinducer.gametest;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.SUChargerBlock;
import at.simulevski.weatherinducer.content.charger.SUChargerBlockEntity;
import at.simulevski.weatherinducer.content.gate.StressGateBlock;
import at.simulevski.weatherinducer.content.gate.StressGateBlockEntity;
import at.simulevski.weatherinducer.content.util.SUValueLadder;
import at.simulevski.weatherinducer.content.inducer.ChargeTimeScrollBehaviour;
import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlockEntity;
import at.simulevski.weatherinducer.content.inducer.WeatherMode;
import at.simulevski.weatherinducer.content.sensor.WeatherSensorBlock;
import at.simulevski.weatherinducer.registry.ModBlocks;
import at.simulevski.weatherinducer.content.lightning.ThrownBottleOLightning;
import at.simulevski.weatherinducer.registry.ModEntityTypes;
import at.simulevski.weatherinducer.registry.ModItems;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Runtime tests for the Weather Inducer's fire logic, exercised headlessly with
 * {@code ./gradlew runGameTestServer}. They drive the deterministic fire path
 * (charge gate, sky gate, redstone rising edge, mode dispatch, discharge)
 * without needing a live kinetic network, by forcing the charge via a test hook.
 *
 * <p>All assertions are on <em>local</em> state (the block's own charge, and a
 * spawned lightning entity) rather than global weather, so the tests are stable
 * even though game tests run in parallel.
 */
@GameTestHolder(WeatherInducerMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ModGameTests {

    private static final BlockPos INDUCER = new BlockPos(3, 2, 3);
    private static final BlockPos REDSTONE = new BlockPos(3, 2, 2);

    private static void placeFloor(GameTestHelper helper) {
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static WeatherInducerBlockEntity inducer(GameTestHelper helper) {
        return helper.getBlockEntity(INDUCER);
    }

    /** Full charge + sky + redstone pulse -> fires and discharges to 0. */
    @GameTest(template = "empty")
    public static void firesAndDischarges(GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    helper.setBlock(INDUCER, ModBlocks.WEATHER_INDUCER.get());
                    WeatherInducerBlockEntity be = inducer(helper);
                    be.setModeForTesting(WeatherMode.RAIN.ordinal());
                    be.setChargeForTesting(WeatherInducerBlockEntity.MAX_CHARGE);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK))
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(inducer(helper).getCharge() == 0.0,
                        "Inducer should discharge to 0 SU after firing"))
                .thenSucceed();
    }

    /** Lightning mode spawns a lightning bolt entity in the arena. */
    @GameTest(template = "empty")
    public static void lightningStrikes(GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    helper.setBlock(INDUCER, ModBlocks.WEATHER_INDUCER.get());
                    WeatherInducerBlockEntity be = inducer(helper);
                    be.setModeForTesting(WeatherMode.LIGHTNING.ordinal());
                    be.setChargeForTesting(WeatherInducerBlockEntity.MAX_CHARGE);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK))
                // The bolt is transient and lands on the surface (which in a
                // headless test world may be well below the small arena box), so
                // poll a wide region around the inducer every tick until it appears.
                .thenWaitUntil(() -> {
                    AABB area = new AABB(helper.absolutePos(INDUCER)).inflate(128);
                    helper.assertTrue(
                            !helper.getLevel().getEntitiesOfClass(LightningBolt.class, area).isEmpty(),
                            "Lightning mode should spawn a lightning bolt");
                })
                .thenSucceed();
    }

    /** A block above the inducer blocks its sky line-of-sight; it must not fire. */
    @GameTest(template = "empty")
    public static void skyBlockedDoesNotFire(GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    helper.setBlock(INDUCER, ModBlocks.WEATHER_INDUCER.get());
                    helper.setBlock(INDUCER.above(), Blocks.STONE);
                    WeatherInducerBlockEntity be = inducer(helper);
                    be.setModeForTesting(WeatherMode.RAIN.ordinal());
                    be.setChargeForTesting(WeatherInducerBlockEntity.MAX_CHARGE);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK))
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(
                        inducer(helper).getCharge() == WeatherInducerBlockEntity.MAX_CHARGE,
                        "Inducer without sky access must not fire or discharge"))
                .thenSucceed();
    }

    /**
     * The charge time slider paces the intake: at the slowest setting
     * (1,280 s) the inducer sips about 41 SU per tick, so after a second
     * it must hold only a few hundred SU even on an oversized network.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void chargeTimeSliderPacesIntake(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(2, 2, 3);
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    Block motor = BuiltInRegistries.BLOCK
                            .get(ResourceLocation.parse("create:creative_motor"));
                    helper.setBlock(motorPos, motor.defaultBlockState()
                            .setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                    helper.setBlock(INDUCER, ModBlocks.WEATHER_INDUCER.get().defaultBlockState()
                            .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
                    WeatherInducerBlockEntity be = inducer(helper);
                    be.setChargeTimeIndexForTesting(ChargeTimeScrollBehaviour.SECONDS.length - 1);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    double charge = inducer(helper).getCharge();
                    helper.assertTrue(charge > 0,
                            "The inducer should charge from the motor's spare SU");
                    helper.assertTrue(charge <= 41 * 24,
                            "The slider must pace the intake, got " + charge + " SU");
                })
                .thenSucceed();
    }

    /**
     * The Weather Sensor tracks the global weather. Runs in its own batch so
     * flipping the weather cannot race the other tests (batches run
     * sequentially, tests within a batch in parallel). The generous timeout
     * is needed because rain/thunder levels ramp at 0.01 per tick, so each
     * weather flip takes around 100 ticks to register.
     */
    @GameTest(template = "empty", batch = "weather", timeoutTicks = 400)
    public static void weatherSensorReadsWeather(GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    helper.setBlock(INDUCER, ModBlocks.WEATHER_SENSOR.get());
                    helper.getLevel().setWeatherParameters(0, 6000, true, true);
                })
                .thenWaitUntil(() -> helper.assertBlockState(INDUCER,
                        state -> state.getValue(WeatherSensorBlock.POWER) == 15,
                        () -> "Weather Sensor should read 15 in a thunderstorm"))
                .thenExecute(() -> helper.getLevel().setWeatherParameters(6000, 0, false, false))
                .thenWaitUntil(() -> helper.assertBlockState(INDUCER,
                        state -> state.getValue(WeatherSensorBlock.POWER) == 0,
                        () -> "Weather Sensor should read 0 under clear skies"))
                .thenSucceed();
    }

    /**
     * With no rotation in the arena the charger's input counts as stopped, so
     * a filled buffer is on offer, its level shows as redstone, and draining
     * it empties both.
     */
    @GameTest(template = "empty")
    public static void chargerDischargesWhenInputStopped(GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    helper.setBlock(INDUCER, ModBlocks.SU_CHARGER.get());
                    SUChargerBlockEntity be = helper.getBlockEntity(INDUCER);
                    be.setBufferForTesting(SUChargerBlockEntity.MAX_BUFFER);
                    be.setChargeSpeedForTesting(16); // as if it had charged at 16 rpm
                })
                .thenIdle(4) // let it flip to battery mode and publish redstone
                .thenExecute(() -> {
                    SUChargerBlockEntity be = helper.getBlockEntity(INDUCER);
                    helper.assertTrue(be.isDischarging(),
                            "A stopped charger with a full buffer must be discharging");
                    helper.assertTrue(
                            be.availableDischarge() == SUChargerBlockEntity.MAX_RATE_PER_TICK,
                            "Discharge offer should be capped at the per-tick rate");
                    helper.assertBlockState(INDUCER,
                            state -> state.getValue(SUChargerBlock.POWER) == 15,
                            () -> "A full charger should emit redstone 15");
                    double taken = be.drain(1234);
                    helper.assertTrue(taken == 1234,
                            "Draining should hand out the requested amount");
                    helper.assertTrue(
                            be.getBuffer() == SUChargerBlockEntity.MAX_BUFFER - 1234,
                            "The buffer should shrink by exactly the drained amount");
                    be.drain(SUChargerBlockEntity.MAX_BUFFER);
                })
                .thenIdle(4)
                .thenExecute(() -> {
                    SUChargerBlockEntity be = helper.getBlockEntity(INDUCER);
                    helper.assertTrue(!be.isDischarging(),
                            "An empty charger has nothing to discharge");
                    helper.assertBlockState(INDUCER,
                            state -> state.getValue(SUChargerBlock.POWER) == 0,
                            () -> "An empty charger should emit no redstone");
                })
                .thenSucceed();
    }

    /**
     * The battery on real kinetics: a motor charges the charger while
     * rotation passes through to a fan; cut the motor, and the charger
     * takes over as the source, spinning the fan from its buffer and
     * draining it by the stress the fan uses.
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void chargerDrivesOutputFromBuffer(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(2, 2, 3);
        BlockPos chargerPos = new BlockPos(3, 2, 3);
        BlockPos fanPos = new BlockPos(4, 2, 3);
        double[] bufferBefore = new double[1];
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    Block motor = BuiltInRegistries.BLOCK
                            .get(ResourceLocation.parse("create:creative_motor"));
                    Block fan = BuiltInRegistries.BLOCK
                            .get(ResourceLocation.parse("create:encased_fan"));
                    helper.setBlock(motorPos, motor.defaultBlockState()
                            .setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                    // Output face east, towards the fan; the motor feeds the back.
                    helper.setBlock(chargerPos, ModBlocks.SU_CHARGER.get().defaultBlockState()
                            .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
                    helper.setBlock(fanPos, fan.defaultBlockState()
                            .setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                })
                .thenWaitUntil(() -> {
                    KineticBlockEntity fanBe = helper.getBlockEntity(fanPos);
                    helper.assertTrue(fanBe.getSpeed() != 0,
                            "While charging, rotation should pass through to the fan");
                })
                .thenExecute(() -> {
                    SUChargerBlockEntity charger = helper.getBlockEntity(chargerPos);
                    charger.setBufferForTesting(65_536); // 2^16
                    helper.setBlock(motorPos, Blocks.AIR);
                })
                .thenWaitUntil(() -> {
                    SUChargerBlockEntity charger = helper.getBlockEntity(chargerPos);
                    KineticBlockEntity fanBe = helper.getBlockEntity(fanPos);
                    helper.assertTrue(charger.isDischarging() && fanBe.getSpeed() != 0,
                            "With the input gone the charger must drive the fan from its buffer");
                })
                .thenExecute(() -> {
                    SUChargerBlockEntity charger = helper.getBlockEntity(chargerPos);
                    bufferBefore[0] = charger.getBuffer();
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    SUChargerBlockEntity charger = helper.getBlockEntity(chargerPos);
                    helper.assertTrue(charger.getBuffer() < bufferBefore[0],
                            "Driving the fan must drain the buffer");
                })
                .thenSucceed();
    }

    /**
     * The battery behind a clutch, the setup the README recommends: the cut
     * clutch keeps spinning on its motor side, and an early version of the
     * charger read that raw speed as "input is back", flipping out of
     * battery mode every other tick so the output never kept its rotation.
     * The charger must stay in battery mode and drive the fan while the
     * clutch is powered.
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void chargerDischargesBehindPoweredClutch(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(1, 2, 3);
        BlockPos clutchPos = new BlockPos(2, 2, 3);
        BlockPos chargerPos = new BlockPos(3, 2, 3);
        BlockPos fanPos = new BlockPos(4, 2, 3);
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    Block motor = BuiltInRegistries.BLOCK
                            .get(ResourceLocation.parse("create:creative_motor"));
                    Block clutch = BuiltInRegistries.BLOCK
                            .get(ResourceLocation.parse("create:clutch"));
                    Block fan = BuiltInRegistries.BLOCK
                            .get(ResourceLocation.parse("create:encased_fan"));
                    helper.setBlock(motorPos, motor.defaultBlockState()
                            .setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                    helper.setBlock(clutchPos, clutch.defaultBlockState()
                            .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
                    helper.setBlock(chargerPos, ModBlocks.SU_CHARGER.get().defaultBlockState()
                            .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
                    helper.setBlock(fanPos, fan.defaultBlockState()
                            .setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                })
                .thenWaitUntil(() -> {
                    KineticBlockEntity fanBe = helper.getBlockEntity(fanPos);
                    helper.assertTrue(fanBe.getSpeed() != 0,
                            "The fan should spin through the open clutch and charger");
                })
                .thenExecute(() -> {
                    SUChargerBlockEntity charger = helper.getBlockEntity(chargerPos);
                    charger.setBufferForTesting(65_536); // 2^16
                    // Power the clutch: the input side keeps spinning, the
                    // charger side stops.
                    helper.setBlock(clutchPos.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenWaitUntil(() -> {
                    SUChargerBlockEntity charger = helper.getBlockEntity(chargerPos);
                    KineticBlockEntity fanBe = helper.getBlockEntity(fanPos);
                    helper.assertTrue(charger.isDischarging() && fanBe.getSpeed() != 0,
                            "Behind a powered clutch the charger must drive the fan from its buffer");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    // Still in battery mode after a second: no flip-flopping.
                    SUChargerBlockEntity charger = helper.getBlockEntity(chargerPos);
                    KineticBlockEntity fanBe = helper.getBlockEntity(fanPos);
                    helper.assertTrue(charger.isDischarging() && fanBe.getSpeed() != 0,
                            "The charger must stay in battery mode while the clutch is powered");
                })
                .thenSucceed();
    }





    /**
     * Guards the value box plumbing the player actually clicks, which an
     * earlier version broke without any server-logic test noticing: Create
     * keeps behaviours in a type-keyed map, so the inducer's four scroll
     * boxes must register under distinct types (and distinct packet ids for
     * click routing), and each box must accept a click on the face it sits
     * on. The hit test only tolerates a quarter block, so the recessed
     * gate body needs its box on the actual surface, not at full-cube
     * depth.
     */
    @GameTest(template = "empty")
    public static void valueBoxesAreDistinctAndClickable(GameTestHelper helper) {
        BlockPos inducerPos = new BlockPos(3, 2, 3);
        BlockPos gatePos = new BlockPos(2, 2, 2);
        helper.setBlock(inducerPos, ModBlocks.WEATHER_INDUCER.get().defaultBlockState());
        helper.setBlock(gatePos, ModBlocks.STRESS_GATE.get().defaultBlockState()
                .setValue(StressGateBlock.AXIS, Direction.Axis.X));

        WeatherInducerBlockEntity inducer = helper.getBlockEntity(inducerPos);
        List<ScrollValueBehaviour> boxes = inducer.getAllBehaviours().stream()
                .filter(ScrollValueBehaviour.class::isInstance)
                .map(ScrollValueBehaviour.class::cast)
                .toList();
        helper.assertTrue(boxes.size() == 4,
                "Expected 4 value boxes on the inducer, found " + boxes.size());
        long distinctIds = boxes.stream().map(ValueSettingsBehaviour::netId).distinct().count();
        helper.assertTrue(distinctIds == 4, "Value box packet ids must be distinct");

        // A click on the middle of the top cap must land in the mode selector.
        boolean modeHit = boxes.stream().anyMatch(box ->
                hits(helper, box, inducerPos, Direction.UP, new Vec3(0.5, 1.0, 0.5)));
        helper.assertTrue(modeHit, "No value box catches a click on the inducer top");

        // Same for the gate: its flanged body surface sits at 12 of 16.
        StressGateBlockEntity gate = helper.getBlockEntity(gatePos);
        boolean thresholdHit = gate.getAllBehaviours().stream()
                .filter(ScrollValueBehaviour.class::isInstance)
                .map(ScrollValueBehaviour.class::cast)
                .anyMatch(box ->
                        hits(helper, box, gatePos, Direction.UP, new Vec3(0.5, 0.75, 0.5)));
        helper.assertTrue(thresholdHit, "The gate threshold box must catch a click on its body surface");
        helper.succeed();
    }

    /** Simulates Create's value box hit test for a click on the given face. */
    private static boolean hits(GameTestHelper helper, ScrollValueBehaviour box, BlockPos pos,
                                Direction side, Vec3 localHit) {
        if (box.getSlotPositioning() instanceof ValueBoxTransform.Sided sided) {
            sided.fromSide(side);
        }
        Vec3 world = Vec3.atLowerCornerOf(helper.absolutePos(pos)).add(localHit);
        return box.testHit(world);
    }

    /**
     * A save from before the keyed value boxes holds one shared
     * "ScrollValue" tag, written last by the Z offset box, so the mode
     * selector could read a lightning offset (say 13) as its enum index and
     * crash Create's option renderer on hover. Replays that save shape and
     * walks the same array access the renderer uses.
     */
    @GameTest(template = "empty")
    public static void legacyNbtDoesNotBreakModeSelector(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.WEATHER_INDUCER.get().defaultBlockState());
        WeatherInducerBlockEntity be = helper.getBlockEntity(pos);

        CompoundTag tag = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putInt("ScrollValue", 13);
        tag.remove("ScrollValueMode");
        tag.remove("ScrollValueOffsetX");
        tag.remove("ScrollValueOffsetZ");
        be.loadWithComponents(tag, helper.getLevel().registryAccess());

        for (var behaviour : be.getAllBehaviours()) {
            if (behaviour instanceof ScrollOptionBehaviour<?> options) {
                helper.assertTrue(options.get() != null,
                        "The mode selector must survive a legacy shared ScrollValue tag");
            }
        }
        helper.assertTrue(be.getMode() != null, "Mode lookup must survive legacy NBT");
        helper.succeed();
    }

    /**
     * The Stress Gate on real kinetics: locked while the network provides
     * less than the threshold, open once it provides enough.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void stressGateUnlocksOnProvision(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(2, 2, 3);
        BlockPos gatePos = new BlockPos(3, 2, 3);
        BlockPos fanPos = new BlockPos(4, 2, 3);
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    Block motor = BuiltInRegistries.BLOCK
                            .get(ResourceLocation.parse("create:creative_motor"));
                    Block fan = BuiltInRegistries.BLOCK
                            .get(ResourceLocation.parse("create:encased_fan"));
                    helper.setBlock(motorPos, motor.defaultBlockState()
                            .setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                    helper.setBlock(gatePos, ModBlocks.STRESS_GATE.get().defaultBlockState()
                            .setValue(StressGateBlock.AXIS, Direction.Axis.X));
                    helper.setBlock(fanPos, fan.defaultBlockState()
                            .setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                    StressGateBlockEntity gate = helper.getBlockEntity(gatePos);
                    gate.setThresholdIndexForTesting(SUValueLadder.STEPS.length - 1); // 2^20 SU
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    StressGateBlockEntity gate = helper.getBlockEntity(gatePos);
                    KineticBlockEntity fanBe = helper.getBlockEntity(fanPos);
                    helper.assertTrue(gate.isLocked(),
                            "The gate should stay locked below its threshold");
                    helper.assertTrue(fanBe.getSpeed() == 0,
                            "A locked gate must not pass rotation");
                })
                .thenExecute(() -> {
                    StressGateBlockEntity gate = helper.getBlockEntity(gatePos);
                    gate.setThresholdIndexForTesting(0); // 0 SU, always met
                })
                .thenWaitUntil(() -> {
                    StressGateBlockEntity gate = helper.getBlockEntity(gatePos);
                    KineticBlockEntity fanBe = helper.getBlockEntity(fanPos);
                    helper.assertTrue(!gate.isLocked() && fanBe.getSpeed() != 0,
                            "The gate should unlock and pass rotation once the threshold is met");
                })
                .thenSucceed();
    }

    /** A lightning strike consumes the Lightning Medium and bottles the strike. */
    @GameTest(template = "empty")
    public static void lightningMediumBottlesStrikes(GameTestHelper helper) {
        BlockPos mediumPos = new BlockPos(3, 2, 3);
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    helper.setBlock(mediumPos, ModBlocks.LIGHTNING_MEDIUM.get());
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
                    bolt.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(mediumPos.above())));
                    helper.getLevel().addFreshEntity(bolt);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.AIR, mediumPos);
                    AABB area = new AABB(helper.absolutePos(mediumPos)).inflate(3);
                    boolean found = helper.getLevel()
                            .getEntitiesOfClass(ItemEntity.class, area).stream()
                            .anyMatch(e -> e.getItem().is(ModItems.BOTTLE_O_LIGHTNING.get()));
                    helper.assertTrue(found,
                            "The strike should bottle into a Bottle o' Lightning");
                })
                .thenSucceed();
    }

    /** A thrown Bottle o' Lightning strikes lightning where it lands. */
    @GameTest(template = "empty")
    public static void thrownBottleSummonsLightning(GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    ThrownBottleOLightning bottle = new ThrownBottleOLightning(
                            ModEntityTypes.THROWN_BOTTLE_O_LIGHTNING.get(), helper.getLevel());
                    bottle.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 4, 3))));
                    bottle.setDeltaMovement(0, -1, 0);
                    helper.getLevel().addFreshEntity(bottle);
                })
                .thenWaitUntil(() -> {
                    AABB area = new AABB(helper.absolutePos(INDUCER)).inflate(8);
                    helper.assertTrue(!helper.getLevel()
                                    .getEntitiesOfClass(LightningBolt.class, area).isEmpty(),
                            "A thrown bottle should summon lightning on impact");
                })
                .thenSucceed();
    }

    /** Below full charge, a redstone pulse must not fire. */
    @GameTest(template = "empty")
    public static void notChargedDoesNotFire(GameTestHelper helper) {
        double half = WeatherInducerBlockEntity.MAX_CHARGE / 2.0;
        helper.startSequence()
                .thenExecute(() -> {
                    placeFloor(helper);
                    helper.setBlock(INDUCER, ModBlocks.WEATHER_INDUCER.get());
                    WeatherInducerBlockEntity be = inducer(helper);
                    be.setModeForTesting(WeatherMode.RAIN.ordinal());
                    be.setChargeForTesting(half);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK))
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(inducer(helper).getCharge() == half,
                        "Inducer below full charge must not fire"))
                .thenSucceed();
    }
}
