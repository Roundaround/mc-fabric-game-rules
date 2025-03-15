package me.roundaround.gamerulesmod.mixin;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.common.gamerule.GameRulesExtensions;
import me.roundaround.gamerulesmod.common.gamerule.RuleHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

import static net.minecraft.world.GameRules.*;

@Mixin(GameRules.class)
public abstract class GameRulesMixin implements GameRulesExtensions {
  @Shadow
  @Final
  private Map<Key<?>, Rule<?>> rules;

  @Shadow
  public abstract <T extends GameRules.Rule<T>> T get(GameRules.Key<T> key);

  @Override
  public int gamerulesmod$size() {
    return this.rules.size();
  }

  @Override
  public Rule<?> gamerulesmod$get(String id) {
    return this.get(RuleHelper.createRuleKey(id));
  }

  @Override
  public Either<Boolean, Integer> gamerulesmod$getValue(String id) {
    return switch (this.rules.get(RuleHelper.createRuleKey(id))) {
      case BooleanRule booleanRule -> Either.left(booleanRule.get());
      case IntRule intRule -> Either.right(intRule.get());
      default -> Either.left(false);
    };
  }

  @Override
  public void gamerulesmod$set(String id, boolean value, MinecraftServer server) {
    this.get(RuleHelper.createBooleanKey(id)).set(value, server);
  }

  @Override
  public void gamerulesmod$set(String id, int value, MinecraftServer server) {
    this.get(RuleHelper.createIntKey(id)).set(value, server);
  }
}
