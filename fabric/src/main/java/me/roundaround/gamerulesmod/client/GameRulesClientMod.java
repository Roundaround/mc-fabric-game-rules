package me.roundaround.gamerulesmod.client;

import me.roundaround.allay.api.Entrypoint;
import net.fabricmc.api.ClientModInitializer;

@Entrypoint(Entrypoint.CLIENT)
public final class GameRulesClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    GameRulesClient.initClient();
  }
}
