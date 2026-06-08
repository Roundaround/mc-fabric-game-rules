package me.roundaround.gamerulesmod.common.gamerule;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;

public final class RuleHelper {
  public static String idOf(GameRule<?> rule) {
    return rule.getIdentifier().toString();
  }

  public static GameRule<?> getRule(String id) {
    Identifier identifier = Identifier.tryParse(id);
    return identifier == null ? null : BuiltInRegistries.GAME_RULE.getValue(identifier);
  }

  public static boolean isBoolean(GameRule<?> rule) {
    return rule.gameRuleType() == GameRuleType.BOOL;
  }

  public static boolean isInt(GameRule<?> rule) {
    return rule.gameRuleType() == GameRuleType.INT;
  }

  public static boolean isSupported(GameRule<?> rule) {
    return isBoolean(rule) || isInt(rule);
  }

  public static long size(GameRules gameRules) {
    return gameRules.availableRules().filter(RuleHelper::isSupported).count();
  }

  public static <T> Either<Boolean, Integer> getValue(GameRules gameRules, GameRule<T> rule) {
    T value = gameRules.get(rule);
    return (value instanceof Integer i) ? Either.right(i) : Either.left((Boolean) value);
  }

  public static Either<Boolean, Integer> getValue(GameRules gameRules, String id) {
    GameRule<?> rule = getRule(id);
    return rule == null ? null : getValue(gameRules, rule);
  }

  public static Either<Boolean, Integer> getDefaultValue(GameRule<?> rule) {
    Object defaultValue = rule.defaultValue();
    return (defaultValue instanceof Integer i) ? Either.right(i) : Either.left((Boolean) defaultValue);
  }

  @SuppressWarnings("unchecked")
  public static void setValue(GameRules gameRules, GameRule<?> rule, Either<Boolean, Integer> value,
      @Nullable MinecraftServer server) {
    if (isInt(rule)) {
      value.ifRight((i) -> gameRules.set((GameRule<Integer>) rule, i, server));
    } else {
      value.ifLeft((b) -> gameRules.set((GameRule<Boolean>) rule, b, server));
    }
  }

  public static void setValue(GameRules gameRules, String id, Either<Boolean, Integer> value) {
    GameRule<?> rule = getRule(id);
    if (rule != null) {
      setValue(gameRules, rule, value, null);
    }
  }

  public static Tag eitherToNbt(Either<Boolean, Integer> value) {
    return value.map(ByteTag::valueOf, IntTag::valueOf);
  }

  public static Either<Boolean, Integer> nbtToEither(Tag nbt) {
    if (nbt.getId() == Tag.TAG_BYTE) {
      return Either.left(((NumericTag) nbt).byteValue() != 0);
    }
    return Either.right(((NumericTag) nbt).intValue());
  }

  private RuleHelper() {
  }
}
