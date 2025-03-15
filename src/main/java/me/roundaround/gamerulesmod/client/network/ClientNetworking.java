package me.roundaround.gamerulesmod.client.network;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gamerulesmod.common.future.CancellableFuture;
import me.roundaround.gamerulesmod.common.gamerule.RuleInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ClientNetworking {
  private static final HashMap<Integer, CompletableFuture<List<RuleInfo>>> PENDING = new HashMap<>();

  private ClientNetworking() {
  }

  public static void registerReceivers() {
    ClientPlayNetworking.registerGlobalReceiver(Networking.FetchS2C.ID, ClientNetworking::handleFetch);

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PENDING.clear());
  }

  public static void sendSet(Map<String, Either<Boolean, Integer>> values) {
    ClientPlayNetworking.send(new Networking.SetC2S(values));
  }

  public static CompletableFuture<List<RuleInfo>> sendFetch(boolean mutableOnly) {
    try {
      int reqId = getUniqueReqId();
      CompletableFuture<List<RuleInfo>> future = new CancellableFuture<>(() -> {
        PENDING.remove(reqId);
        return true;
      });
      PENDING.put(reqId, future);
      ClientPlayNetworking.send(new Networking.FetchC2S(reqId, mutableOnly));
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
      CompletableFuture<List<RuleInfo>> future = PENDING.remove(payload.reqId());
      if (future == null || future.isCancelled()) {
        return;
      }

      future.complete(payload.rules());
    });
  }
}
