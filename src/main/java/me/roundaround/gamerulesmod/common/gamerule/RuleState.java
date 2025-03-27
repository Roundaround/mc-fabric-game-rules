package me.roundaround.gamerulesmod.common.gamerule;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

public enum RuleState {
  MUTABLE(0), IMMUTABLE(1), LOCKED(2), DENIED(3);

  public static final IntFunction<RuleState> ID_TO_VALUE_FUNCTION = ValueLists.createIndexToValueFunction(
      RuleState::getIndex,
      values(),
      ValueLists.OutOfBoundsHandling.WRAP
  );
  public static final PacketCodec<ByteBuf, RuleState> PACKET_CODEC = PacketCodecs.indexed(
      ID_TO_VALUE_FUNCTION,
      RuleState::getIndex
  );

  private final int index;

  RuleState(int index) {
    this.index = index;
  }

  public int getIndex() {
    return this.index;
  }
}
