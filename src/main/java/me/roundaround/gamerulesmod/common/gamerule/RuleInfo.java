package me.roundaround.gamerulesmod.common.gamerule;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import me.roundaround.gamerulesmod.roundalib.network.CustomCodecs;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.Date;

public record RuleInfo(String id, Either<Boolean, Integer> value, RuleState state, Date changed) {
  public static final PacketCodec<ByteBuf, RuleInfo> PACKET_CODEC = PacketCodec.tuple(
      PacketCodecs.STRING,
      RuleInfo::id,
      PacketCodecs.either(PacketCodecs.BOOLEAN, PacketCodecs.INTEGER),
      RuleInfo::value,
      RuleState.PACKET_CODEC,
      RuleInfo::state,
      CustomCodecs.nullable(CustomCodecs.DATE),
      RuleInfo::changed,
      RuleInfo::new
  );
}
