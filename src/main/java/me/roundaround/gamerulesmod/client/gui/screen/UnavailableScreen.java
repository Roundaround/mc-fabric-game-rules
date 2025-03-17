package me.roundaround.gamerulesmod.client.gui.screen;

import me.roundaround.gamerulesmod.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.gamerulesmod.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.List;

public class UnavailableScreen extends Screen {
  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;
  private final List<Text> info;

  public UnavailableScreen(Screen parent, List<Text> info) {
    super(Text.translatable("gamerulesmod.unavailable.title"));
    this.parent = parent;
    this.info = info;
  }

  @Override
  protected void init() {
    assert this.client != null;

    this.layout.addHeader(this.textRenderer, this.title);

    this.layout.addBody(LabelWidget.builder(this.textRenderer, this.info)
        .alignTextCenterX()
        .alignTextCenterY()
        .hideBackground()
        .showShadow()
        .lineSpacing(2)
        .build());

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.BACK, (b) -> this.close())
        .width(ButtonWidget.field_49479)
        .build());

    this.layout.forEachChild(this::addDrawableChild);
    this.refreshWidgetPositions();
  }

  @Override
  protected void refreshWidgetPositions() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    assert this.client != null;
    this.client.setScreen(this.parent);
  }
}
