package at.simulevski.weatherinducer.data;

import at.simulevski.weatherinducer.WeatherInducerMod;
import at.simulevski.weatherinducer.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, WeatherInducerMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Item models simply inherit the block model; the inducer and charger
        // use their empty (level 0) variant.
        withExistingParent("weather_inducer", modLoc("block/weather_inducer_0"));
        withExistingParent("weather_sensor", modLoc("block/weather_sensor"));
        withExistingParent("kinetic_charger", modLoc("block/kinetic_charger_0"));
        withExistingParent("charger_link", modLoc("block/charger_link"));
        withExistingParent("stress_gate", modLoc("block/stress_gate_locked"));
        withExistingParent("lightning_medium", modLoc("block/lightning_medium"));

        // Lightning gear: flat sprites; tools use the handheld transform.
        basicItem(ModItems.BOTTLE_O_LIGHTNING.get());
        basicItem(ModItems.LIGHTNING_BOLT.get());
        basicItem(ModItems.LIGHTNING_HELMET.get());
        basicItem(ModItems.LIGHTNING_CHESTPLATE.get());
        basicItem(ModItems.LIGHTNING_LEGGINGS.get());
        basicItem(ModItems.LIGHTNING_BOOTS.get());
        registerLightningSword();
    }

    /**
     * The sword is real 3D in hand: modelled blade, guard, grip and pommel,
     * with electric arcs crawling up the blade via a four frame animated
     * texture. In the inventory, on the ground and in item frames it stays
     * the familiar flat sprite, courtesy of NeoForge's separate transforms
     * loader.
     */
    private void registerLightningSword() {
        ItemModelBuilder flat = getBuilder("lightning_sword_flat")
                .parent(getExistingFile(mcLoc("item/handheld")))
                .texture("layer0", modLoc("item/lightning_sword"));

        // No item/handheld parent here: that chain bottoms out in
        // builtin/generated, which insists on baking sprite layers and has
        // no layer0 to bake, so the whole model falls over and nothing
        // renders. The 3D base is self-contained and carries its own hand
        // transforms instead (GUI and frames use the flat sprite anyway).
        ItemModelBuilder blade = getBuilder("lightning_sword_3d")
                .texture("blade", modLoc("item/lightning_sword_blade"))
                .texture("particle", modLoc("item/lightning_sword_blade"))
                .guiLight(net.minecraft.client.renderer.block.model.BlockModel.GuiLight.FRONT);
        // Vanilla's own handheld pose numbers. They assume the sword runs
        // diagonally through the model like every vanilla sword sprite,
        // which is why each element below carries the shared -45 degree
        // roll: authored upright for sane coordinates, baked diagonal.
        blade.transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(0, -90, 55).translation(0, 4, 0.5f).scale(0.85f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(0, 90, -55).translation(0, 4, 0.5f).scale(0.85f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                // Lower, more central and a touch smaller than vanilla's
                // pose, so the long blade sweeps across the view instead
                // of vanishing off the top right corner.
                .rotation(0, -90, 25).translation(0.4f, 1.2f, 0.8f).scale(0.58f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(0, 90, -25).translation(0.4f, 1.2f, 0.8f).scale(0.58f).end()
                .end();
        // Katana anatomy, hilt to tip: kashira cap, long two-hand tsuka
        // with the diamond wrap, small oval tsuba, then a slender
        // single-edged blade whose segments step sideways toward the
        // spine for the curve and close in an angled kissaki.
        blade.element() // kashira, the small end cap
                .from(7.3f, -4.6f, 7.3f).to(8.7f, -4, 8.7f)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(4, 12, 5, 13).end()
                .face(Direction.SOUTH).texture("#blade").uvs(4, 12, 5, 13).end()
                .face(Direction.EAST).texture("#blade").uvs(4, 12, 5, 13).end()
                .face(Direction.WEST).texture("#blade").uvs(4, 12, 5, 13).end()
                .face(Direction.DOWN).texture("#blade").uvs(4, 12, 5, 13).end()
                .end();
        blade.element() // tsuka, the wrapped grip
                .from(7.4f, -4, 7.4f).to(8.6f, 4, 8.6f)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(12, 12, 14, 16).end()
                .face(Direction.SOUTH).texture("#blade").uvs(12, 12, 14, 16).end()
                .face(Direction.EAST).texture("#blade").uvs(12, 12, 14, 16).end()
                .face(Direction.WEST).texture("#blade").uvs(12, 12, 14, 16).end()
                .end();
        blade.element() // tsuba, the small oval guard
                .from(6.9f, 4, 7.1f).to(9.1f, 4.75f, 8.9f)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(4, 12, 10, 14).end()
                .face(Direction.SOUTH).texture("#blade").uvs(4, 12, 10, 14).end()
                .face(Direction.EAST).texture("#blade").uvs(4, 12, 6, 14).end()
                .face(Direction.WEST).texture("#blade").uvs(4, 12, 6, 14).end()
                .face(Direction.UP).texture("#blade").uvs(4, 12, 10, 14).end()
                .face(Direction.DOWN).texture("#blade").uvs(4, 12, 10, 14).end()
                .end();
        // The blade: five segments, each stepping a third of a pixel
        // toward the spine, sweep the edge in a long arc, and the kissaki
        // narrows onto the spine line the way a real tip does. The spine
        // rides the +x side, which in the baked diagonal puts the cutting
        // edge forward, the way the sword is actually swung.
        bladeSegment(blade, 7.3f, 8.7f, 4.75f, 9.5f, 0.5f, 0, 3.2f);
        bladeSegment(blade, 7.6f, 9, 9.5f, 13.5f, 0.48f, 3.2f, 5.9f);
        bladeSegment(blade, 7.9f, 9.3f, 13.5f, 17, 0.46f, 5.9f, 8.3f);
        bladeSegment(blade, 8.2f, 9.6f, 17, 20, 0.44f, 8.3f, 10.3f);
        bladeSegment(blade, 8.5f, 9.9f, 20, 22.5f, 0.42f, 10.3f, 12);
        bladeSegment(blade, 8.85f, 9.9f, 22.5f, 23.8f, 0.38f, 12, 12.9f);
        bladeSegment(blade, 9.35f, 9.9f, 23.8f, 24.6f, 0.34f, 12.9f, 13.4f);

        getBuilder("lightning_sword")
                .customLoader(SeparateTransformsModelBuilder::begin)
                .base(blade)
                .perspective(ItemDisplayContext.GUI, flat)
                .perspective(ItemDisplayContext.FIXED, flat)
                .perspective(ItemDisplayContext.GROUND, flat)
                .end();
    }

    /**
     * One segment of the katana blade running {@code y1..y2} between
     * {@code x1..x2}, {@code thick} deep, flats sampling texture rows
     * {@code v1..v2} of the animated strip (body and bright edge
     * columns), the west face the dark spine column, the east face the
     * edge glow. Carries the same shared -45 degree roll as the rest of
     * the sword, and caps its up face so each step's exposed rim stays
     * closed.
     */
    private void bladeSegment(ItemModelBuilder blade, float x1, float x2,
                              float y1, float y2, float thick, float v1, float v2) {
        float z1 = 8 - thick / 2;
        float z2 = 8 + thick / 2;
        blade.element()
                .from(x1, y1, z1).to(x2, y2, z2)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(0.5f, v1, 2, v2).end()
                .face(Direction.SOUTH).texture("#blade").uvs(0.5f, v1, 2, v2).end()
                .face(Direction.EAST).texture("#blade").uvs(2, v1, 3, v2).end()
                .face(Direction.WEST).texture("#blade").uvs(3, v1, 4, v2).end()
                .face(Direction.UP).texture("#blade").uvs(0.5f, v1, 2, v1 + 0.4f).end()
                .end();
    }
}
