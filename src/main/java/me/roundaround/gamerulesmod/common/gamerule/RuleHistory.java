package me.roundaround.gamerulesmod.common.gamerule;

import com.mojang.datafixers.util.Either;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.Date;
import java.util.Objects;
import java.util.TreeMap;

public class RuleHistory {
  private final Either<Boolean, Integer> originalValue;
  private final TreeMap<Date, Either<Boolean, Integer>> changes;

  private RuleHistory(Either<Boolean, Integer> originalValue, TreeMap<Date, Either<Boolean, Integer>> changes) {
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

  public void recordChange(Either<Boolean, Integer> previousValue) {
    this.changes.put(new Date(), previousValue);
  }

  public NbtCompound writeNbt(NbtCompound nbt) {
    nbt.put("OriginalValue", RuleHelper.eitherToNbt(this.originalValue));

    NbtList changesNbt = new NbtList();
    this.changes.forEach((date, value) -> {
      NbtCompound entryNbt = new NbtCompound();
      entryNbt.putLong("Date", date.getTime());
      entryNbt.put("Value", RuleHelper.eitherToNbt(value));
      changesNbt.add(entryNbt);
    });
    nbt.put("Changes", changesNbt);

    return nbt;
  }

  public static RuleHistory fromNbt(NbtCompound nbt) {
    Either<Boolean, Integer> originalValue = RuleHelper.nbtToEither(Objects.requireNonNull(nbt.get("OriginalValue")));

    TreeMap<Date, Either<Boolean, Integer>> changes = new TreeMap<>();
    NbtList changesNbt = nbt.getList("Changes", NbtElement.COMPOUND_TYPE);
    for (NbtElement elementNbt : changesNbt) {
      NbtCompound entryNbt = (NbtCompound) elementNbt;
      Date date = new Date(entryNbt.getLong("Date"));
      Either<Boolean, Integer> value = RuleHelper.nbtToEither(Objects.requireNonNull(entryNbt.get("Value")));
      changes.put(date, value);
    }

    return new RuleHistory(originalValue, changes);
  }

  public static RuleHistory create(Either<Boolean, Integer> originalValue) {
    return new RuleHistory(originalValue, new TreeMap<>());
  }
}
