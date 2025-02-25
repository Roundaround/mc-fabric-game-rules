package me.roundaround.gamerulesmod.server.network;

import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gamerulesmod.util.RuleInfo;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ServerNetworking {
  private ServerNetworking() {
  }

  public static void registerReceivers() {
    ServerPlayNetworking.registerGlobalReceiver(Networking.FetchC2S.ID, ServerNetworking::handleFetch);
    ServerPlayNetworking.registerGlobalReceiver(Networking.SetC2S.ID, ServerNetworking::handleSet);
  }

  public static void sendFetch(ServerPlayerEntity player, int reqId, List<RuleInfo> rules) {
    ServerPlayNetworking.send(player, new Networking.FetchS2C(reqId, rules));
  }

  private static void handleFetch(Networking.FetchC2S payload, ServerPlayNetworking.Context context) {
    final ServerPlayerEntity player = context.player();
    final MinecraftServer server = player.server;
    server.execute(() -> {
      if (!server.isSingleplayer() && !player.hasPermissionLevel(2)) {
        return;
      }

      ServerWorld world = player.getServerWorld();
      if (world == null) {
        return;
      }

      sendFetch(player, payload.reqId(), RuleInfo.collect(world.getGameRules(), player, payload.includeImmutable()));
    });
  }

  private static void handleSet(Networking.SetC2S payload, ServerPlayNetworking.Context context) {
    final ServerPlayerEntity player = context.player();
    final MinecraftServer server = player.server;
    server.execute(() -> {
      if (!server.isSingleplayer() && !player.hasPermissionLevel(2)) {
        return;
      }

      ServerWorld world = player.getServerWorld();
      if (world == null) {
        return;
      }

      final GameRules gameRules = world.getGameRules();
      final Set<String> mutableRules = RuleInfo.collect(gameRules, player, false)
          .stream()
          .map(RuleInfo::id)
          .collect(Collectors.toSet());

      payload.values().forEach((id, either) -> {
        if (mutableRules.contains(id)) {
          gameRules.gamerulesmod$set(id, either);
        }
      });
    });
  }
}
