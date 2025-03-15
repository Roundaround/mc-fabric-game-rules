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
      GameRules.SPAWN_CHUNK_RADIUS
  );
  private static final Set<GameRules.Key<?>> HARDCORE_NON_CHEAT = Set.of(
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

  public static RuleState getState(GameRules.Key<?> key, ServerPlayerEntity player) {
    MinecraftServer server = player.server;
    ServerWorld world = player.getServerWorld();

    boolean isMultiplayer = !server.isSingleplayer();
    boolean areCheatsEnabled = server.getPlayerManager().areCheatsAllowed();
    boolean hasOps = player.hasPermissionLevel(server.getOpPermissionLevel());
    boolean isHardcore = world.getLevelProperties().isHardcore();

    if (isMultiplayer && !hasOps) {
      return RuleState.DENIED;
    }

    RuleState state = switch (Constants.ACTIVE_VARIANT) {
      case Variant.HARDCORE ->
          isHardcore || (isMultiplayer && areCheatsEnabled) ? getStateForHardcore(key) : getStateForTechnical(key);
      case Variant.TECHNICAL -> getStateForTechnical(key);
      case Variant.BASE -> RuleState.MUTABLE;
    };

    if (isHardcore && state.equals(RuleState.MUTABLE)) {
      state = GameRulesStorage.getInstance(server).hasChanged(key) ? RuleState.LOCKED : state;
    }

    return state;
  }

  public static List<RuleInfo> collect(
      final GameRules gameRules,
      final ServerPlayerEntity player,
      @Nullable final Predicate<RuleState> stateFilter
  ) {
    final ArrayList<RuleInfo> ruleInfos = new ArrayList<>();
    GameRules.accept(new GameRules.Visitor() {
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

  private static RuleState getStateForHardcore(GameRules.Key<?> key) {
    return HARDCORE_NON_CHEAT.contains(key) ? RuleState.MUTABLE : RuleState.IMMUTABLE;
  }

  private static RuleState getStateForTechnical(GameRules.Key<?> key) {
    return TECHNICAL_NON_CHEAT.contains(key) ? RuleState.MUTABLE : RuleState.IMMUTABLE;
  }

  private static Date getChangedDate(GameRules.Key<?> key, ServerPlayerEntity player) {
    return GameRulesStorage.getInstance(player.server).getLastChangeDate(key);
  }

  private RuleInfoServerHelper() {
  }
}
