package me.roundaround.gamerulesmod.client.option;

import me.roundaround.gamerulesmod.client.gui.screen.GameRuleScreen;
import me.roundaround.gamerulesmod.roundalib.client.event.MinecraftClientEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
  public static KeyBinding openEditScreen;

  public static void register() {
    openEditScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "gamerulesmod.key.openEditScreen",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        "gamerulesmod.key.category"
    ));

    MinecraftClientEvents.HANDLE_INPUT.register((client) -> {
      while (openEditScreen.wasPressed()) {
        client.setScreen(new GameRuleScreen(client.currentScreen));
      }
    });
  }

  private KeyBindings() {
  }
}
