package me.roundaround.gamerulesmod.client.gui.widget;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.gamerulesmod.util.CancelHandle;
import me.roundaround.gamerulesmod.util.RuleInfo;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GameRuleListWidget extends ParentElementEntryListWidget<GameRuleListWidget.Entry> implements AutoCloseable {
  private CancelHandle cancelHandle;

  public GameRuleListWidget(MinecraftClient client, ThreeSectionLayoutWidget layout) {
    super(client, layout);
  }

  public void fetch(boolean mutableOnly) {
    this.cancel();

    this.clearEntries();
    this.addEntry(LoadingEntry.factory(this.client.textRenderer));
    this.refreshPositions();

    CompletableFuture<List<RuleInfo>> future = ClientNetworking.sendFetch(mutableOnly);
    this.cancelHandle = CancelHandle.of(future);

    future.orTimeout(30, TimeUnit.SECONDS).whenCompleteAsync((rules, throwable) -> {
      if (throwable != null) {
        this.setError();
      } else {
        this.setRules(rules, mutableOnly);
      }
    }, this.client);
  }

  private void cancel() {
    if (this.cancelHandle != null) {
      this.cancelHandle.cancel();
      this.cancelHandle = null;
    }
  }

  private void setError() {
    this.clearEntries();
    this.addEntry(ErrorEntry.factory(this.client.textRenderer));
    this.refreshPositions();
  }

  private void setRules(final List<RuleInfo> rules, final boolean mutableOnly) {
    this.clearEntries();

    final TextRenderer textRenderer = this.client.textRenderer;

    final GameRules gameRules = new GameRules();
    final HashMap<String, Boolean> mutability = new HashMap<>();

    rules.forEach((ruleInfo) -> {
      ruleInfo.applyValue(gameRules);
      mutability.put(ruleInfo.id(), ruleInfo.mutable());
    });

    final HashMap<GameRules.Category, HashMap<GameRules.Key<?>, FlowListWidget.EntryFactory<? extends RuleEntry>>> ruleEntries = new HashMap<>();

    GameRules.accept(new GameRules.Visitor() {
      @Override
      public void visitBoolean(GameRules.Key<GameRules.BooleanRule> key, GameRules.Type<GameRules.BooleanRule> type) {
        boolean mutable = mutability.getOrDefault(key.getName(), false);
        if (!mutableOnly || mutable) {
          this.addEntry(key, BooleanRuleEntry.factory(gameRules, key, mutable, textRenderer));
        }
      }

      @Override
      public void visitInt(GameRules.Key<GameRules.IntRule> key, GameRules.Type<GameRules.IntRule> type) {
        boolean mutable = mutability.getOrDefault(key.getName(), false);
        if (!mutableOnly || mutable) {
          this.addEntry(key, IntRuleEntry.factory(gameRules, key, mutable, textRenderer));
        }
      }

      private void addEntry(GameRules.Key<?> key, FlowListWidget.EntryFactory<? extends RuleEntry> factory) {
        ruleEntries.computeIfAbsent(key.getCategory(), (category) -> new HashMap<>()).put(key, factory);
      }
    });

    if (ruleEntries.isEmpty()) {
      this.addEntry(EmptyEntry.factory(textRenderer));
    }

    ruleEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach((categoryEntry) -> {
      this.addEntry(CategoryEntry.factory(Text.translatable(categoryEntry.getKey().getCategory()), textRenderer));

      categoryEntry.getValue()
          .entrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRules.Key::getName)))
          .forEach((ruleEntry) -> this.addEntry(ruleEntry.getValue()));
    });

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

  @Override
  public void close() {
    this.cancel();
  }

  public static class RuleContext {
    private final String id;
    private final Text name;
    private final List<Text> tooltip;
    private final String narrationName;
    private final Either<Boolean, Integer> initialValue;
    private final boolean mutable;

    private Either<Boolean, Integer> value;

    private RuleContext(
        String id,
        Text name,
        List<Text> tooltip,
        String narrationName,
        Either<Boolean, Integer> initialValue,
        boolean mutable
    ) {
      this.id = id;
      this.name = name;
      this.tooltip = tooltip;
      this.narrationName = narrationName;
      this.initialValue = initialValue;
      this.mutable = mutable;

      this.value = initialValue;
    }

    public static <T extends GameRules.Rule<T>> RuleContext of(
        GameRules gameRules, GameRules.Key<T> key, boolean mutable
    ) {
      T rule = gameRules.get(key);
      return new RuleContext(key.getName(),
          getName(key),
          getTooltip(key, rule),
          getNarrationName(key, rule),
          gameRules.gamerulesmod$getValue(key.getName()),
          mutable
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
      if (!this.mutable) {
        return;
      }
      this.value = Either.left(value);
    }

    public void setValue(int value) {
      if (!this.mutable) {
        return;
      }
      this.value = Either.right(value);
    }

    public boolean isDirty() {
      return !Objects.equals(this.initialValue, this.value);
    }

    public boolean isMutable() {
      return this.mutable;
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
      this(textRenderer, index, left, top, width, HEIGHT);
    }

    protected Entry(TextRenderer textRenderer, int index, int left, int top, int width, int height) {
      super(index, left, top, width, height);
      this.textRenderer = textRenderer;
    }
  }

  public static class LoadingEntry extends Entry {
    private static final Text LOADING_TEXT = Text.translatable("gamerulesmod.main.loading");

    private final long createTime;
    private final LabelWidget spinner;

    public LoadingEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(textRenderer, index, left, top, width, 36);

      this.createTime = Util.getMeasuringTimeMs();

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
      if (Util.getMeasuringTimeMs() - this.createTime < 100) {
        // Prevent flashing by holding off on rendering anything until after 100ms
        return;
      }

      super.render(context, mouseX, mouseY, delta);
      this.spinner.setText(this.getSpinnerText());
    }

    public static FlowListWidget.EntryFactory<LoadingEntry> factory(TextRenderer textRenderer) {
      return (index, left, top, width) -> new LoadingEntry(textRenderer, index, left, top, width);
    }
  }

  public static class ErrorEntry extends Entry {
    private static final Text ERROR_TEXT_1 = Text.translatable("gamerulesmod.main.error1");
    private static final Text ERROR_TEXT_2 = Text.translatable("gamerulesmod.main.error2");

    public ErrorEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(textRenderer, index, left, top, width, 36);

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
          .mainAxisContentAlignCenter()
          .defaultOffAxisContentAlignCenter();

      layout.add(LabelWidget.builder(textRenderer, List.of(ERROR_TEXT_1, ERROR_TEXT_2))
          .color(Colors.RED)
          .alignTextCenterX()
          .alignTextCenterY()
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

    public static FlowListWidget.EntryFactory<ErrorEntry> factory(TextRenderer textRenderer) {
      return (index, left, top, width) -> new ErrorEntry(textRenderer, index, left, top, width);
    }
  }

  public static class EmptyEntry extends Entry {
    private static final Text TEXT = Text.translatable("gamerulesmod.main.none").formatted(Formatting.ITALIC);

    public EmptyEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(textRenderer, index, left, top, width, 36);

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
          .mainAxisContentAlignCenter()
          .defaultOffAxisContentAlignCenter();

      layout.add(LabelWidget.builder(textRenderer, TEXT)
          .alignTextCenterX()
          .alignTextCenterY()
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
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

    public static FlowListWidget.EntryFactory<EmptyEntry> factory(TextRenderer textRenderer) {
      return (index, left, top, width) -> new EmptyEntry(textRenderer, index, left, top, width);
    }
  }

  public static class CategoryEntry extends Entry {
    public CategoryEntry(Text text, TextRenderer textRenderer, int index, int left, int top, int width) {
      super(textRenderer, index, left, top, width);

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
          .mainAxisContentAlignCenter()
          .defaultOffAxisContentAlignCenter();

      layout.add(LabelWidget.builder(textRenderer, text.copy().formatted(Formatting.BOLD, Formatting.YELLOW))
          .alignTextCenterX()
          .alignTextCenterY()
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
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

    @Override
    protected void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      renderRowShade(context,
          this.getContentLeft(),
          this.getContentTop(),
          this.getContentRight(),
          this.getContentBottom(),
          DEFAULT_SHADE_FADE_WIDTH,
          DEFAULT_SHADE_STRENGTH
      );
    }

    public static FlowListWidget.EntryFactory<CategoryEntry> factory(Text text, TextRenderer textRenderer) {
      return (index, left, top, width) -> new CategoryEntry(text, textRenderer, index, left, top, width);
    }
  }

  public abstract static class RuleEntry extends Entry {
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

  public static class BooleanRuleEntry extends RuleEntry {
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
      var result = CyclingButtonWidget.onOffBuilder(this.getBooleanValue())
          .omitKeyText()
          .narration((button) -> button.getGenericNarrationMessage()
              .append("\n")
              .append(this.context.getNarrationName()))
          .build(0, 0, 1, 1, this.context.getName(), (button, value) -> this.context.setValue(value));
      result.active = this.context.isMutable();
      return result;
    }

    public static FlowListWidget.EntryFactory<BooleanRuleEntry> factory(
        GameRules gameRules, GameRules.Key<GameRules.BooleanRule> key, boolean mutable, TextRenderer textRenderer
    ) {
      return (index, left, top, width) -> new BooleanRuleEntry(RuleContext.of(gameRules, key, mutable),
          textRenderer,
          index,
          left,
          top,
          width
      );
    }
  }

  public static class IntRuleEntry extends RuleEntry {
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
          TextRenderer textRenderer = IntRuleEntry.this.textRenderer;
          Text value = Text.of(IntRuleEntry.this.getIntValue().toString());
          int x = this.getX() + this.getWidth() / 2;
          int y = this.getY() + (this.getHeight() - textRenderer.fontHeight) / 2;
          context.drawCenteredTextWithShadow(textRenderer,
              value,
              x,
              y,
              IntRuleEntry.this.context.isMutable() ? GuiUtil.LABEL_COLOR : Colors.LIGHT_GRAY
          );
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
          builder.put(NarrationPart.TITLE, this.getMessage());
        }
      };
    }

    public static FlowListWidget.EntryFactory<IntRuleEntry> factory(
        GameRules gameRules, GameRules.Key<GameRules.IntRule> key, boolean mutable, TextRenderer textRenderer
    ) {
      return (index, left, top, width) -> new IntRuleEntry(RuleContext.of(gameRules, key, mutable),
          textRenderer,
          index,
          left,
          top,
          width
      );
    }
  }
}
