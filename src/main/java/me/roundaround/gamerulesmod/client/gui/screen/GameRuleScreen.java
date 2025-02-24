package me.roundaround.gamerulesmod.client.gui.screen;

import me.roundaround.gamerulesmod.client.gui.widget.GameRuleListWidget;
import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class GameRuleScreen extends Screen {
  private static final int BUTTON_WIDTH = ButtonWidget.DEFAULT_WIDTH_SMALL;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  private GameRuleListWidget list;

  public GameRuleScreen(Screen parent) {
    super(Text.translatable("gamerulesmod.main.title"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    assert this.client != null;
    assert this.client.world != null;

    this.layout.addHeader(this.textRenderer, this.title);

    this.list = this.layout.addBody(new GameRuleListWidget(this.client, this.layout));

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, (b) -> this.close()).width(BUTTON_WIDTH).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();

    this.list.fetch();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    if (this.client == null) {
      return;
    }
    if (this.list != null) {
      ClientNetworking.sendSet(this.list.getDirtyValues());
    }
    this.client.setScreen(this.parent);
  }
}
