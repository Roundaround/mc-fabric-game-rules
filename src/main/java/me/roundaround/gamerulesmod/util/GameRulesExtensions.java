package me.roundaround.gamerulesmod.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.MinecraftServer;

public interface GameRulesExtensions {
  default Either<Boolean, Integer> gamerulesmod$getValue(String id) {
    return Either.left(false);
  }

  default void gamerulesmod$set(String id, boolean value) {
    this.gamerulesmod$set(id, value, null);
  }

  default void gamerulesmod$set(String id, boolean value, MinecraftServer server) {
  }

  default void gamerulesmod$set(String id, int value) {
    this.gamerulesmod$set(id, value, null);
  }

  default void gamerulesmod$set(String id, int value, MinecraftServer server) {
  }

  default void gamerulesmod$set(String id, Either<Boolean, Integer> value) {
    value.ifLeft((booleanValue) -> this.gamerulesmod$set(id, booleanValue))
        .ifRight((intValue) -> this.gamerulesmod$set(id, intValue));
  }
}
