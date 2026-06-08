package me.roundaround.gamerulesmod.client;

import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.gamerulesmod.client.option.KeyBindings;

public final class GameRulesClient {
  private GameRulesClient() {
  }

  public static void initClient() {
    ClientNetworking.registerReceivers();
    KeyBindings.register();
  }
}
