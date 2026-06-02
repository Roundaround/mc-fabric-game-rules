package me.roundaround.gamerulesmod.server.gamerule;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.common.gamerule.RuleHelper;
import me.roundaround.gamerulesmod.common.gamerule.RuleInfo;
import me.roundaround.gamerulesmod.common.gamerule.RuleState;
import me.roundaround.gamerulesmod.generated.Constants;
import me.roundaround.gamerulesmod.generated.Variant;
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
import java.util.Set;
import java.util.function.Predicate;

public final class RuleInfoServerHelper {
  private static final Set<GameRule<?>> TECHNICAL_NON_CHEAT = Set.of(
      GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER,
      GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS,
      GameRules.ANNOUNCE_ADVANCEMENTS,
      GameRules.COMMAND_BLOCKS_WORK,
      GameRules.COMMAND_BLOCK_OUTPUT,
      GameRules.MAX_BLOCK_MODIFICATIONS,
      GameRules.ELYTRA_MOVEMENT_CHECK,
      GameRules.DO_IMMEDIATE_RESPAWN,
      GameRules.LIMITED_CRAFTING,
      GameRules.DO_MOB_GRIEFING,
      GameRules.SPREAD_VINES,
      GameRules.GLOBAL_SOUND_EVENTS,
      GameRules.LOCATOR_BAR,
      GameRules.LOG_ADMIN_COMMANDS,
      GameRules.MAX_COMMAND_SEQUENCE_LENGTH,
      GameRules.MAX_COMMAND_FORKS,
      GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY,
      GameRules.PLAYERS_SLEEPING_PERCENTAGE,
      GameRules.PVP,
      GameRules.REDUCED_DEBUG_INFO,
      GameRules.SEND_COMMAND_FEEDBACK,
      GameRules.SHOW_DEATH_MESSAGES,
      GameRules.SPAWN_MONSTERS,
      GameRules.SPAWNER_BLOCKS_WORK,
      GameRules.TNT_EXPLODES
  );
  private static final Set<GameRule<?>> HARDCORE_NON_CHEAT = Set.of(
      GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER,
      GameRules.DO_MOB_GRIEFING,
      GameRules.SPREAD_VINES,
      GameRules.LOCATOR_BAR,
      GameRules.PVP
  );

  public static RuleInfo createInfo(GameRules gameRules, GameRule<?> rule, ServerPlayerEntity player) {
    String id = RuleHelper.idOf(rule);
    Either<Boolean, Integer> value = RuleHelper.getValue(gameRules, rule);
    if (player == null) {
      return new RuleInfo(id, value, RuleState.IMMUTABLE, null);
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
      return getStateForMultiplayer(rule, server, player);
    } else if (world.getLevelProperties().isHardcore()) {
      return getStateForSingleplayerHardcoreWorld(rule, server);
    }

    Variant variant = Constants.ACTIVE_VARIANT;
    if (server.getPlayerManager().areCheatsAllowed()) {
      variant = variant.equals(Variant.HARDCORE) ? Variant.TECHNICAL : Variant.BASE;
    }
    return getBaseStateForVariant(rule, variant);
  }

  private static RuleState getStateForSingleplayerHardcoreWorld(GameRule<?> rule, MinecraftServer server) {
    RuleState state = getBaseStateForVariant(rule, Constants.ACTIVE_VARIANT);
    return state.equals(RuleState.MUTABLE) && server.gamerulesmod$getGameRulesHistory().hasChanged(rule) ?
        RuleState.LOCKED :
        state;
  }

  private static RuleState getStateForMultiplayer(
      GameRule<?> rule,
      MinecraftServer server,
      ServerPlayerEntity player
  ) {
    if (!player.getPermissions().hasPermission(new Permission.Level(server.getOpPermissionLevel().getLevel()))) {
      return RuleState.DENIED;
    }
    return getBaseStateForVariant(rule, Constants.ACTIVE_VARIANT);
  }

  private static RuleState getBaseStateForVariant(GameRule<?> rule, Variant variant) {
    return switch (variant) {
      case Variant.HARDCORE -> getStateForHardcore(rule);
      case Variant.TECHNICAL -> getStateForTechnical(rule);
      case Variant.BASE -> RuleState.MUTABLE;
    };
  }

  private static RuleState getStateForHardcore(GameRule<?> rule) {
    return HARDCORE_NON_CHEAT.contains(rule) ? RuleState.MUTABLE : RuleState.IMMUTABLE;
  }

  private static RuleState getStateForTechnical(GameRule<?> rule) {
    return TECHNICAL_NON_CHEAT.contains(rule) ? RuleState.MUTABLE : RuleState.IMMUTABLE;
  }

  private static Date getChangedDate(GameRule<?> rule, ServerPlayerEntity player) {
    return player.getEntityWorld().getServer().gamerulesmod$getGameRulesHistory().getLastChangeDate(rule);
  }

  private RuleInfoServerHelper() {
  }
}
