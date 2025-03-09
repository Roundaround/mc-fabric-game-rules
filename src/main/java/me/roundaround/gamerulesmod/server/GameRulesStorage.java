package me.roundaround.gamerulesmod.server;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.GameRulesMod;
import me.roundaround.gamerulesmod.util.GameRuleHistory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.Date;
import java.util.HashMap;

public class GameRulesStorage extends PersistentState {
  private final HashMap<String, GameRuleHistory> history = new HashMap<>();

  private GameRulesStorage() {
  }

  public static GameRulesStorage getInstance(MinecraftServer server) {
    ServerWorld world;
    if (server == null || (world = server.getWorld(World.OVERWORLD)) == null) {
      IllegalStateException exception = new IllegalStateException(
          "Trying to get a GameRulesStorage instance when server is not running");
      GameRulesMod.LOGGER.error(exception);
      throw exception;
    }
    Type<GameRulesStorage> persistentStateType = new PersistentState.Type<>(
        GameRulesStorage::new,
        GameRulesStorage::fromNbt,
        null
    );
    return world.getPersistentStateManager().getOrCreate(persistentStateType, GameRulesMod.MOD_ID);
  }

  public boolean hasChanged(GameRules.Key<?> key) {
    return this.hasChanged(key.getName());
  }

  public boolean hasChanged(String id) {
    return this.history.containsKey(id) && this.history.get(id).hasChanged();
  }

  public int getChangeCount(GameRules.Key<?> key) {
    return this.getChangeCount(key.getName());
  }

  public int getChangeCount(String id) {
    return this.history.containsKey(id) ? this.history.get(id).getChangeCount() : 0;
  }

  public Date getLastChangeDate(GameRules.Key<?> key) {
    return this.getLastChangeDate(key.getName());
  }

  public Date getLastChangeDate(String id) {
    return this.history.containsKey(id) ? this.history.get(id).getLastChangeDate() : null;
  }

  public Either<Boolean, Integer> getOriginalValue(GameRules.Key<?> key) {
    return this.getOriginalValue(key.getName());
  }

  public Either<Boolean, Integer> getOriginalValue(String id) {
    return this.history.containsKey(id) ? this.history.get(id).getOriginalValue() : null;
  }

  public Either<Boolean, Integer> getPreviousValue(GameRules.Key<?> key) {
    return this.getPreviousValue(key.getName());
  }

  public Either<Boolean, Integer> getPreviousValue(String id) {
    return this.history.containsKey(id) ? this.history.get(id).getPreviousValue() : null;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    NbtList historyNbt = new NbtList();
    this.history.forEach((id, history) -> {
      NbtCompound entryNbt = new NbtCompound();
      entryNbt.putString("Key", id);
      historyNbt.add(history.writeNbt(entryNbt));
    });
    nbt.put("History", historyNbt);
    return nbt;
  }

  private static GameRulesStorage fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    GameRulesStorage storage = new GameRulesStorage();

    NbtList historyNbt = nbt.getList("History", NbtElement.COMPOUND_TYPE);
    for (NbtElement elementNbt : historyNbt) {
      NbtCompound entryNbt = (NbtCompound) elementNbt;
      String id = entryNbt.getString("Key");
      storage.history.put(id, GameRuleHistory.fromNbt(entryNbt));
    }

    return storage;
  }
}
