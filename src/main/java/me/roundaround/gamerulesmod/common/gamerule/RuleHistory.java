package me.roundaround.gamerulesmod.common.gamerule;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

public class RuleHistory {
  public static final Codec<Date> DATE_STRING_CODEC = Codec.STRING.xmap(Long::parseLong, String::valueOf)
      .xmap(Date::new, Date::getTime);
  public static final Codec<Either<Boolean, Integer>> VALUE_CODEC = Codec.either(Codec.BOOL, Codec.INT);
  public static final Codec<RuleHistory> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
      VALUE_CODEC.fieldOf("originalValue").forGetter(RuleHistory::getOriginalValue),
      Codec.unboundedMap(DATE_STRING_CODEC, VALUE_CODEC)
          .xmap(TreeMap::new, Function.identity())
          .fieldOf("changes")
          .forGetter(RuleHistory::getChanges)
  ).apply(instance, RuleHistory::new));

  private final Either<Boolean, Integer> originalValue;
  private final TreeMap<Date, Either<Boolean, Integer>> changes;

  private RuleHistory(Either<Boolean, Integer> originalValue, TreeMap<Date, Either<Boolean, Integer>> changes) {
    this.originalValue = originalValue;
    this.changes = changes;
  }

  public Either<Boolean, Integer> getOriginalValue() {
    return this.originalValue;
  }

  public TreeMap<Date, Either<Boolean, Integer>> getChanges() {
    return new TreeMap<>(this.changes);
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

  public Either<Boolean, Integer> getPreviousValue() {
    return this.changes.isEmpty() ? null : this.changes.lastEntry().getValue();
  }

  public void recordChange(Either<Boolean, Integer> previousValue) {
    this.changes.put(new Date(), previousValue);
  }

  public static RuleHistory create(Either<Boolean, Integer> originalValue) {
    return new RuleHistory(originalValue, new TreeMap<>());
  }

  public record ListStyle(List<DatedChange> changes, Either<Boolean, Integer> originalValue, String key) {
    public static final Codec<ListStyle> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        Codec.list(DatedChange.CODEC).fieldOf("Changes").forGetter(ListStyle::changes),
        VALUE_CODEC.fieldOf("OriginalValue").forGetter(ListStyle::originalValue),
        Codec.STRING.fieldOf("Key").forGetter(ListStyle::key)
    ).apply(instance, ListStyle::new));

    public static ListStyle fromMapStyle(RuleHistory history, String key) {
      ArrayList<DatedChange> changes = new ArrayList<>();
      history.changes.forEach((date, value) -> changes.add(new DatedChange(value, date)));
      return new ListStyle(changes, history.originalValue, key);
    }

    public RuleHistory toMapStyle() {
      TreeMap<Date, Either<Boolean, Integer>> changes = new TreeMap<>();
      for (DatedChange change : this.changes()) {
        changes.put(change.date(), change.value());
      }
      return new RuleHistory(this.originalValue(), changes);
    }
  }

  public record DatedChange(Either<Boolean, Integer> value, Date date) {
    public static final Codec<Date> DATE_LONG_CODEC = Codec.LONG.xmap(Date::new, Date::getTime);
    public static final Codec<DatedChange> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        VALUE_CODEC.fieldOf("Value").forGetter(DatedChange::value),
        DATE_LONG_CODEC.fieldOf("Date").forGetter(DatedChange::date)
    ).apply(instance, DatedChange::new));
  }
}
