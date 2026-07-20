package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.WeatherInducerMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the name typed into the Charger Link's naming
 * screen. An empty name clears the custom one, falling back to the
 * link's automatic number.
 */
public record ChargerLinkNamePayload(BlockPos pos, String name) implements CustomPacketPayload {

    public static final Type<ChargerLinkNamePayload> TYPE =
            new Type<>(WeatherInducerMod.asResource("charger_link_name"));

    public static final StreamCodec<ByteBuf, ChargerLinkNamePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChargerLinkNamePayload::pos,
                    ByteBufCodecs.stringUtf8(48), ChargerLinkNamePayload::name,
                    ChargerLinkNamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
