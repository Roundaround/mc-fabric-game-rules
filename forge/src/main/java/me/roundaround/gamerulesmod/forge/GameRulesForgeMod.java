package me.roundaround.gamerulesmod.forge;

import me.roundaround.gamerulesmod.GameRules;
import me.roundaround.gamerulesmod.client.GameRulesClient;
import me.roundaround.trove.forge.TroveForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("gamerulesmod")
public final class GameRulesForgeMod {
  public GameRulesForgeMod(FMLJavaModLoadingContext context) {
    TroveForge.bootstrap(context);
    GameRules.init();

    // game-rules has no HUD layer and no mod-config screen. Its screen is opened
    // via keybind only (the Trove KeyBindings bridge wired in bootstrap above),
    // so there is no ConfigScreenHandler extension point. The client-only setup
    // listener is isolated behind a dist check so a dedicated server never links
    // the client-only initClient path.
    if (FMLEnvironment.dist == Dist.CLIENT) {
      FMLClientSetupEvent.getBus(context.getModBusGroup())
          .addListener(event -> GameRulesClient.initClient());
    }
  }
}
