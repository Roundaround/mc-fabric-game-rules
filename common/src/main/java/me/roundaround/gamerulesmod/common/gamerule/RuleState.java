package me.roundaround.gamerulesmod.common.gamerule;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

public enum RuleState {
  MUTABLE(0), LOCKED(1), DENIED(2);

  public static final IntFunction<RuleState> ID_TO_VALUE_FUNCTION = ByIdMap.continuous(
      RuleState::getIndex,
      values(),
      ByIdMap.OutOfBoundsStrategy.WRAP
  );
  public static final StreamCodec<ByteBuf, RuleState> PACKET_CODEC = ByteBufCodecs.idMapper(
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
