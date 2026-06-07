package me.roundaround.gamerulesmod.server.gamerule;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.common.gamerule.RuleHelper;
import me.roundaround.gamerulesmod.common.gamerule.RuleInfo;
import me.roundaround.gamerulesmod.common.gamerule.RuleState;
import net.minecraft.command.permission.Permission;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

public final class RuleInfoServerHelper {
  public static RuleInfo createInfo(GameRules gameRules, GameRule<?> rule, ServerPlayerEntity player) {
    String id = RuleHelper.idOf(rule);
    Either<Boolean, Integer> value = RuleHelper.getValue(gameRules, rule);
    if (player == null) {
      return new RuleInfo(id, value, RuleState.DENIED, null);
    }

    return new RuleInfo(id, value, getState(rule, player), getChangedDate(rule, player));
  }

  public static List<RuleInfo> collect(
      final GameRules gameRules,
      final ServerPlayerEntity player,
      @Nullable final Predicate<RuleState> stateFilter
  ) {
    final ArrayList<RuleInfo> ruleInfos = new ArrayList<>();
    gameRules.streamRules().filter(RuleHelper::isSupported).forEach((rule) -> {
      RuleInfo ruleInfo = createInfo(gameRules, rule, player);
      if (stateFilter == null || stateFilter.test(ruleInfo.state())) {
        ruleInfos.add(ruleInfo);
      }
    });
    return ruleInfos;
  }

  private static RuleState getState(GameRule<?> rule, ServerPlayerEntity player) {
    ServerWorld world = player.getEntityWorld();
    MinecraftServer server = world.getServer();

    if (!server.isSingleplayer()) {
      return getStateForMultiplayer(server, player);
    }

    // Hardcore worlds keep the "change once" balancing rule even in the base variant: a rule stays
    // editable until it has been changed once, after which it locks.
    if (world.getLevelProperties().isHardcore() && server.gamerulesmod$getGameRulesHistory().hasChanged(rule)) {
      return RuleState.LOCKED;
    }

    return RuleState.MUTABLE;
  }

  private static RuleState getStateForMultiplayer(MinecraftServer server, ServerPlayerEntity player) {
    if (!player.getPermissions().hasPermission(new Permission.Level(server.getOpPermissionLevel().getLevel()))) {
      return RuleState.DENIED;
    }
    return RuleState.MUTABLE;
  }

  private static Date getChangedDate(GameRule<?> rule, ServerPlayerEntity player) {
    return player.getEntityWorld().getServer().gamerulesmod$getGameRulesHistory().getLastChangeDate(rule);
  }

  private RuleInfoServerHelper() {
  }
}
