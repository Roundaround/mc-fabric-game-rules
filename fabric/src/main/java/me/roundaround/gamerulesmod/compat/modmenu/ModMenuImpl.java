package me.roundaround.gamerulesmod.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.roundaround.allay.api.Entrypoint;
import me.roundaround.gamerulesmod.client.gui.screen.GameRuleScreen;
import me.roundaround.gamerulesmod.client.gui.screen.UnavailableScreen;
import me.roundaround.gamerulesmod.network.Networking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

@Entrypoint(Entrypoint.MOD_MENU)
public class ModMenuImpl implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return (screen) -> {
      Minecraft client = Minecraft.getInstance();
      if (client == null || client.level == null || client.player == null) {
        return new UnavailableScreen(
            screen, List.of(
            Component.translatable("gamerulesmod.unavailable.notInWorld.1"),
            Component.translatable("gamerulesmod.unavailable.notInWorld.2")
        )
        );
      }
      if (!ClientPlayNetworking.canSend(Networking.FetchC2S.ID)) {
        return new UnavailableScreen(
            screen, List.of(
            Component.translatable("gamerulesmod.unavailable.missingOnServer.1"),
            Component.translatable("gamerulesmod.unavailable.missingOnServer.2")
        )
        );
      }
      return new GameRuleScreen(screen);
    };
  }
}
