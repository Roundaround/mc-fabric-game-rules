package me.roundaround.gamerulesmod.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.world.GameRules.BooleanRule;
import net.minecraft.world.GameRules.Category;
import net.minecraft.world.GameRules.IntRule;
import net.minecraft.world.GameRules.Key;

public final class Util {
  public static Key<?> createRuleKey(String id) {
    return new Key<>(id, Category.PLAYER);
  }

  public static Key<BooleanRule> createBooleanKey(String id) {
    return new Key<>(id, Category.PLAYER);
  }

  public static Key<IntRule> createIntKey(String id) {
    return new Key<>(id, Category.PLAYER);
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

  private Util() {
  }
}
