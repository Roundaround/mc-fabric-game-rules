package me.roundaround.gamerulesmod.client;

import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.roundalib.gradle.api.annotation.Entrypoint;
import net.fabricmc.api.ClientModInitializer;

@Entrypoint(Entrypoint.CLIENT)
public class GameRulesClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientNetworking.registerReceivers();
  }
}
