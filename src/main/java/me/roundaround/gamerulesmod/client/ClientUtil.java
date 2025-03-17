package me.roundaround.gamerulesmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.world.GameRules;

public final class ClientUtil {
  public static GameRules getDefaultRules() {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.world == null) {
      return new GameRules(FeatureFlags.FEATURE_MANAGER.getFeatureSet());
    }
    return getDefaultRules(client.world);
  }

  public static GameRules getDefaultRules(ClientWorld world) {
    return new GameRules(world.getEnabledFeatures());
  }

  private ClientUtil() {
  }
}
