package me.roundaround.gamerulesmod.server.gamerule;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.roundaround.gamerulesmod.common.gamerule.RuleHelper;
import me.roundaround.gamerulesmod.common.gamerule.RuleHistory;
import me.roundaround.gamerulesmod.generated.Constants;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.rule.GameRule;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRulesStorage extends PersistentState {
  public static final Codec<GameRulesStorage> CODEC = Codec.unboundedMap(Codec.STRING, RuleHistory.CODEC)
      .xmap(GameRulesStorage::new, GameRulesStorage::getHistory);
  public static final Codec<GameRulesStorage> LIST_STYLE_CODEC = RecordCodecBuilder.create((instance) -> instance.group(
          Codec.list(RuleHistory.ListStyle.CODEC).fieldOf("History").forGetter(GameRulesStorage::getHistoryValues))
      .apply(instance, GameRulesStorage::new));
  public static PersistentStateType<GameRulesStorage> STATE_TYPE = new PersistentStateType<>(
      Constants.MOD_ID,
      GameRulesStorage::new,
      Codec.withAlternative(CODEC, LIST_STYLE_CODEC),
      null
  );

  private final HashMap<String, RuleHistory> history = new HashMap<>();

  private GameRulesStorage() {
    this.markDirty();
  }

  private GameRulesStorage(Map<String, RuleHistory> history) {
    this.history.putAll(history);
  }

  private GameRulesStorage(List<RuleHistory.ListStyle> historyValues) {
    for (RuleHistory.ListStyle history : historyValues) {
      this.history.put(history.key(), history.toMapStyle());
    }
    // Mark dirty to force re-saving in the new map-based format
    this.markDirty();
  }

  public Map<String, RuleHistory> getHistory() {
    return Map.copyOf(this.history);
  }

  public List<RuleHistory.ListStyle> getHistoryValues() {
    return this.history.entrySet()
        .stream()
        .map((entry) -> RuleHistory.ListStyle.fromMapStyle(entry.getValue(), entry.getKey()))
        .toList();
  }

  public boolean hasChanged(GameRule<?> rule) {
    return this.hasChanged(RuleHelper.idOf(rule));
  }

  public boolean hasChanged(String id) {
    return this.history.containsKey(id) && this.history.get(id).hasChanged();
  }

  public int getChangeCount(GameRule<?> rule) {
    return this.getChangeCount(RuleHelper.idOf(rule));
  }

  public int getChangeCount(String id) {
    return this.history.containsKey(id) ? this.history.get(id).getChangeCount() : 0;
  }

  public Date getLastChangeDate(GameRule<?> rule) {
    return this.getLastChangeDate(RuleHelper.idOf(rule));
  }

  public Date getLastChangeDate(String id) {
    return this.history.containsKey(id) ? this.history.get(id).getLastChangeDate() : null;
  }

  public Either<Boolean, Integer> getOriginalValue(GameRule<?> rule) {
    return this.getOriginalValue(RuleHelper.idOf(rule));
  }

  public Either<Boolean, Integer> getOriginalValue(String id) {
    return this.history.containsKey(id) ? this.history.get(id).getOriginalValue() : null;
  }

  public Either<Boolean, Integer> getPreviousValue(GameRule<?> rule) {
    return this.getPreviousValue(RuleHelper.idOf(rule));
  }

  public Either<Boolean, Integer> getPreviousValue(String id) {
    return this.history.containsKey(id) ? this.history.get(id).getPreviousValue() : null;
  }

  public void recordChange(GameRule<?> rule, Either<Boolean, Integer> previousValue) {
    this.recordChange(RuleHelper.idOf(rule), previousValue);
  }

  public void recordChange(String id, Either<Boolean, Integer> previousValue) {
    this.history.computeIfAbsent(id, (key) -> RuleHistory.create(previousValue)).recordChange(previousValue);
    this.markDirty();
  }
}
