package me.roundaround.gamerulesmod.client.network;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gamerulesmod.roundalib.network.request.RequestTracker;
import me.roundaround.gamerulesmod.roundalib.network.request.ServerRequest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Map;

public final class ClientNetworking {
  private static final RequestTracker REQUESTS = new RequestTracker();

  private ClientNetworking() {
  }

  public static void registerReceivers() {
    ClientPlayNetworking.registerGlobalReceiver(Networking.FetchS2C.ID, ClientNetworking::handleFetch);

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> REQUESTS.clear());
  }

  public static void sendSet(Map<String, Either<Boolean, Integer>> values) {
    ClientPlayNetworking.send(new Networking.SetC2S(values));
  }

  public static ServerRequest<Networking.FetchS2C> sendFetch(boolean mutableOnly) {
    ServerRequest<Networking.FetchS2C> request = REQUESTS.create(Networking.FetchS2C.class);
    ClientPlayNetworking.send(new Networking.FetchC2S(request.getReqId(), mutableOnly));
    return request;
  }

  private static void handleFetch(Networking.FetchS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> REQUESTS.resolve(payload.reqId(), payload));
  }
}
