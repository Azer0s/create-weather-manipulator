package at.simulevski.weatherinducer.registry;

import at.simulevski.weatherinducer.content.charger.ChargerLinkBlockEntity;
import at.simulevski.weatherinducer.content.charger.ChargerLinkNamePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPackets {

    private ModPackets() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModPackets::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ChargerLinkNamePayload.TYPE, ChargerLinkNamePayload.STREAM_CODEC,
                (payload, context) -> {
                    Player player = context.player();
                    Level level = player.level();
                    BlockPos pos = payload.pos();
                    if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5,
                            pos.getZ() + 0.5) > 64
                            || !level.isLoaded(pos)
                            || !(level.getBlockEntity(pos) instanceof ChargerLinkBlockEntity link)) {
                        return;
                    }
                    String name = payload.name().trim();
                    link.setCustomName(name.isEmpty() ? null : name);
                });
    }
}
