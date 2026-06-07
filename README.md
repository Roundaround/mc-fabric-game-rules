![Game Rules](https://imgur.com/yaCr6O5.png)

![](https://img.shields.io/badge/Loader-Fabric-313e51?style=for-the-badge)
![](https://img.shields.io/badge/MC-1.21--1.21.10-313e51?style=for-the-badge)
![](https://img.shields.io/badge/Side-Client%20&%20Server-313e51?style=for-the-badge)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/game-rules?style=flat&logo=modrinth&color=00AF5C)](https://modrinth.com/mod/game-rules)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1292156?style=flat&logo=curseforge&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/game-rules)
[![GitHub Repo stars](https://img.shields.io/github/stars/Roundaround/mc-fabric-game-rules?style=flat&logo=github)](https://github.com/Roundaround/mc-fabric-game-rules)

[![Support me on Ko-fi](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/donate/kofi-singular-alt_vector.svg)](https://ko-fi.com/roundaround)

---

Modify game rules in your existing worlds, all without enabling cheats/commands! Comes in multiple variants that control which game rules are modifiable. Open the game rule edit screen through Mod Menu or with the special keybinding created for the mod (by default not bound to any key).

![Edit Game Rules screen](https://cdn.modrinth.com/data/cached_images/955659465c109c318cd5d31615ec317d05d02b26_0.webp)

The list of game rules you can edit will depend on the mod variant installed and the environment you're trying to work in. Each variant (base, technical, and hardcore) has its own list of what rules can be edited.

For multiplayer servers, the rules you can edit through the mod are limited to those defined by the variant of mod installed server-side, and requires OP permissions to use.

The same lists exist for single player, though with minor changes depending on whether you have cheats enabled in that world. If you do, then the game rules available for editing are laxed "one level", i.e. the hardcore variant will show the rules for technical, and technical will show all rules similar to the base variant.

Finally an extra restriction exists for hardcore worlds - game rules in hardcore worlds _regardless of mod variant_ can only be changed once! This was added as a balancing mechanism to allow long-running hardcore worlds to change the settings like vine spreading (which may not have been an option years ago at world creation) without feeling too cheaty. The mod will show you the changed date and describe the exact time and date you changed and of the rules in your world, so that other folks might be able to hold you accountable!

The rules and lists are still under evaluation (especially in relation to multiplayer servers and whether cheats are enabled), so if you have any thoughts or questions, feel free to open an issue on the mod's GitHub repo ([https://github.com/Roundaround/mc-fabric-game-rules/issues](https://github.com/Roundaround/mc-fabric-game-rules/issues)).

## Variants

With the base variant of the mod, all game rules will be available for edit in both single player worlds and multiplayer servers (as long as you have OP).

### Technical

The technical variant restricts the list a bit by omitting some of the more "cheaty" rules, while keeping around some that might be useful for more advanced users.

- allowEnteringNetherUsingPortals (1.21.9+)
- allowFireTicksAwayFromPlayer (1.21.5+)
- announceAdvancements
- commandBlocksEnabled (1.21.9+)
- commandBlockOutput
- commandModificationBlockLimit
- disableElytraMovementCheck
- doFireTick
- doImmediateRespawn
- doLimitedCrafting
- doVinesSpread
- globalSoundEvents
- locatorBar (1.21.6+)
- logAdminCommands
- maxCommandChainLength
- maxCommandForkCount
- mobGriefing
- playersNetherPortalCreativeDelay
- playersSleepingPercentage
- pvp (1.21.9+)
- reducedDebugInfo
- sendCommandFeedback
- showDeathMessages
- spawnChunkRadius (<1.21.9)
- spawnMonsters (1.21.9+)
- spawnerBlocksEnabled (1.21.9+)
- tntExplodes (1.21.5+)

### Hardcore

The hardcore variant goes even further, restricting the list down to just a few that are often considered not cheating (especially for particularly long-running hardcore worlds) because the game rules were introduced in more recent versions of the game and it is commonplace to modify them for new worlds. However, the super restrictive list in the hardcore mod variant only applies to single player worlds with cheats disabled and multiplayer servers. In single player worlds where you have cheats enabled, all the game rules in the technical variant will also be available to you.

- allowFireTicksAwayFromPlayer (1.21.5+)
- doFireTick
- doVinesSpread
- locatorBar (1.21.6+)
- mobGriefing
- pvp (1.21.9+)
- spawnChunkRadius (<1.21.9)
