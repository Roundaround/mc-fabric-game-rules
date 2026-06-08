package me.roundaround.gamerulesmod;

import me.roundaround.allay.api.Entrypoint;
import net.fabricmc.api.ModInitializer;

@Entrypoint(Entrypoint.MAIN)
public final class GameRulesMod implements ModInitializer {
  @Override
  public void onInitialize() {
    GameRules.init();
  }
}
