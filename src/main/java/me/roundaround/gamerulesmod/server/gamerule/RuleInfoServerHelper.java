package me.roundaround.gamerulesmod.server.gamerule;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.common.gamerule.RuleInfo;
import me.roundaround.gamerulesmod.common.gamerule.RuleState;
import me.roundaround.gamerulesmod.generated.Constants;
import me.roundaround.gamerulesmod.generated.Variant;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class RuleInfoServerHelper {
  private static final Set<GameRules.Key<?>> TECHNICAL_NON_CHEAT = Set.of(
      GameRules.ALLOW_FIRE_TICKS_AWAY_FROM_PLAYER,
      GameRules.ANNOUNCE_ADVANCEMENTS,
      GameRules.COMMAND_BLOCK_OUTPUT,
      GameRules.COMMAND_MODIFICATION_BLOCK_LIMIT,
      GameRules.DISABLE_ELYTRA_MOVEMENT_CHECK,
      GameRules.DO_FIRE_TICK,
      GameRules.DO_IMMEDIATE_RESPAWN,
      GameRules.DO_LIMITED_CRAFTING,
      GameRules.DO_MOB_GRIEFING,
      GameRules.DO_VINES_SPREAD,
      GameRules.GLOBAL_SOUND_EVENTS,
      GameRules.LOG_ADMIN_COMMANDS,
      GameRules.MAX_COMMAND_CHAIN_LENGTH,
      GameRules.MAX_COMMAND_FORK_COUNT,
      GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY,
      GameRules.PLAYERS_SLEEPING_PERCENTAGE,
      GameRules.REDUCED_DEBUG_INFO,
      GameRules.SEND_COMMAND_FEEDBACK,
      GameRules.SHOW_DEATH_MESSAGES,
      GameRules.SPAWN_CHUNK_RADIUS,
      GameRules.TNT_EXPLODES
  );
  private static final Set<GameRules.Key<?>> HARDCORE_NON_CHEAT = Set.of(
      GameRules.ALLOW_FIRE_TICKS_AWAY_FROM_PLAYER,
      GameRules.DO_FIRE_TICK,
      GameRules.DO_MOB_GRIEFING,
      GameRules.DO_VINES_SPREAD,
      GameRules.SPAWN_CHUNK_RADIUS
  );

  public static RuleInfo createInfo(GameRules gameRules, GameRules.Key<?> key, ServerPlayerEntity player) {
    String id = key.getName();
    Either<Boolean, Integer> value = gameRules.gamerulesmod$getValue(id);
    if (player == null) {
      return new RuleInfo(id, value, RuleState.IMMUTABLE, null);
    }

    return new RuleInfo(id, value, getState(key, player), getChangedDate(key, player));
  }

  public static List<RuleInfo> collect(
      final GameRules gameRules,
      final ServerPlayerEntity player,
      @Nullable final Predicate<RuleState> stateFilter
  ) {
    final ArrayList<RuleInfo> ruleInfos = new ArrayList<>();
    gameRules.accept(new GameRules.Visitor() {
      @Override
      public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
        if (!(gameRules.get(key) instanceof GameRules.BooleanRule) &&
            !(gameRules.get(key) instanceof GameRules.IntRule)) {
          return;
        }

        RuleInfo ruleInfo = createInfo(gameRules, key, player);
        if (stateFilter == null || stateFilter.test(ruleInfo.state())) {
          ruleInfos.add(ruleInfo);
        }
      }
    });
    return ruleInfos;
  }

  private static RuleState getState(GameRules.Key<?> key, ServerPlayerEntity player) {
    MinecraftServer server = player.server;
    ServerWorld world = player.getServerWorld();

    if (!server.isSingleplayer()) {
      return getStateForMultiplayer(key, server, player);
    } else if (world.getLevelProperties().isHardcore()) {
      return getStateForSingleplayerHardcoreWorld(key, server);
    }

    Variant variant = Constants.ACTIVE_VARIANT;
    if (server.getPlayerManager().areCheatsAllowed()) {
      variant = variant.equals(Variant.HARDCORE) ? Variant.TECHNICAL : Variant.BASE;
    }
    return getBaseStateForVariant(key, variant);
  }

  private static RuleState getStateForSingleplayerHardcoreWorld(GameRules.Key<?> key, MinecraftServer server) {
    RuleState state = getBaseStateForVariant(key, Constants.ACTIVE_VARIANT);
    return state.equals(RuleState.MUTABLE) && server.gamerulesmod$getGameRulesHistory().hasChanged(key) ?
        RuleState.LOCKED :
        state;
  }

  private static RuleState getStateForMultiplayer(
      GameRules.Key<?> key,
      MinecraftServer server,
      ServerPlayerEntity player
  ) {
    if (!player.hasPermissionLevel(server.getOpPermissionLevel())) {
      return RuleState.DENIED;
    }
    return getBaseStateForVariant(key, Constants.ACTIVE_VARIANT);
  }

  private static RuleState getBaseStateForVariant(GameRules.Key<?> key, Variant variant) {
    return switch (variant) {
      case Variant.HARDCORE -> getStateForHardcore(key);
      case Variant.TECHNICAL -> getStateForTechnical(key);
      case Variant.BASE -> RuleState.MUTABLE;
    };
  }

  private static RuleState getStateForHardcore(GameRules.Key<?> key) {
    return HARDCORE_NON_CHEAT.contains(key) ? RuleState.MUTABLE : RuleState.IMMUTABLE;
  }

  private static RuleState getStateForTechnical(GameRules.Key<?> key) {
    return TECHNICAL_NON_CHEAT.contains(key) ? RuleState.MUTABLE : RuleState.IMMUTABLE;
  }

  private static Date getChangedDate(GameRules.Key<?> key, ServerPlayerEntity player) {
    return player.server.gamerulesmod$getGameRulesHistory().getLastChangeDate(key);
  }

  private RuleInfoServerHelper() {
  }
}
