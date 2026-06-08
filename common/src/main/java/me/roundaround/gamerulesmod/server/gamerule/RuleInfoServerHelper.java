package me.roundaround.gamerulesmod.server.gamerule;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.common.gamerule.RuleHelper;
import me.roundaround.gamerulesmod.common.gamerule.RuleInfo;
import me.roundaround.gamerulesmod.common.gamerule.RuleState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

public final class RuleInfoServerHelper {
  public static RuleInfo createInfo(GameRules gameRules, GameRule<?> rule, ServerPlayer player) {
    String id = RuleHelper.idOf(rule);
    Either<Boolean, Integer> value = RuleHelper.getValue(gameRules, rule);
    if (player == null) {
      return new RuleInfo(id, value, RuleState.DENIED, null);
    }

    return new RuleInfo(id, value, getState(rule, player), getChangedDate(rule, player));
  }

  public static List<RuleInfo> collect(
      final GameRules gameRules,
      final ServerPlayer player,
      @Nullable final Predicate<RuleState> stateFilter
  ) {
    final ArrayList<RuleInfo> ruleInfos = new ArrayList<>();
    gameRules.availableRules().filter(RuleHelper::isSupported).forEach((rule) -> {
      RuleInfo ruleInfo = createInfo(gameRules, rule, player);
      if (stateFilter == null || stateFilter.test(ruleInfo.state())) {
        ruleInfos.add(ruleInfo);
      }
    });
    return ruleInfos;
  }

  private static RuleState getState(GameRule<?> rule, ServerPlayer player) {
    ServerLevel world = player.level();
    MinecraftServer server = world.getServer();

    if (!server.isSingleplayer()) {
      return getStateForMultiplayer(server, player);
    }

    // Hardcore worlds keep the "change once" balancing rule even in the base variant: a rule stays
    // editable until it has been changed once, after which it locks.
    if (server.isHardcore()
        && server.getDataStorage().computeIfAbsent(GameRulesStorage.STATE_TYPE).hasChanged(rule)) {
      return RuleState.LOCKED;
    }

    return RuleState.MUTABLE;
  }

  private static RuleState getStateForMultiplayer(MinecraftServer server, ServerPlayer player) {
    // Honor the server's configured op-permission level (op-permission-level in server.properties),
    // matching the pre-port behavior, rather than a fixed gamemaster (level 2) gate.
    Permission required = new Permission.HasCommandLevel(server.operatorUserPermissions().level());
    if (!player.permissions().hasPermission(required)) {
      return RuleState.DENIED;
    }
    return RuleState.MUTABLE;
  }

  private static Date getChangedDate(GameRule<?> rule, ServerPlayer player) {
    return player.level()
        .getServer()
        .getDataStorage()
        .computeIfAbsent(GameRulesStorage.STATE_TYPE)
        .getLastChangeDate(rule);
  }

  private RuleInfoServerHelper() {
  }
}
