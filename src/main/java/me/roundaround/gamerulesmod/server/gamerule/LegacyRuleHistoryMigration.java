package me.roundaround.gamerulesmod.server.gamerule;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.common.gamerule.RuleHistory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Migrates persisted {@link RuleHistory} keys from the pre-1.21.11 camelCase rule names to the
 * 1.21.11 registry ids (e.g. {@code keepInventory} -> {@code minecraft:keep_inventory}).
 * <p>
 * The rename table mirrors vanilla's {@code GameRuleRegistryFix} datafixer exactly, including the
 * three rules whose meaning was inverted and the two boolean fire rules that were consolidated into
 * the integer {@code minecraft:fire_spread_radius_around_player}. Rules that vanilla removed
 * outright ({@code spawnChunkRadius}, {@code entitiesWithPassengersCanUsePortals}) are dropped.
 * Any key that already looks like a namespaced id (or is otherwise unrecognized) is preserved
 * as-is so we never silently lose data.
 */
public final class LegacyRuleHistoryMigration {
  // History values are already typed (Either<Boolean, Integer>), so these transforms only adjust
  // the value where the rule's semantics or type changed -- not parse strings like the datafixer.
  private static final Function<Either<Boolean, Integer>, Either<Boolean, Integer>> IDENTITY = Function.identity();
  private static final Function<Either<Boolean, Integer>, Either<Boolean, Integer>> INVERT =
      (value) -> value.mapLeft((b) -> !b);
  // doFireTick (boolean) -> fire_spread_radius_around_player (int): off = 0, on = vanilla default 128.
  private static final Function<Either<Boolean, Integer>, Either<Boolean, Integer>> FIRE_TICK =
      (value) -> Either.right(value.map((b) -> b ? 128 : 0, (i) -> i));
  // allowFireTicksAwayFromPlayer (boolean) -> fire_spread_radius_around_player (int): away = -1, near-only = 128.
  private static final Function<Either<Boolean, Integer>, Either<Boolean, Integer>> FIRE_AWAY =
      (value) -> Either.right(value.map((b) -> b ? -1 : 128, (i) -> i));

  private static final Map<String, Target> RENAMES = new HashMap<>();
  private static final Set<String> REMOVED = Set.of("spawnChunkRadius", "entitiesWithPassengersCanUsePortals");

  private record Target(String newId, Function<Either<Boolean, Integer>, Either<Boolean, Integer>> transform) {
  }

  private static void put(String oldName, String newId) {
    put(oldName, newId, IDENTITY);
  }

  private static void put(String oldName, String newId, Function<Either<Boolean, Integer>, Either<Boolean, Integer>> transform) {
    RENAMES.put(oldName, new Target(newId, transform));
  }

