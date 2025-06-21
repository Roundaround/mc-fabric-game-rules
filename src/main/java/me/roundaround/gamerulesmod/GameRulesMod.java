package me.roundaround.gamerulesmod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.roundaround.gamerulesmod.generated.Constants;
import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gamerulesmod.server.network.ServerNetworking;
import me.roundaround.gradle.api.annotation.Entrypoint;
import net.fabricmc.api.ModInitializer;

@Entrypoint(Entrypoint.MAIN)
public final class GameRulesMod implements ModInitializer {
  public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);

  @Override
  public void onInitialize() {
    Networking.registerC2SPayloads();
    Networking.registerS2CPayloads();
    ServerNetworking.registerReceivers();
  }
}
