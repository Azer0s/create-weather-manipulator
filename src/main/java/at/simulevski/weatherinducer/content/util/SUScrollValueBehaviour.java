package at.simulevski.weatherinducer.content.util;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * A scroll behaviour over the {@link SUValueLadder}: it stores a ladder
 * index but presents the SU value that index stands for. Without the
 * {@link #createBoard} override the value settings screen would label the
 * cursor with the raw index (0 to 15), which is what the box also syncs
 * over the network; the formatter here turns it back into 64 ... 1,048,576
 * SU wherever the player sees it.
 */
public class SUScrollValueBehaviour extends ScrollValueBehaviour {

    public SUScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        between(0, SUValueLadder.STEPS.length - 1);
        withFormatter(SUValueLadder::format);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, max, 4,
                List.of(Component.translatable("weatherinducer.value.su_row")),
                new ValueSettingsFormatter(settings ->
                        Component.literal(SUValueLadder.format(settings.value()) + " SU")));
    }
}
