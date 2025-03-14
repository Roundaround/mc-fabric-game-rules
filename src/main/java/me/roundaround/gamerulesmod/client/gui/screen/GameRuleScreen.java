package me.roundaround.gamerulesmod.client.gui.screen;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.client.gui.widget.GameRuleListWidget;
import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.gamerulesmod.roundalib.client.gui.GuiUtil;
import me.roundaround.gamerulesmod.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.gamerulesmod.util.RuleInfo;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;

public class GameRuleScreen extends Screen {
  private static final int BUTTON_WIDTH = ButtonWidget.DEFAULT_WIDTH_SMALL;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final HashMap<String, Either<Boolean, Integer>> dirtyValues = new HashMap<>();
  private final Screen parent;

  private CheckboxWidget checkbox;
  private GameRuleListWidget list;
  private ButtonWidget saveButton;

  public GameRuleScreen(Screen parent) {
    super(Text.translatable("gamerulesmod.main.title"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    assert this.client != null;
    assert this.client.world != null;

    this.list = this.layout.addBody(new GameRuleListWidget(this.client, this.layout, this::onRulesResponse, this::onRuleChange));

    this.layout.setHeaderHeight(this.layout.getHeaderHeight() + 2 * GuiUtil.PADDING);
    this.layout.addHeader(this.textRenderer, this.title);

    this.checkbox = this.layout.addHeader(CheckboxWidget.builder(
            Text.translatable("gamerulesmod.main.showImmutable"),
            this.textRenderer
        )
        .callback((checkbox, checked) -> this.list.setShowImmutable(checked))
        .checked(this.list.isShowingImmutable())
        .build());

    this.saveButton = this.layout.addFooter(ButtonWidget.builder(
        ScreenTexts.DONE, (button) -> {
          if (this.dirtyValues.isEmpty()) {
            return;
          }

          this.client.setScreen(new ConfirmScreen(
              this::onConfirmChoice,
              Text.translatable("gamerulesmod.confirm.title"),
              this.generateConfirmText()
          ));
        }
    ).width(BUTTON_WIDTH).build());

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> this.close())
        .width(BUTTON_WIDTH)
        .build());

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
    if (this.list != null) {
      this.list.close();
    }
    if (this.client != null) {
      this.client.setScreen(this.parent);
    }
  }

  private void onRulesResponse(List<RuleInfo> rules) {
    if (this.client == null || this.client.world == null) {
      return;
    }

    long mutable = rules.stream().filter((rule) -> rule.state().equals(RuleInfo.State.MUTABLE)).count();
    this.checkbox.active = mutable < this.client.world.getGameRules().gamerulesmod$size();
  }

  private void onRuleChange(boolean allValid, boolean anyDirty) {
    this.dirtyValues.clear();

    this.saveButton.active = allValid && anyDirty;
    if (this.saveButton.active) {
      this.dirtyValues.putAll(this.list.getDirtyValues());
    }
  }

  private void onConfirmChoice(boolean confirmed) {
    if (confirmed) {
      ClientNetworking.sendSet(this.list.getDirtyValues());
      this.close();
    } else {
      assert this.client != null;
      this.client.setScreen(this);
    }
  }

  private Text generateConfirmText() {
    MutableText text = Text.empty();

    assert this.client != null;
    ClientWorld world = this.client.world;
    if (world != null && world.getLevelProperties().isHardcore()) {
      text.append(Text.translatable("gamerulesmod.confirm.hardcore")).append("\n\n");
    }

    text.append(Text.translatable("gamerulesmod.confirm.summary")).append("\n");
    this.dirtyValues.forEach((id, value) -> text.append(id)
        .append(": ")
        .append(value.map(Object::toString, Object::toString))
        .append("\n"));

    return text;
  }
}
