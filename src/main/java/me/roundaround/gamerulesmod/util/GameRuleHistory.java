package me.roundaround.gamerulesmod.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.Date;
import java.util.Objects;
import java.util.TreeMap;

public class GameRuleHistory {
  private final Either<Boolean, Integer> originalValue;
  private final TreeMap<Date, Either<Boolean, Integer>> changes;

  private GameRuleHistory(Either<Boolean, Integer> originalValue, TreeMap<Date, Either<Boolean, Integer>> changes) {
    this.originalValue = originalValue;
    this.changes = changes;
  }

  public boolean hasChanged() {
    return !this.changes.isEmpty();
  }

  public int getChangeCount() {
    return this.changes.size();
  }

  public Date getLastChangeDate() {
    return this.changes.isEmpty() ? null : this.changes.lastKey();
  }

  public Either<Boolean, Integer> getOriginalValue() {
    return this.originalValue;
  }

  public Either<Boolean, Integer> getPreviousValue() {
    return this.changes.isEmpty() ? null : this.changes.lastEntry().getValue();
  }

  public NbtCompound writeNbt(NbtCompound nbt) {
    nbt.put("OriginalValue", Util.eitherToNbt(this.originalValue));

    NbtList changesNbt = new NbtList();
    this.changes.forEach((date, value) -> {
      NbtCompound entryNbt = new NbtCompound();
      entryNbt.putLong("Date", date.getTime());
      entryNbt.put("Value", Util.eitherToNbt(value));
      changesNbt.add(entryNbt);
    });
    nbt.put("Changes", changesNbt);

    return nbt;
  }

  public static GameRuleHistory fromNbt(NbtCompound nbt) {
    Either<Boolean, Integer> originalValue = Util.nbtToEither(Objects.requireNonNull(nbt.get("OriginalValue")));

    TreeMap<Date, Either<Boolean, Integer>> changes = new TreeMap<>();
    NbtList changesNbt = nbt.getList("Changes", NbtElement.COMPOUND_TYPE);
    for (NbtElement elementNbt : changesNbt) {
      NbtCompound entryNbt = (NbtCompound) elementNbt;
      Date date = new Date(entryNbt.getLong("Date"));
      Either<Boolean, Integer> value = Util.nbtToEither(Objects.requireNonNull(entryNbt.get("Value")));
      changes.put(date, value);
    }

    return new GameRuleHistory(originalValue, changes);
  }
}
