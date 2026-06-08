package me.roundaround.gamerulesmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;

public final class ClientUtil {
  public static GameRules getDefaultRules() {
    Minecraft client = Minecraft.getInstance();
    if (client == null || client.level == null) {
      return new GameRules(FeatureFlags.DEFAULT_FLAGS);
    }
    return getDefaultRules(client.level);
  }

  public static GameRules getDefaultRules(ClientLevel world) {
    return new GameRules(world.enabledFeatures());
  }

  private ClientUtil() {
  }
}
