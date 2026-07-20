package at.simulevski.weatherinducer.content.inducer;

import at.simulevski.weatherinducer.content.util.KeyedScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * The Weather Inducer's charge time slider: instead of an SU-per-tick
 * number, the player picks how many seconds a full charge takes. The
 * ladder is ten seconds times a power of two, and ten seconds is the
 * floor: even flat out, the inducer fires at most once every ten seconds.
 * The box stores the ladder index; every label the player sees shows the
 * seconds it stands for.
 */
public class ChargeTimeScrollBehaviour extends KeyedScrollValueBehaviour {

    /** Full-charge durations in seconds: 10 times a power of two. */
    public static final int[] SECONDS = {10, 20, 40, 80, 160, 320, 640, 1280};

    public ChargeTimeScrollBehaviour(SmartBlockEntity be, ValueBoxTransform slot) {
        super("ChargeTime", 4,
                Component.translatable("weatherinducer.value.charge_time"), be, slot);
        between(0, SECONDS.length - 1);
        withFormatter(index -> seconds(index) + " s");
    }

    public static int seconds(int index) {
        return SECONDS[Math.max(0, Math.min(index, SECONDS.length - 1))];
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, max, 1,
                List.of(Component.translatable("weatherinducer.value.charge_time_row")),
                new ValueSettingsFormatter(settings ->
                        Component.literal(seconds(settings.value()) + " s")));
    }
}
