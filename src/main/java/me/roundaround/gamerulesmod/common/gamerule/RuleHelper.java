package me.roundaround.gamerulesmod.common.gamerule;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleType;
import net.minecraft.world.rule.GameRules;

public final class RuleHelper {
  public static String idOf(GameRule<?> rule) {
    return rule.getId().toString();
  }

  public static GameRule<?> getRule(String id) {
    Identifier identifier = Identifier.tryParse(id);
    return identifier == null ? null : Registries.GAME_RULE.get(identifier);
  }

  public static boolean isBoolean(GameRule<?> rule) {
    return rule.getType() == GameRuleType.BOOL;
  }

  public static boolean isInt(GameRule<?> rule) {
    return rule.getType() == GameRuleType.INT;
  }

  public static boolean isSupported(GameRule<?> rule) {
    return isBoolean(rule) || isInt(rule);
  }

  public static long size(GameRules gameRules) {
    return gameRules.streamRules().filter(RuleHelper::isSupported).count();
  }

  public static <T> Either<Boolean, Integer> getValue(GameRules gameRules, GameRule<T> rule) {
    T value = gameRules.getValue(rule);
    return (value instanceof Integer i) ? Either.right(i) : Either.left((Boolean) value);
  }

  public static Either<Boolean, Integer> getValue(GameRules gameRules, String id) {
    GameRule<?> rule = getRule(id);
    return rule == null ? null : getValue(gameRules, rule);
  }

  public static Either<Boolean, Integer> getDefaultValue(GameRule<?> rule) {
    Object defaultValue = rule.getDefaultValue();
    return (defaultValue instanceof Integer i) ? Either.right(i) : Either.left((Boolean) defaultValue);
  }

  @SuppressWarnings("unchecked")
  public static void setValue(GameRules gameRules, GameRule<?> rule, Either<Boolean, Integer> value,
      @Nullable MinecraftServer server) {
    if (isInt(rule)) {
      value.ifRight((i) -> gameRules.setValue((GameRule<Integer>) rule, i, server));
    } else {
      value.ifLeft((b) -> gameRules.setValue((GameRule<Boolean>) rule, b, server));
    }
  }

  public static void setValue(GameRules gameRules, String id, Either<Boolean, Integer> value) {
    GameRule<?> rule = getRule(id);
    if (rule != null) {
      setValue(gameRules, rule, value, null);
    }
  }

  public static NbtElement eitherToNbt(Either<Boolean, Integer> value) {
    return value.map(NbtByte::of, NbtInt::of);
  }

  public static Either<Boolean, Integer> nbtToEither(NbtElement nbt) {
    if (nbt.getType() == NbtElement.BYTE_TYPE) {
      return Either.left(((AbstractNbtNumber) nbt).byteValue() != 0);
    }
    return Either.right(((AbstractNbtNumber) nbt).intValue());
  }

  private RuleHelper() {
  }
}
