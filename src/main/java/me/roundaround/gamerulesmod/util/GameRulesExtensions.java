package me.roundaround.gamerulesmod.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

public interface GameRulesExtensions {
  default int gamerulesmod$size() {
    return 0;
  }

  default GameRules.Rule<?> gamerulesmod$get(String id) {
    return null;
  }

  default Either<Boolean, Integer> gamerulesmod$getValue(String id) {
    return null;
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
