package me.roundaround.gamerulesmod.server.network;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.GameRulesMod;
import me.roundaround.gamerulesmod.common.gamerule.RuleHelper;
import me.roundaround.gamerulesmod.common.gamerule.RuleInfo;
import me.roundaround.gamerulesmod.common.gamerule.RuleState;
import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gamerulesmod.server.gamerule.GameRulesStorage;
import me.roundaround.gamerulesmod.server.gamerule.RuleInfoServerHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.rule.GameRules;

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
    context.server().execute(() -> {
      ServerPlayerEntity player = context.player();
      ServerWorld world = player.getEntityWorld();
      if (world == null) {
        return;
      }

      sendFetch(
          player,
          payload.reqId(),
          RuleInfoServerHelper.collect(world.getGameRules(), player, null)
      );
    });
  }

  private static void handleSet(Networking.SetC2S payload, ServerPlayNetworking.Context context) {
    context.server().execute(() -> {
      ServerPlayerEntity player = context.player();
      ServerWorld world = player.getEntityWorld();
      if (world == null) {
        return;
      }

      final GameRules gameRules = world.getGameRules();
      final Set<String> mutableRules = RuleInfoServerHelper.collect(
              gameRules,
              player,
              (state) -> state.equals(RuleState.MUTABLE)
          )
          .stream()
          .map(RuleInfo::id)
          .collect(Collectors.toSet());
      final GameRulesStorage historyStorage = world.getServer().gamerulesmod$getGameRulesHistory();
      final var warnCount = new Object() {
        int value = 0;
      };

      payload.values().forEach((id, either) -> {
        if (!mutableRules.contains(id)) {
          warnCount.value++;
        }

        Either<Boolean, Integer> previousValue = RuleHelper.getValue(gameRules, id);
        if (previousValue == null) {
          // Unknown or unregistered rule id (e.g. from a malformed packet); skip so we never
          // store a null value into the history, which the value codec cannot serialize.
          return;
        }
        RuleHelper.setValue(gameRules, id, either);
        historyStorage.recordChange(id, previousValue);
      });

      if (warnCount.value > 0) {
        GameRulesMod.LOGGER.warn(
            "Player {} attempted to change {} game rule(s), but did not have permission to do so.",
            player.getPlayerConfigEntry().name(),
            warnCount.value
        );
      }
    });
  }
}
