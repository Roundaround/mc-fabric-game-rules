package me.roundaround.gamerulesmod.mixin;

import me.roundaround.gamerulesmod.GameRulesMod;
import me.roundaround.gamerulesmod.server.MinecraftServerExtensions;
import me.roundaround.gamerulesmod.server.gamerule.GameRulesStorage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements MinecraftServerExtensions {
  @Shadow
  public abstract @Nullable ServerWorld getWorld(RegistryKey<World> key);

  @Override
  public GameRulesStorage gamerulesmod$getGameRulesHistory() {
    ServerWorld world;
    if ((world = this.getWorld(World.OVERWORLD)) == null) {
      IllegalStateException exception = new IllegalStateException(
          "Trying to get a GameRulesStorage instance when server is not running");
      GameRulesMod.LOGGER.error(exception);
      throw exception;
    }
    return world.getPersistentStateManager().getOrCreate(GameRulesStorage.STATE_TYPE);
  }
}
