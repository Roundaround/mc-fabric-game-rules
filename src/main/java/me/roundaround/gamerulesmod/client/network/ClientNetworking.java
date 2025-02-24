package me.roundaround.gamerulesmod.client.network;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.network.Networking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.GameRules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ClientNetworking {
  private static final HashMap<Integer, CompletableFuture<GameRules>> PENDING = new HashMap<>();

  private ClientNetworking() {
  }

  public static void registerReceivers() {
    ClientPlayNetworking.registerGlobalReceiver(Networking.FetchS2C.ID, ClientNetworking::handleFetch);

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PENDING.clear());
  }

  public static void sendSet(Map<String, Either<Boolean, Integer>> values) {
    ClientPlayNetworking.send(new Networking.SetC2S(values));
  }

  public static CompletableFuture<GameRules> sendFetch(List<String> ids) {
    try {
      int reqId = getUniqueReqId();
      CompletableFuture<GameRules> future = new CompletableFuture<>();
      PENDING.put(reqId, future);
      ClientPlayNetworking.send(new Networking.FetchC2S(reqId, ids));
      return future;
    } catch (IllegalStateException e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  private static int getUniqueReqId() {
    for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE; i++) {
      if (!PENDING.containsKey(i)) {
        return i;
      }
    }
    throw new IllegalStateException("Failed to find an available request ID.");
  }

  private static void handleFetch(Networking.FetchS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      CompletableFuture<GameRules> future = PENDING.remove(payload.reqId());
      if (future == null) {
        return;
      }

      GameRules gameRules = new GameRules();
      payload.values().forEach(gameRules::gamerulesmod$set);
      future.complete(gameRules);
    });
  }
}
