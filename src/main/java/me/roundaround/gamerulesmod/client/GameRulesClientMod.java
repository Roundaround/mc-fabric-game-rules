package me.roundaround.gamerulesmod.client;

import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class GameRulesClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientNetworking.registerReceivers();
  }
}
