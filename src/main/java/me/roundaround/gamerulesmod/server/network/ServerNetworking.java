package me.roundaround.gamerulesmod.server.network;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.network.Networking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;

import java.util.HashMap;
import java.util.Map;

public final class ServerNetworking {
  private ServerNetworking() {
  }

  public static void registerReceivers() {
    ServerPlayNetworking.registerGlobalReceiver(Networking.FetchC2S.ID, ServerNetworking::handleFetch);
    ServerPlayNetworking.registerGlobalReceiver(Networking.SetC2S.ID, ServerNetworking::handleSet);
  }

  public static void sendFetch(ServerPlayerEntity player, int reqId, Map<String, Either<Boolean, Integer>> values) {
    ServerPlayNetworking.send(player, new Networking.FetchS2C(reqId, values));
  }

  private static void handleFetch(Networking.FetchC2S payload, ServerPlayNetworking.Context context) {
    ServerPlayerEntity player = context.player();
    MinecraftServer server = player.server;
    server.execute(() -> {
      if (!server.isSingleplayer() && !player.hasPermissionLevel(2)) {
        return;
      }

      ServerWorld world = player.getServerWorld();
      if (world == null) {
        return;
      }

      GameRules gameRules = world.getGameRules();
      HashMap<String, Either<Boolean, Integer>> values = new HashMap<>();
      for (String id : payload.ids()) {
        values.put(id, gameRules.gamerulesmod$getValue(id));
      }

      sendFetch(player, payload.reqId(), values);
    });
  }

  private static void handleSet(Networking.SetC2S payload, ServerPlayNetworking.Context context) {
    ServerPlayerEntity player = context.player();
    MinecraftServer server = player.server;
    server.execute(() -> {
      if (!server.isSingleplayer() && !player.hasPermissionLevel(2)) {
        return;
      }

      ServerWorld world = player.getServerWorld();
      if (world == null) {
        return;
      }

      GameRules gameRules = world.getGameRules();
      // TODO: Some kind of "are cheats enabled?" check for _some_ rules
      payload.values().forEach(gameRules::gamerulesmod$set);
    });
  }
}
