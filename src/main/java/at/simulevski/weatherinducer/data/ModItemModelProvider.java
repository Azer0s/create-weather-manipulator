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
        for (String tool : new String[]{"pickaxe", "axe", "shovel", "hoe"}) {
            withExistingParent("lightning_" + tool, mcLoc("item/handheld"))
                    .texture("layer0", modLoc("item/lightning_" + tool));
        }
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
                .rotation(0, -90, 25).translation(1.13f, 3.2f, 1.13f).scale(0.68f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(0, 90, -25).translation(1.13f, 3.2f, 1.13f).scale(0.68f).end()
                .end();
        // Pommel, slightly wider than the grip, capped top and bottom.
        blade.element()
                .from(6.5f, -2, 7).to(9.5f, -0.5f, 9)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(4, 12, 7, 14).end()
                .face(Direction.SOUTH).texture("#blade").uvs(4, 12, 7, 14).end()
                .face(Direction.EAST).texture("#blade").uvs(4, 12, 6, 14).end()
                .face(Direction.WEST).texture("#blade").uvs(4, 12, 6, 14).end()
                .face(Direction.UP).texture("#blade").uvs(4, 12, 7, 14).end()
                .face(Direction.DOWN).texture("#blade").uvs(4, 12, 7, 14).end()
                .end();
        // Grip, long enough for the whole fist.
        blade.element()
                .from(7.25f, -0.5f, 7.4f).to(8.75f, 4, 8.6f)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(12, 12, 14, 16).end()
                .face(Direction.SOUTH).texture("#blade").uvs(12, 12, 14, 16).end()
                .face(Direction.EAST).texture("#blade").uvs(12, 12, 14, 16).end()
                .face(Direction.WEST).texture("#blade").uvs(12, 12, 14, 16).end()
                .end();
        // Cross guard.
        blade.element()
                .from(5, 4, 6.8f).to(11, 5.5f, 9.2f)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(4, 12, 10, 14).end()
                .face(Direction.SOUTH).texture("#blade").uvs(4, 12, 10, 14).end()
                .face(Direction.EAST).texture("#blade").uvs(4, 12, 6, 14).end()
                .face(Direction.WEST).texture("#blade").uvs(4, 12, 6, 14).end()
                .face(Direction.UP).texture("#blade").uvs(4, 12, 10, 14).end()
                .face(Direction.DOWN).texture("#blade").uvs(4, 12, 10, 14).end()
                .end();
        // The blade, wearing the animated arc texture, shoulder capped so
        // there is no hollow to look into from above.
        blade.element()
                .from(7, 5.5f, 7.6f).to(9, 15.5f, 8.4f)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(0, 0, 2, 10).end()
                .face(Direction.SOUTH).texture("#blade").uvs(0, 0, 2, 10).end()
                .face(Direction.EAST).texture("#blade").uvs(2, 0, 3, 10).end()
                .face(Direction.WEST).texture("#blade").uvs(2, 0, 3, 10).end()
                .face(Direction.UP).texture("#blade").uvs(0, 0, 2, 1).end()
                .end();
        // Tapered tip.
        blade.element()
                .from(7.5f, 15.5f, 7.7f).to(8.5f, 17.5f, 8.3f)
                .rotation().angle(-45).axis(Direction.Axis.Z).origin(8, 8, 8).end()
                .face(Direction.NORTH).texture("#blade").uvs(0, 9, 1, 11).end()
                .face(Direction.SOUTH).texture("#blade").uvs(0, 9, 1, 11).end()
                .face(Direction.EAST).texture("#blade").uvs(2, 9, 3, 11).end()
                .face(Direction.WEST).texture("#blade").uvs(2, 9, 3, 11).end()
                .face(Direction.UP).texture("#blade").uvs(0, 9, 1, 10).end()
                .end();

        getBuilder("lightning_sword")
                .customLoader(SeparateTransformsModelBuilder::begin)
                .base(blade)
                .perspective(ItemDisplayContext.GUI, flat)
                .perspective(ItemDisplayContext.FIXED, flat)
                .perspective(ItemDisplayContext.GROUND, flat)
                .end();
    }
}
