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

}
