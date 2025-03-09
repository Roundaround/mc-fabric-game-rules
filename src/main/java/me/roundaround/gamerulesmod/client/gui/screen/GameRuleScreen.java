package me.roundaround.gamerulesmod.client.gui.screen;

import me.roundaround.gamerulesmod.client.gui.widget.GameRuleListWidget;
import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.gamerulesmod.roundalib.client.gui.GuiUtil;
import me.roundaround.gamerulesmod.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
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

    this.layout.setHeaderHeight(this.layout.getHeaderHeight() + 2 * GuiUtil.PADDING);
    this.layout.addHeader(this.textRenderer, this.title);
    this.layout.addHeader(CheckboxWidget.builder(Text.translatable("gamerulesmod.main.mutable"), this.textRenderer)
        .callback((checkbox, checked) -> this.list.fetch(checked))
        .checked(false)
        .build());

    this.list = this.layout.addBody(new GameRuleListWidget(this.client, this.layout));

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::save).width(BUTTON_WIDTH).build());
    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.CANCEL, this::cancel).width(BUTTON_WIDTH).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();

    this.list.fetch(true);
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
      this.list.close();
    }
    this.client.setScreen(this.parent);
  }

  private void save(ButtonWidget button) {
    if (this.list != null) {
      ClientNetworking.sendSet(this.list.getDirtyValues());
      this.list.close();
    }
    this.close();
  }

  private void cancel(ButtonWidget button) {
    this.close();
  }
}
