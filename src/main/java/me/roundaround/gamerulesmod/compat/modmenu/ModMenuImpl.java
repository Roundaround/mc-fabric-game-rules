package me.roundaround.gamerulesmod.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.roundaround.gamerulesmod.client.gui.screen.GameRuleScreen;
import net.minecraft.client.MinecraftClient;

public class ModMenuImpl implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return (screen) -> {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null || !isOp(client)) {
        return null;
      }
      return new GameRuleScreen(screen);
    };
  }

  private static boolean isOp(MinecraftClient client) {
    if (client.player == null) {
      return false;
    }
    return client.isInSingleplayer() || client.player.hasPermissionLevel(2);
  }
}
