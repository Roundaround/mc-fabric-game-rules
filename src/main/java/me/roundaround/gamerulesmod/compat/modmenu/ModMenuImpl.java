package me.roundaround.gamerulesmod.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.roundaround.gamerulesmod.client.gui.screen.GameRuleScreen;
import me.roundaround.roundalib.gradle.api.annotation.Entrypoint;
import net.minecraft.client.MinecraftClient;

@Entrypoint(Entrypoint.MOD_MENU)
public class ModMenuImpl implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return (screen) -> {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null || client.player == null) {
        return null;
      }
      return new GameRuleScreen(screen);
    };
  }
}
