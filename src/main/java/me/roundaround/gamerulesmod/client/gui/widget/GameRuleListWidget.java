package me.roundaround.gamerulesmod.client.gui.widget;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.NonPositioningLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.TooltipWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.world.GameRules;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class GameRuleListWidget extends ParentElementEntryListWidget<GameRuleListWidget.Entry> {
  public GameRuleListWidget(MinecraftClient client, ThreeSectionLayoutWidget layout) {
    super(client, layout);
  }

  public void fetch() {
    this.clearEntries();
    this.addEntry(GameRuleListWidget.LoadingEntry.factory(client.textRenderer));
    this.refreshPositions();

    ClientNetworking.sendFetch(List.of(GameRules.DO_VINES_SPREAD.getName(),
        GameRules.DO_FIRE_TICK.getName(),
        GameRules.PLAYERS_SLEEPING_PERCENTAGE.getName()
    )).orTimeout(30, TimeUnit.SECONDS).whenCompleteAsync((gameRules, throwable) -> {
      if (throwable != null) {
        this.setError();
      } else {
        this.setRules(gameRules);
      }
    }, client);
  }

  private void setError() {
    this.clearEntries();
    this.addEntry(ErrorEntry.factory(this.client.textRenderer));
    this.refreshPositions();
  }

  private void setRules(GameRules gameRules) {
    this.clearEntries();
    this.addEntry(BooleanRuleEntry.factory(gameRules, GameRules.DO_VINES_SPREAD, this.client.textRenderer));
    this.addEntry(BooleanRuleEntry.factory(gameRules, GameRules.DO_FIRE_TICK, this.client.textRenderer));
    this.addEntry(IntRuleEntry.factory(gameRules, GameRules.PLAYERS_SLEEPING_PERCENTAGE, this.client.textRenderer));
    this.refreshPositions();
  }

  public Map<String, Either<Boolean, Integer>> getDirtyValues() {
    HashMap<String, Either<Boolean, Integer>> values = new HashMap<>();
    for (Entry entry : this.entries) {
      if (entry instanceof RuleEntry ruleEntry && ruleEntry.isDirty()) {
        values.put(ruleEntry.getId(), ruleEntry.getValue());
      }
    }
    return values;
  }

  public static class RuleContext {
    private final String id;
    private final Text name;
    private final List<Text> tooltip;
    private final String narrationName;
    private final Either<Boolean, Integer> initialValue;

    private Either<Boolean, Integer> value;

    private RuleContext(
        String id, Text name, List<Text> tooltip, String narrationName, Either<Boolean, Integer> initialValue
    ) {
      this.id = id;
      this.name = name;
      this.tooltip = tooltip;
      this.narrationName = narrationName;
      this.initialValue = initialValue;

      this.value = initialValue;
    }

    private static <T extends GameRules.Rule<T>> RuleContext of(
        GameRules gameRules, GameRules.Key<T> key
    ) {
      T rule = gameRules.get(key);
      return new RuleContext(key.getName(),
          getName(key),
          getTooltip(key, rule),
          getNarrationName(key, rule),
          gameRules.gamerulesmod$getValue(key.getName())
      );
    }

    public String getId() {
      return this.id;
    }

    public Text getName() {
      return this.name;
    }

    public List<Text> getTooltip() {
      return this.tooltip;
    }

    public String getNarrationName() {
      return this.narrationName;
    }

    public Either<Boolean, Integer> getValue() {
      return this.value;
    }

    public void setValue(boolean value) {
      this.value = Either.left(value);
    }

    public void setValue(int value) {
      this.value = Either.right(value);
    }

    public boolean isDirty() {
      return !Objects.equals(this.initialValue, this.value);
    }

    private static <T extends GameRules.Rule<T>> Text getName(GameRules.Key<T> key) {
      return Text.translatable(key.getTranslationKey());
    }

    private static <T extends GameRules.Rule<T>> List<Text> getTooltip(GameRules.Key<T> key, T rule) {
      ArrayList<Text> lines = new ArrayList<>();
      lines.add(Text.literal(key.getName()).formatted(Formatting.YELLOW));
      getDescription(key).ifPresent(lines::add);
      lines.add(getDefaultLine(rule));
      return lines;
    }

    private static <T extends GameRules.Rule<T>> Optional<Text> getDescription(GameRules.Key<T> key) {
      String descriptionI18nKey = key.getTranslationKey() + ".description";
      if (I18n.hasTranslation(descriptionI18nKey)) {
        return Optional.of(Text.translatable(descriptionI18nKey));
      }
      return Optional.empty();
    }

    private static <T extends GameRules.Rule<T>> Text getDefaultLine(T rule) {
      return Text.translatable("editGamerule.default", Text.literal(rule.serialize())).formatted(Formatting.GRAY);
    }

    private static <T extends GameRules.Rule<T>> String getNarrationName(GameRules.Key<T> key, T rule) {
      String defaultLine = getDefaultLine(rule).getString();
      return getDescription(key).map((description) -> description.getString() + "\n" + defaultLine).orElse(defaultLine);
    }
  }

  public abstract static class Entry extends ParentElementEntryListWidget.Entry {
    protected static final int HEIGHT = 20;

    protected final TextRenderer textRenderer;

    protected Entry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(index, left, top, width, HEIGHT);

      this.textRenderer = textRenderer;
    }
  }

  public static class LoadingEntry extends GameRuleListWidget.Entry {
    private static final Text LOADING_TEXT = Text.translatable("gamerulesmod.main.loading");

    private final LabelWidget spinner;

    public LoadingEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(textRenderer, index, left, top, width);

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
          .mainAxisContentAlignCenter()
          .defaultOffAxisContentAlignCenter();

      LabelWidget label = layout.add(LabelWidget.builder(textRenderer, LOADING_TEXT)
          .showShadow()
          .hideBackground()
          .build());

      this.spinner = layout.add(LabelWidget.builder(textRenderer, getSpinnerText())
          .color(Colors.GRAY)
          .showShadow()
          .hideBackground()
          .build());

      this.addLayout(layout,
          (self) -> self.setPositionAndDimensions(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
      );

      this.addDrawableChild(label);
      this.addDrawable(this.spinner);
    }

    private Text getSpinnerText() {
      return Text.of(LoadingDisplay.get(Util.getMeasuringTimeMs()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.spinner.setText(this.getSpinnerText());
    }

    public static FlowListWidget.EntryFactory<GameRuleListWidget.LoadingEntry> factory(TextRenderer textRenderer) {
      return (index, left, top, width) -> new GameRuleListWidget.LoadingEntry(textRenderer, index, left, top, width);
    }
  }

  public static class ErrorEntry extends GameRuleListWidget.Entry {
    private static final Text ERROR_TEXT_1 = Text.translatable("gamerulesmod.main.error1");
    private static final Text ERROR_TEXT_2 = Text.translatable("gamerulesmod.main.error2");

    public ErrorEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(textRenderer, index, left, top, width);

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(this.getContentLeft(),
          this.getContentTop(),
          this.getContentWidth(),
          this.getContentHeight()
      );

      layout.add(LabelWidget.builder(textRenderer, List.of(ERROR_TEXT_1, ERROR_TEXT_2))
          .color(Colors.RED)
          .alignTextCenterX()
          .showShadow()
          .hideBackground()
          .build());

      this.addLayout(layout,
          (self) -> self.setPositionAndDimensions(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
      );

      layout.forEachChild(this::addDrawableChild);
    }

    public static FlowListWidget.EntryFactory<GameRuleListWidget.ErrorEntry> factory(TextRenderer textRenderer) {
      return (index, left, top, width) -> new GameRuleListWidget.ErrorEntry(textRenderer, index, left, top, width);
    }
  }

  public abstract static class RuleEntry extends GameRuleListWidget.Entry {
    protected static final int CONTROL_MIN_WIDTH = 100;

    protected final RuleContext context;

    protected RuleEntry(
        RuleContext context, TextRenderer textRenderer, int index, int left, int top, int width
    ) {
      super(textRenderer, index, left, top, width);

      this.context = context;

      this.createAndAddTooltip();
      this.createAndAddContent();
    }

    protected abstract ClickableWidget createControlWidget();

    public boolean isDirty() {
      return this.context.isDirty();
    }

    public String getId() {
      return this.context.getId();
    }

    public Either<Boolean, Integer> getValue() {
      return this.context.getValue();
    }

    private void createAndAddTooltip() {
      NonPositioningLayoutWidget layout = new NonPositioningLayoutWidget(this.getContentLeft(),
          this.getContentTop(),
          this.getContentWidth(),
          this.getContentHeight()
      );
      TooltipWidget tooltip = layout.add(new TooltipWidget(this.context.getTooltip()),
          (parent, self) -> self.setDimensionsAndPosition(parent.getWidth(),
              parent.getHeight(),
              parent.getX(),
              parent.getY()
          )
      );
      this.addLayout(layout,
          (self) -> self.setPositionAndDimensions(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
      );
      this.addDrawable(tooltip);
    }

    protected void createAndAddContent() {
      LinearLayoutWidget layout = LinearLayoutWidget.horizontal(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
          .spacing(GuiUtil.PADDING)
          .defaultOffAxisContentAlignCenter();

      layout.add(LabelWidget.builder(this.textRenderer, this.context.getName())
          .alignTextLeft()
          .alignTextCenterY()
          .overflowBehavior(LabelWidget.OverflowBehavior.WRAP)
          .maxLines(2)
          .showShadow()
          .hideBackground()
          .build(), (parent, self) -> self.setDimensions(this.getLabelWidth(parent), this.getContentHeight()));

      layout.add(this.createControlWidget(),
          (parent, self) -> self.setDimensions(this.getControlWidth(parent), this.getContentHeight())
      );

      this.addLayout(layout,
          (self) -> self.setPositionAndDimensions(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
      );
      layout.forEachChild(this::addDrawableChild);
    }

    protected int getLabelWidth(LinearLayoutWidget layout) {
      return layout.getWidth() - 2 * layout.getSpacing() - this.getControlWidth(layout) - IconButtonWidget.SIZE_V;
    }

    protected int getControlWidth(LinearLayoutWidget layout) {
      return Math.max(CONTROL_MIN_WIDTH, Math.round(layout.getWidth() * 0.3f));
    }
  }

  public static class BooleanRuleEntry extends GameRuleListWidget.RuleEntry {
    protected BooleanRuleEntry(
        RuleContext context, TextRenderer textRenderer, int index, int left, int top, int width
    ) {
      super(context, textRenderer, index, left, top, width);
    }

    public Boolean getBooleanValue() {
      return this.getValue().left().orElseThrow();
    }

    @Override
    protected ClickableWidget createControlWidget() {
      return CyclingButtonWidget.onOffBuilder(this.getBooleanValue())
          .omitKeyText()
          .narration((button) -> button.getGenericNarrationMessage()
              .append("\n")
              .append(this.context.getNarrationName()))
          .build(0, 0, 1, 1, this.context.getName(), (button, value) -> this.context.setValue(value));
    }

    public static FlowListWidget.EntryFactory<GameRuleListWidget.BooleanRuleEntry> factory(
        GameRules gameRules, GameRules.Key<GameRules.BooleanRule> key, TextRenderer textRenderer
    ) {
      return (index, left, top, width) -> new GameRuleListWidget.BooleanRuleEntry(RuleContext.of(gameRules, key),
          textRenderer,
          index,
          left,
          top,
          width
      );
    }
  }

  public static class IntRuleEntry extends GameRuleListWidget.RuleEntry {
    protected IntRuleEntry(
        RuleContext context, TextRenderer textRenderer, int index, int left, int top, int width
    ) {
      super(context, textRenderer, index, left, top, width);
    }

    public Integer getIntValue() {
      return this.getValue().right().orElseThrow();
    }

    @Override
    protected ClickableWidget createControlWidget() {
      return new ClickableWidget(0, 0, 1, 1, Text.of("Hello world")) {
        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
          TextRenderer textRenderer = GameRuleListWidget.IntRuleEntry.this.textRenderer;
          Text value = Text.of(GameRuleListWidget.IntRuleEntry.this.getIntValue().toString());
          int x = this.getX() + this.getWidth() / 2;
          int y = this.getY() + (this.getHeight() - textRenderer.fontHeight) / 2;
          context.drawCenteredTextWithShadow(textRenderer, value, x, y, GuiUtil.LABEL_COLOR);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
          builder.put(NarrationPart.TITLE, this.getMessage());
        }
      };
    }

    public static FlowListWidget.EntryFactory<GameRuleListWidget.IntRuleEntry> factory(
        GameRules gameRules, GameRules.Key<GameRules.IntRule> key, TextRenderer textRenderer
    ) {
      return (index, left, top, width) -> new GameRuleListWidget.IntRuleEntry(RuleContext.of(gameRules, key),
          textRenderer,
          index,
          left,
          top,
          width
      );
    }
  }
}
