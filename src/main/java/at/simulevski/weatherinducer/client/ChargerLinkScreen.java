package at.simulevski.weatherinducer.client;

import at.simulevski.weatherinducer.content.charger.ChargerLinkNamePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The little naming screen a Charger Link opens on right click: one text
 * box, prefilled with the current name. Confirming sends the name to the
 * server; clearing the box hands the link back its automatic number.
 */
public class ChargerLinkScreen extends Screen {

    private final BlockPos pos;
    private final String initial;
    private EditBox nameBox;

    public ChargerLinkScreen(BlockPos pos, String initial) {
        super(Component.translatable("weatherinducer.screen.charger_link"));
        this.pos = pos;
        this.initial = initial;
    }

    @Override
    protected void init() {
        nameBox = new EditBox(font, width / 2 - 100, height / 2 - 12, 200, 20,
                Component.translatable("weatherinducer.screen.charger_link.name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(initial);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> confirm())
                .bounds(width / 2 - 102, height / 2 + 16, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(width / 2 + 2, height / 2 + 16, 100, 20).build());
        setInitialFocus(nameBox);
    }

    private void confirm() {
        PacketDistributor.sendToServer(new ChargerLinkNamePayload(pos, nameBox.getValue()));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter confirms, like every other naming box.
        if (keyCode == 257 || keyCode == 335) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 34, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