  static {
    put("allowEnteringNetherUsingPortals", "minecraft:allow_entering_nether_using_portals");
    put("announceAdvancements", "minecraft:show_advancement_messages");
    put("blockExplosionDropDecay", "minecraft:block_explosion_drop_decay");
    put("commandBlockOutput", "minecraft:command_block_output");
    put("enableCommandBlocks", "minecraft:command_blocks_work");
    put("commandBlocksEnabled", "minecraft:command_blocks_work");
    put("commandModificationBlockLimit", "minecraft:max_block_modifications");
    put("disableElytraMovementCheck", "minecraft:elytra_movement_check", INVERT);
    put("disablePlayerMovementCheck", "minecraft:player_movement_check", INVERT);
    put("disableRaids", "minecraft:raids", INVERT);
    put("doDaylightCycle", "minecraft:advance_time");
    put("doEntityDrops", "minecraft:entity_drops");
    put("doImmediateRespawn", "minecraft:immediate_respawn");
    put("doInsomnia", "minecraft:spawn_phantoms");
    put("doLimitedCrafting", "minecraft:limited_crafting");
    put("doMobLoot", "minecraft:mob_drops");
    put("doMobSpawning", "minecraft:spawn_mobs");
    put("doPatrolSpawning", "minecraft:spawn_patrols");
    put("doTileDrops", "minecraft:block_drops");
    put("doTraderSpawning", "minecraft:spawn_wandering_traders");
    put("doVinesSpread", "minecraft:spread_vines");
    put("doWardenSpawning", "minecraft:spawn_wardens");
    put("doWeatherCycle", "minecraft:advance_weather");
    put("drowningDamage", "minecraft:drowning_damage");
    put("enderPearlsVanishOnDeath", "minecraft:ender_pearls_vanish_on_death");
    put("fallDamage", "minecraft:fall_damage");
    put("fireDamage", "minecraft:fire_damage");
    put("forgiveDeadPlayers", "minecraft:forgive_dead_players");
    put("freezeDamage", "minecraft:freeze_damage");
    put("globalSoundEvents", "minecraft:global_sound_events");
    put("keepInventory", "minecraft:keep_inventory");
    put("lavaSourceConversion", "minecraft:lava_source_conversion");
    put("locatorBar", "minecraft:locator_bar");
    put("logAdminCommands", "minecraft:log_admin_commands");
    put("maxCommandChainLength", "minecraft:max_command_sequence_length");
    put("maxCommandForkCount", "minecraft:max_command_forks");
    put("maxEntityCramming", "minecraft:max_entity_cramming");
    put("minecartMaxSpeed", "minecraft:max_minecart_speed");
    put("mobExplosionDropDecay", "minecraft:mob_explosion_drop_decay");
    put("mobGriefing", "minecraft:mob_griefing");
    put("naturalRegeneration", "minecraft:natural_health_regeneration");
    put("playersNetherPortalCreativeDelay", "minecraft:players_nether_portal_creative_delay");
    put("playersNetherPortalDefaultDelay", "minecraft:players_nether_portal_default_delay");
    put("playersSleepingPercentage", "minecraft:players_sleeping_percentage");
    put("projectilesCanBreakBlocks", "minecraft:projectiles_can_break_blocks");
    put("pvp", "minecraft:pvp");
    put("randomTickSpeed", "minecraft:random_tick_speed");
    put("reducedDebugInfo", "minecraft:reduced_debug_info");
    put("sendCommandFeedback", "minecraft:send_command_feedback");
    put("showDeathMessages", "minecraft:show_death_messages");
    put("snowAccumulationHeight", "minecraft:max_snow_accumulation_height");
    put("spawnMonsters", "minecraft:spawn_monsters");
    put("spawnRadius", "minecraft:respawn_radius");
    put("spawnerBlocksEnabled", "minecraft:spawner_blocks_work");
    put("spectatorsGenerateChunks", "minecraft:spectators_generate_chunks");
    put("tntExplodes", "minecraft:tnt_explodes");
    put("tntExplosionDropDecay", "minecraft:tnt_explosion_drop_decay");
    put("universalAnger", "minecraft:universal_anger");
    put("waterSourceConversion", "minecraft:water_source_conversion");
    put("doFireTick", "minecraft:fire_spread_radius_around_player", FIRE_TICK);
    put("allowFireTicksAwayFromPlayer", "minecraft:fire_spread_radius_around_player", FIRE_AWAY);
  }

  /**
   * Copies {@code source} into {@code target}, remapping legacy keys. Returns {@code true} if any
   * entry was renamed or dropped (i.e. the caller should re-persist in the migrated form).
   */
  public static boolean migrate(Map<String, RuleHistory> source, Map<String, RuleHistory> target) {
    boolean changed = false;
    for (Map.Entry<String, RuleHistory> entry : source.entrySet()) {
      String key = entry.getKey();
      RuleHistory history = entry.getValue();

      // Already a namespaced id (new format, or a rule from another mod) -> keep untouched.
      if (key.indexOf(':') >= 0) {
        merge(target, key, history);
        continue;
      }

      Target target_ = RENAMES.get(key);
      if (target_ != null) {
        merge(target, target_.newId(), history.mapValues(target_.transform()));
        changed = true;
        continue;
      }

      if (REMOVED.contains(key)) {
        // Rule no longer exists; nothing to migrate to.
        changed = true;
        continue;
      }

      // Unrecognized legacy key -> preserve as-is rather than lose data.
      merge(target, key, history);
    }
    return changed;
  }

  private static void merge(Map<String, RuleHistory> target, String key, RuleHistory history) {
    target.merge(key, history, RuleHistory::mergedWith);
  }

  private LegacyRuleHistoryMigration() {
  }
}
