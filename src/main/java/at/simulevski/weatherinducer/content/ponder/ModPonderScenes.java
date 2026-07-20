package at.simulevski.weatherinducer.content.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Ponder storyboards. Both scenes reveal the shared schematic
 * {@code assets/weatherinducer/ponder/weather_devices.nbt}: a creative motor
 * driving a Weather Inducer.
 */
public final class ModPonderScenes {

    private ModPonderScenes() {
    }

    public static void weatherInducer(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("weather_inducer", "Controlling the weather with the Weather Inducer");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        BlockPos inducer = util.grid().at(4, 1, 2);

        scene.overlay().showText(80)
                .text("The Weather Inducer charges from the Stress Units on its shaft")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(inducer, Direction.WEST));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("It must be able to see the sky")
                .placeNearTarget()
                .pointAt(util.vector().topOf(inducer));
        scene.idle(90);

        scene.overlay().showText(90)
                .text("Once charged to 1,048,576 SU, a redstone pulse applies the selected weather: rain, clear, or a lightning strike")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(inducer, Direction.UP));
        scene.idle(100);

        scene.markAsFinished();
    }

    public static void kineticCharger(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("kinetic_charger", "Storing rotation with the Kinetic Charger");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        BlockPos charger = util.grid().at(2, 1, 2);

        scene.overlay().showText(90)
                .text("The Kinetic Charger is a battery: while its input shaft turns, it banks the network's power into a buffer")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(charger));
        scene.idle(100);

        scene.overlay().showText(100)
                .text("Flywheels attached on the input side set the capacity: 2,048 SU bare, about 104,858 more per wheel, up to ten. Any more jams the charger")
                .placeNearTarget()
                .pointAt(util.vector().topOf(charger));
        scene.idle(110);

        scene.overlay().showText(90)
                .text("Filling loads the network like a machine: the capacity divided by the charge time on its side value box, ten seconds at the fastest")
                .placeNearTarget()
                .pointAt(util.vector().topOf(charger));
        scene.idle(100);

        scene.overlay().showText(90)
                .text("Stop the input, and the charger takes over: it spins its output side at the speed it charged with until the buffer runs dry")
                .placeNearTarget()
                .pointAt(util.vector().topOf(charger));
        scene.idle(100);

        scene.markAsFinished();
    }

}
