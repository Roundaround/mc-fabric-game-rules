package me.roundaround.gamerulesmod.util;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record RuleInfo(String id, Either<Boolean, Integer> value, boolean mutable) {
  public static final PacketCodec<ByteBuf, RuleInfo> PACKET_CODEC = PacketCodec.tuple(
      PacketCodecs.STRING,
      RuleInfo::id,
      PacketCodecs.either(PacketCodecs.BOOL, PacketCodecs.INTEGER),
      RuleInfo::value,
      PacketCodecs.BOOL,
      RuleInfo::mutable,
      RuleInfo::new
  );
  public static final Set<GameRules.Key<?>> NON_CHEAT_RULES = Set.of(
      GameRules.ANNOUNCE_ADVANCEMENTS,
      GameRules.COMMAND_BLOCK_OUTPUT,
      GameRules.COMMAND_MODIFICATION_BLOCK_LIMIT,
      GameRules.DISABLE_ELYTRA_MOVEMENT_CHECK,
      GameRules.DO_FIRE_TICK,
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
      GameRules.SHOW_DEATH_MESSAGES
  );

  public static RuleInfo of(GameRules gameRules, GameRules.Key<?> key) {
    return of(gameRules, key, null);
  }

  public static RuleInfo of(GameRules gameRules, GameRules.Key<?> key, ServerPlayerEntity player) {
    String id = key.getName();
    Either<Boolean, Integer> value = gameRules.gamerulesmod$getValue(id);
    if (player == null) {
      return new RuleInfo(id, value, false);
    }

    boolean mutable = isMutable(key, player);
    return new RuleInfo(id, value, mutable);
  }

  public void applyValue(GameRules gameRules) {
    gameRules.gamerulesmod$set(this.id(), this.value());
  }

  public GameRules.Rule<?> getRule(GameRules gameRules) {
    return gameRules.gamerulesmod$get(this.id());
  }

  public static boolean isMutable(GameRules.Key<?> key, ServerPlayerEntity player) {
    if (NON_CHEAT_RULES.contains(key)) {
      return true;
    }

    MinecraftServer server = player.server;
    if (server.isSingleplayer()) {
      return server.getSaveProperties().areCommandsAllowed();
    }

    return player.hasPermissionLevel(2);
  }

  public static List<RuleInfo> collect(
      final GameRules gameRules, final ServerPlayerEntity player, final boolean includeImmutable
  ) {
    final ArrayList<RuleInfo> ruleInfos = new ArrayList<>();
    GameRules.accept(new GameRules.Visitor() {
      @Override
      public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
        RuleInfo ruleInfo = RuleInfo.of(gameRules, key, player);
        if (includeImmutable || ruleInfo.mutable()) {
          ruleInfos.add(ruleInfo);
        }
      }
    });
    return ruleInfos;
  }
}
