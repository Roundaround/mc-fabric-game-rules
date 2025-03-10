package me.roundaround.gamerulesmod;

import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gamerulesmod.server.network.ServerNetworking;
import me.roundaround.roundalib.gradle.api.annotation.Entrypoint;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Entrypoint(Entrypoint.MAIN)
public final class GameRulesMod implements ModInitializer {
  public static final String MOD_ID = "gamerulesmod";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    Networking.registerC2SPayloads();
    Networking.registerS2CPayloads();
    ServerNetworking.registerReceivers();
  }
}
