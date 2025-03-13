package me.roundaround.gamerulesmod.util;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import me.roundaround.gamerulesmod.generated.Constants;
import me.roundaround.gamerulesmod.generated.Variant;
import me.roundaround.gamerulesmod.roundalib.network.CustomCodecs;
import me.roundaround.gamerulesmod.server.GameRulesStorage;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.function.ValueLists;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public record RuleInfo(String id, Either<Boolean, Integer> value, State state, Date changed) {
  public static final PacketCodec<ByteBuf, RuleInfo> PACKET_CODEC = PacketCodec.tuple(
      PacketCodecs.STRING,
      RuleInfo::id,
      PacketCodecs.either(PacketCodecs.BOOL, PacketCodecs.INTEGER),
      RuleInfo::value,
      State.PACKET_CODEC,
      RuleInfo::state,
      CustomCodecs.nullable(CustomCodecs.DATE),
      RuleInfo::changed,
      RuleInfo::new
  );

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

  public static RuleInfo of(GameRules gameRules, GameRules.Key<?> key, ServerPlayerEntity player) {
    String id = key.getName();
    Either<Boolean, Integer> value = gameRules.gamerulesmod$getValue(id);
    if (player == null) {
      return new RuleInfo(id, value, State.IMMUTABLE, null);
    }

    return new RuleInfo(id, value, getState(key, player), getChangedDate(key, player));
  }

  public void applyValue(GameRules gameRules) {
    gameRules.gamerulesmod$set(this.id(), this.value());
  }

  public static State getState(GameRules.Key<?> key, ServerPlayerEntity player) {
    MinecraftServer server = player.server;
    ServerWorld world = player.getServerWorld();

    if (!server.isSingleplayer() && !player.hasPermissionLevel(server.getOpPermissionLevel())) {
      return State.IMMUTABLE;
    }

    return switch (Constants.ACTIVE_VARIANT) {
      case Variant.HARDCORE -> getStateForHardcore(key, server, world);
      case Variant.TECHNICAL -> getStateForTechnical(key, server);
      case Variant.BASE -> State.MUTABLE;
    };
  }

  private static State getStateForHardcore(GameRules.Key<?> key, MinecraftServer server, ServerWorld world) {
    if (!world.getLevelProperties().isHardcore()) {
      return getStateForTechnical(key, server);
    }
    if (!HARDCORE_NON_CHEAT.contains(key)) {
      return State.IMMUTABLE;
    }
    return GameRulesStorage.getInstance(server).hasChanged(key) ? State.LOCKED : State.MUTABLE;
  }

  private static State getStateForTechnical(GameRules.Key<?> key, MinecraftServer server) {
    return TECHNICAL_NON_CHEAT.contains(key) ? State.MUTABLE : State.IMMUTABLE;
  }

  private static Date getChangedDate(GameRules.Key<?> key, ServerPlayerEntity player) {
    return GameRulesStorage.getInstance(player.server).getLastChangeDate(key);
  }

  public static List<RuleInfo> collect(
      final GameRules gameRules,
      final ServerPlayerEntity player,
      @Nullable final Predicate<State> stateFilter
  ) {
    final ArrayList<RuleInfo> ruleInfos = new ArrayList<>();
    GameRules.accept(new GameRules.Visitor() {
      @Override
      public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
        if (!(gameRules.get(key) instanceof GameRules.BooleanRule) &&
            !(gameRules.get(key) instanceof GameRules.IntRule)) {
          return;
        }

        RuleInfo ruleInfo = RuleInfo.of(gameRules, key, player);
        if (stateFilter == null || stateFilter.test(ruleInfo.state())) {
          ruleInfos.add(ruleInfo);
        }
      }
    });
    return ruleInfos;
  }

  public enum State {
    MUTABLE(0), IMMUTABLE(1), LOCKED(2);

    public static final IntFunction<State> ID_TO_VALUE_FUNCTION = ValueLists.createIdToValueFunction(
        State::getId,
        values(),
        ValueLists.OutOfBoundsHandling.WRAP
    );
    public static final PacketCodec<ByteBuf, State> PACKET_CODEC = PacketCodecs.indexed(
        ID_TO_VALUE_FUNCTION,
        State::getId
    );

    private final int id;

    State(int id) {
      this.id = id;
    }

    public int getId() {
      return this.id;
    }
  }
}
