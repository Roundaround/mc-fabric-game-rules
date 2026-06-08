package me.roundaround.gamerulesmod.client.network;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.trove.event.ClientLifecycle;
import me.roundaround.trove.network.TroveNetworking;
import me.roundaround.trove.network.request.RequestTracker;
import me.roundaround.trove.network.request.ServerRequest;

import java.util.Map;

public final class ClientNetworking {
  private static final RequestTracker REQUESTS = new RequestTracker();

  private ClientNetworking() {
  }

  public static void registerReceivers() {
    // The S2C receiver itself is registered via TroveNetworking in Networking.register().
    // Here we only clear pending requests when the client disconnects from the world.
    ClientLifecycle.onStop(REQUESTS::clear);
  }

  public static void sendSet(Map<String, Either<Boolean, Integer>> values) {
    TroveNetworking.sendToServer(new Networking.SetC2S(values));
  }

  public static ServerRequest<Networking.FetchS2C> sendFetch() {
    ServerRequest<Networking.FetchS2C> request = REQUESTS.create(Networking.FetchS2C.class);
    TroveNetworking.sendToServer(new Networking.FetchC2S(request.getReqId()));
    return request;
  }

  // Dispatched on the client thread by the loader networking bridge.
  public static void handleFetch(Networking.FetchS2C payload) {
    REQUESTS.resolve(payload.reqId(), payload);
  }
}
