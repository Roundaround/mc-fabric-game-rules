package me.roundaround.gamerulesmod.neoforge;

import me.roundaround.gamerulesmod.GameRules;
import me.roundaround.gamerulesmod.client.GameRulesClient;
import me.roundaround.trove.neoforge.TroveNeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("gamerulesmod")
public final class GameRulesNeoForgeMod {
  public GameRulesNeoForgeMod(IEventBus modBus, ModContainer container) {
    TroveNeoForge.bootstrap(modBus, container);
    GameRules.init();

    // game-rules has no HUD layer and no mod-config screen. Its screen is opened
    // via keybind only (the Trove KeyBindings bridge wired in bootstrap above),
    // so there is no IConfigScreenFactory extension point.
    if (FMLEnvironment.getDist() == Dist.CLIENT) {
      modBus.addListener(FMLClientSetupEvent.class, event -> GameRulesClient.initClient());
    }
  }
}
