package me.roundaround.gamerulesmod.server.network;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.GameRulesMod;
import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gamerulesmod.server.GameRulesStorage;
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
      ServerWorld world = player.getServerWorld();
      if (world == null) {
        return;
      }

      sendFetch(
          player, payload.reqId(), RuleInfo.collect(
              world.getGameRules(),
              player,
              (state) -> payload.includeImmutable() || !state.equals(RuleInfo.State.IMMUTABLE)
          )
      );
    });
  }

  private static void handleSet(Networking.SetC2S payload, ServerPlayNetworking.Context context) {
    final ServerPlayerEntity player = context.player();
    final MinecraftServer server = player.server;
    server.execute(() -> {
      ServerWorld world = player.getServerWorld();
      if (world == null) {
        return;
      }

      final GameRules gameRules = world.getGameRules();
      final Set<String> mutableRules = RuleInfo.collect(
              gameRules,
              player,
              (state) -> state.equals(RuleInfo.State.MUTABLE)
          )
          .stream()
          .map(RuleInfo::id)
          .collect(Collectors.toSet());
      final GameRulesStorage historyStorage = GameRulesStorage.getInstance(server);
      final var warnCount = new Object() {
        int value = 0;
      };

      payload.values().forEach((id, either) -> {
        if (!mutableRules.contains(id)) {
          warnCount.value++;
        }

        Either<Boolean, Integer> previousValue = gameRules.gamerulesmod$getValue(id);
        gameRules.gamerulesmod$set(id, either);
        historyStorage.recordChange(id, previousValue);
      });

      if (warnCount.value > 0) {
        GameRulesMod.LOGGER.warn(
            "Player {} attempted to change {} game rule(s), but did not have permission to do so.",
            player.getGameProfile().getName(),
            warnCount.value
        );
      }
    });
  }
}
