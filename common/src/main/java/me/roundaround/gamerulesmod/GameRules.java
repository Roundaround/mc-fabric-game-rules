package me.roundaround.gamerulesmod;

import com.mojang.logging.LogUtils;
import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gamerulesmod.server.network.ServerNetworking;
import org.slf4j.Logger;

public final class GameRules {
  public static final Logger LOGGER = LogUtils.getLogger();

  private GameRules() {
  }

  public static void init() {
    Networking.register();
    ServerNetworking.init();
  }
}
