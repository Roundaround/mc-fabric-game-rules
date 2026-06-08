package me.roundaround.gamerulesmod.server.gamerule;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.roundaround.gamerulesmod.GameRules;
import me.roundaround.gamerulesmod.common.gamerule.RuleHelper;
import me.roundaround.gamerulesmod.common.gamerule.RuleHistory;
import me.roundaround.gamerulesmod.generated.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRulesStorage extends SavedData {
  public static final Codec<GameRulesStorage> CODEC = Codec.unboundedMap(Codec.STRING, RuleHistory.CODEC)
      .xmap(GameRulesStorage::new, GameRulesStorage::getHistory);
  public static final Codec<GameRulesStorage> LIST_STYLE_CODEC = RecordCodecBuilder.create((instance) -> instance.group(
          Codec.list(RuleHistory.ListStyle.CODEC).fieldOf("History").forGetter(GameRulesStorage::getHistoryValues))
      .apply(instance, GameRulesStorage::new));
  public static SavedDataType<GameRulesStorage> STATE_TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "history"),
      GameRulesStorage::new,
      Codec.withAlternative(CODEC, LIST_STYLE_CODEC),
      null
  );

  /**
   * Obtain the server-wide storage, importing legacy history once. Builds &lt; 2.0.0 (yarn/1.21.x) stored
   * the history at the flat path {@code <save>/data/gamerulesmod.dat}; the mojmap {@link SavedDataType}
   * namespaces the file to {@code <save>/data/gamerulesmod/history.dat}, so a one-time import of the old
   * file is needed when upgrading a world. Call once on server start (the new file always wins if present).
   */
  public static GameRulesStorage bootstrap(MinecraftServer server) {
    GameRulesStorage storage = server.getDataStorage().computeIfAbsent(STATE_TYPE);
    if (!storage.history.isEmpty()) {
      // New-format data already loaded; nothing to import.
      return storage;
    }

    Path legacyFile = server.getWorldPath(LevelResource.DATA).resolve(Constants.MOD_ID + ".dat");
    if (!Files.isRegularFile(legacyFile)) {
      return storage;
    }

    try {
      CompoundTag root = NbtIo.readCompressed(legacyFile, NbtAccounter.unlimitedHeap());
      root.getCompound("data").ifPresent((data) -> Codec.withAlternative(CODEC, LIST_STYLE_CODEC)
          .parse(NbtOps.INSTANCE, data)
          .resultOrPartial((error) -> GameRules.LOGGER.warn("Could not parse legacy game-rule history: {}", error))
          .ifPresent((legacy) -> {
            // The map/list-style ctors already ran LegacyRuleHistoryMigration on the decoded keys.
            storage.history.putAll(legacy.history);
            storage.setDirty();
            GameRules.LOGGER.info(
                "Imported {} legacy game-rule history entr(ies) from {}", legacy.history.size(), legacyFile);
          }));
    } catch (Exception e) {
      GameRules.LOGGER.error("Failed to import legacy game-rule history from {}", legacyFile, e);
    }
    return storage;
  }

  private final HashMap<String, RuleHistory> history = new HashMap<>();

  private GameRulesStorage() {
    this.setDirty();
  }

  private GameRulesStorage(Map<String, RuleHistory> history) {
    // Migrate pre-1.21.11 camelCase keys to the registry ids; re-save if anything changed.
    if (LegacyRuleHistoryMigration.migrate(history, this.history)) {
      this.setDirty();
    }
  }

  private GameRulesStorage(List<RuleHistory.ListStyle> historyValues) {
    HashMap<String, RuleHistory> loaded = new HashMap<>();
    for (RuleHistory.ListStyle history : historyValues) {
      loaded.put(history.key(), history.toMapStyle());
    }
    LegacyRuleHistoryMigration.migrate(loaded, this.history);
    // Mark dirty to force re-saving in the new (map-based, migrated) format
    this.setDirty();
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
    this.setDirty();
  }
}
