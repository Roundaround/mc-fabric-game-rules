package me.roundaround.gamerulesmod.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.roundaround.gamerulesmod.client.gui.screen.GameRuleScreen;
import me.roundaround.gamerulesmod.client.gui.screen.UnavailableScreen;
import me.roundaround.gamerulesmod.network.Networking;
import me.roundaround.gradle.api.annotation.Entrypoint;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

@Entrypoint(Entrypoint.MOD_MENU)
public class ModMenuImpl implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return (screen) -> {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null || client.player == null) {
        return new UnavailableScreen(
            screen, List.of(
            Text.translatable("gamerulesmod.unavailable.notInWorld.1"),
            Text.translatable("gamerulesmod.unavailable.notInWorld.2")
        )
        );
      }
      if (!ClientPlayNetworking.canSend(Networking.FetchC2S.ID)) {
        return new UnavailableScreen(
            screen, List.of(
            Text.translatable("gamerulesmod.unavailable.missingOnServer.1"),
            Text.translatable("gamerulesmod.unavailable.missingOnServer.2")
        )
        );
      }
      return new GameRuleScreen(screen);
    };
  }
}
