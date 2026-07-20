package at.simulevski.weatherinducer.gametest;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.content.charger.SUChargerBlock;
import at.simulevski.weatherinducer.content.charger.SUChargerBlockEntity;
import at.simulevski.weatherinducer.content.gate.StressGateBlock;
import at.simulevski.weatherinducer.content.gate.StressGateBlockEntity;
import at.simulevski.weatherinducer.content.util.SUValueLadder;
import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlockEntity;
import at.simulevski.weatherinducer.content.inducer.WeatherMode;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlock;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlockEntity;
import at.simulevski.weatherinducer.content.sensor.WeatherSensorBlock;
import at.simulevski.weatherinducer.registry.ModBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
                })
                .thenIdle(2) // let a tick publish the redstone level
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
                .thenIdle(2)
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
     * The breaker on real Create kinetics: a creative motor drives an encased
     * fan through a resistor whose cap is 0 SU, so any demand at all must
     * trip it and cut the fan off, while the resistor itself keeps spinning
     * on the source side.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void resistorTripsOnOverload(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(2, 2, 3);
        BlockPos resistorPos = new BlockPos(3, 2, 3);
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
                    helper.setBlock(resistorPos, ModBlocks.SU_RESISTOR.get().defaultBlockState()
                            .setValue(SUResistorBlock.AXIS, Direction.Axis.X));
                    helper.setBlock(fanPos, fan.defaultBlockState()
                            .setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                    SUResistorBlockEntity resistor = helper.getBlockEntity(resistorPos);
                    resistor.setLimitIndexForTesting(0); // cap: 0 SU
                })
                .thenWaitUntil(() -> {
                    SUResistorBlockEntity resistor = helper.getBlockEntity(resistorPos);
                    helper.assertTrue(resistor.isTripped(),
                            "The resistor should trip when downstream draw exceeds the cap");
                })
                .thenWaitUntil(() -> {
                    KineticBlockEntity fanBe = helper.getBlockEntity(fanPos);
                    SUResistorBlockEntity resistor = helper.getBlockEntity(resistorPos);
                    helper.assertTrue(resistor.isTripped() && fanBe.getSpeed() == 0,
                            "Expected a tripped resistor with the fan cut off");
                })
                .thenSucceed();
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
