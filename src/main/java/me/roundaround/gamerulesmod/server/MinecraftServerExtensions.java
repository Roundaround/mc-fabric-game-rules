package me.roundaround.gamerulesmod.server;

import me.roundaround.gamerulesmod.server.gamerule.GameRulesStorage;

public interface MinecraftServerExtensions {
  default GameRulesStorage gamerulesmod$getGameRulesHistory() {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }
}
