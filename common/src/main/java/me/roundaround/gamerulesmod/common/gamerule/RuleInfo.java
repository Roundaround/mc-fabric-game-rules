package me.roundaround.gamerulesmod.common.gamerule;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import me.roundaround.trove.network.TrovePacketCodecs;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Date;

public record RuleInfo(String id, Either<Boolean, Integer> value, RuleState state, Date changed) {
  public static final StreamCodec<ByteBuf, RuleInfo> PACKET_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      RuleInfo::id,
      ByteBufCodecs.either(ByteBufCodecs.BOOL, ByteBufCodecs.INT),
      RuleInfo::value,
      RuleState.PACKET_CODEC,
      RuleInfo::state,
      TrovePacketCodecs.nullable(TrovePacketCodecs.DATE),
      RuleInfo::changed,
      RuleInfo::new
  );
}
