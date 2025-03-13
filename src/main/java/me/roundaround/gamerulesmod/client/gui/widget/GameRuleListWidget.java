package me.roundaround.gamerulesmod.client.gui.widget;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.GameRulesMod;
import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.gamerulesmod.roundalib.client.gui.GuiUtil;
import me.roundaround.gamerulesmod.roundalib.client.gui.layout.NonPositioningLayoutWidget;
import me.roundaround.gamerulesmod.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.gamerulesmod.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.gamerulesmod.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.gamerulesmod.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.gamerulesmod.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.gamerulesmod.roundalib.client.gui.widget.TooltipWidget;
import me.roundaround.gamerulesmod.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.gamerulesmod.util.CancelHandle;
import me.roundaround.gamerulesmod.util.RuleInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.world.GameRules;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class GameRuleListWidget extends ParentElementEntryListWidget<GameRuleListWidget.Entry> implements AutoCloseable {
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

  private static GameRules defaultRules = null;

  private final BiConsumer<Boolean, Boolean> onRuleChange;

  private CancelHandle cancelHandle;
  private boolean showImmutable = false;

  public GameRuleListWidget(
      MinecraftClient client,
      ThreeSectionLayoutWidget layout,
      BiConsumer<Boolean, Boolean> onRuleChange
  ) {
    super(client, layout);
    this.onRuleChange = onRuleChange;
  }

  public void setShowImmutable(boolean showImmutable) {
    this.showImmutable = showImmutable;
    this.fetch();
  }

  public boolean isShowingImmutable() {
    return this.showImmutable;
  }

  public void fetch() {
    this.cancel();

    this.clearEntries();
    this.addEntry(LoadingEntry.factory(this.client.textRenderer));
    this.refreshPositions();

    CompletableFuture<List<RuleInfo>> future = ClientNetworking.sendFetch(showImmutable);
    this.cancelHandle = CancelHandle.of(future);

    future.orTimeout(30, TimeUnit.SECONDS).whenCompleteAsync(
        (rules, throwable) -> {
          if (throwable != null) {
            this.setError();
          } else {
            try {
              this.setRules(rules, showImmutable);
            } catch (Exception e) {
              GameRulesMod.LOGGER.error("Exception thrown while populating rules list!", e);
              this.setError();
            }
          }
        }, this.client
    );
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

  private void setRules(final List<RuleInfo> rules, final boolean showImmutable) {
    this.clearEntries();

    final TextRenderer textRenderer = this.client.textRenderer;

    final GameRules gameRules = new GameRules();
    final HashMap<String, RuleInfo.State> stateMap = new HashMap<>();
    final HashMap<String, Date> changedMap = new HashMap<>();

    rules.forEach((ruleInfo) -> {
      ruleInfo.applyValue(gameRules);
      stateMap.put(ruleInfo.id(), ruleInfo.state());
      changedMap.put(ruleInfo.id(), ruleInfo.changed());
    });

    final HashMap<GameRules.Category, HashMap<GameRules.Key<?>, FlowListWidget.EntryFactory<? extends RuleEntry>>> ruleEntries = new HashMap<>();

    GameRules.accept(new GameRules.Visitor() {
      @Override
      public void visitBoolean(GameRules.Key<GameRules.BooleanRule> key, GameRules.Type<GameRules.BooleanRule> type) {
        RuleInfo.State state = stateMap.getOrDefault(key.getName(), RuleInfo.State.IMMUTABLE);
        if (showImmutable || !state.equals(RuleInfo.State.IMMUTABLE)) {
          this.addEntry(
              key, BooleanRuleEntry.factory(
                  gameRules,
                  key,
                  state,
                  changedMap.get(key.getName()),
                  GameRuleListWidget.this::onRuleChange,
                  textRenderer
              )
          );
        }
      }

      @Override
      public void visitInt(GameRules.Key<GameRules.IntRule> key, GameRules.Type<GameRules.IntRule> type) {
        RuleInfo.State state = stateMap.getOrDefault(key.getName(), RuleInfo.State.IMMUTABLE);
        if (showImmutable || !state.equals(RuleInfo.State.IMMUTABLE)) {
          this.addEntry(
              key, IntRuleEntry.factory(
                  gameRules,
                  key,
                  state,
                  changedMap.get(key.getName()),
                  GameRuleListWidget.this::onRuleChange,
                  textRenderer
              )
          );
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

    this.onRuleChange();
    this.refreshPositions();
  }

  private void onRuleChange() {
    boolean allValid = this.entries.stream()
        .allMatch((entry) -> !(entry instanceof RuleEntry ruleEntry) || ruleEntry.isValid());
    boolean anyDirty = this.entries.stream()
        .anyMatch((entry) -> entry instanceof RuleEntry ruleEntry && ruleEntry.isDirty());

    this.onRuleChange.accept(allValid, anyDirty);
  }

  public Map<String, Either<Boolean, Integer>> getDirtyValues() {
    HashMap<String, Either<Boolean, Integer>> values = new HashMap<>();
    for (Entry entry : this.entries) {
      if (entry instanceof RuleEntry ruleEntry) {
        if (!ruleEntry.isValid()) {
          return Map.of();
        }
        if (ruleEntry.isDirty()) {
          values.put(ruleEntry.getId(), ruleEntry.getValue());
        }
      }
    }
    return values;
  }

  @Override
  public void close() {
    this.cancel();
  }

  private static GameRules getDefaultRules() {
    if (defaultRules == null) {
      defaultRules = new GameRules();
    }
    return defaultRules;
  }

  public static class RuleContext {
    private final String id;
    private final Text name;
    private final List<Text> tooltip;
    private final String narrationName;
    private final Either<Boolean, Integer> initialValue;
    private final RuleInfo.State state;
    private final Runnable onChange;

    private Either<Boolean, Integer> value;
    private boolean valid = true;

    private RuleContext(
        String id,
        Text name,
        List<Text> tooltip,
        String narrationName,
        Either<Boolean, Integer> initialValue,
        RuleInfo.State state,
        Runnable onChange
    ) {
      this.id = id;
      this.name = name;
      this.tooltip = tooltip;
      this.narrationName = narrationName;
      this.initialValue = initialValue;
      this.state = state;
      this.onChange = onChange;

      this.value = initialValue;
    }

    public static <T extends GameRules.Rule<T>> RuleContext of(
        GameRules gameRules,
        GameRules.Key<T> key,
        RuleInfo.State state,
        Date changed,
        Runnable onChange
    ) {
      return new RuleContext(
          key.getName(),
          getName(key),
          getTooltip(key, state, changed),
          getNarrationName(key),
          gameRules.gamerulesmod$getValue(key.getName()),
          state,
          onChange
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
      if (!this.isMutable()) {
        return;
      }
      this.value = Either.left(value);
    }

    public void setValue(int value) {
      if (!this.isMutable()) {
        return;
      }
      this.value = Either.right(value);
    }

    public boolean isDirty() {
      return !Objects.equals(this.initialValue, this.value);
    }

    public boolean isValid() {
      return this.valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public boolean isMutable() {
      return this.state.equals(RuleInfo.State.MUTABLE);
    }

    public void markChanged() {
      this.onChange.run();
    }

    private static <T extends GameRules.Rule<T>> Text getName(GameRules.Key<T> key) {
      return Text.translatable(key.getTranslationKey());
    }

    private static <T extends GameRules.Rule<T>> List<Text> getTooltip(
        GameRules.Key<T> key,
        RuleInfo.State state,
        Date changed
    ) {
      ArrayList<Text> lines = new ArrayList<>();
      lines.add(Text.literal(key.getName()).formatted(Formatting.YELLOW));
      getDescription(key).ifPresent(lines::add);
      lines.add(getDefaultLine(key));

      Text formattedChanged = changed == null ?
          Text.translatable("gamerulesmod.main.never").formatted(Formatting.GRAY) :
          Text.literal(formatDate(changed)).formatted(Formatting.AQUA);
      lines.add(Text.translatable("gamerulesmod.main.date", formattedChanged));

      if (state.equals(RuleInfo.State.LOCKED)) {
        lines.add(Text.translatable("gamerulesmod.main.locked").formatted(Formatting.GRAY, Formatting.ITALIC));
      } else if (state.equals(RuleInfo.State.IMMUTABLE)) {
        lines.add(Text.translatable("gamerulesmod.main.immutable").formatted(Formatting.GRAY, Formatting.ITALIC));
      }

      return lines;
    }

    private static String formatDate(Date date) {
      return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private static <T extends GameRules.Rule<T>> Optional<Text> getDescription(GameRules.Key<T> key) {
      String descriptionI18nKey = key.getTranslationKey() + ".description";
      if (I18n.hasTranslation(descriptionI18nKey)) {
        return Optional.of(Text.translatable(descriptionI18nKey));
      }
      return Optional.empty();
    }

    private static <T extends GameRules.Rule<T>> Text getDefaultLine(GameRules.Key<T> key) {
      return Text.translatable("editGamerule.default", Text.literal(getDefaultRules().get(key).serialize()))
          .formatted(Formatting.GRAY);
    }

    private static <T extends GameRules.Rule<T>> String getNarrationName(GameRules.Key<T> key) {
      String defaultLine = getDefaultLine(key).getString();
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

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(
              this.getContentLeft(),
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

      this.addLayout(
          layout,
          (self) -> self.setPositionAndDimensions(
              this.getContentLeft(),
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

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(
              this.getContentLeft(),
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

      this.addLayout(
          layout,
          (self) -> self.setPositionAndDimensions(
              this.getContentLeft(),
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

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(
              this.getContentLeft(),
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

      this.addLayout(
          layout,
          (self) -> self.setPositionAndDimensions(
              this.getContentLeft(),
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

      LinearLayoutWidget layout = LinearLayoutWidget.vertical(
              this.getContentLeft(),
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

      this.addLayout(
          layout,
          (self) -> self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
      );

      layout.forEachChild(this::addDrawableChild);
    }

    @Override
    protected void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      renderRowShade(
          context,
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

    protected RuleEntry(RuleContext context, TextRenderer textRenderer, int index, int left, int top, int width) {
      super(textRenderer, index, left, top, width);

      this.context = context;

      this.createAndAddTooltip();
      this.createAndAddContent();
    }

    protected abstract ClickableWidget createControlWidget();

    public boolean isDirty() {
      return this.context.isDirty();
    }

    public boolean isValid() {
      return this.context.isValid();
    }

    public String getId() {
      return this.context.getId();
    }

    public Either<Boolean, Integer> getValue() {
      return this.context.getValue();
    }

    private void createAndAddTooltip() {
      NonPositioningLayoutWidget layout = new NonPositioningLayoutWidget(
          this.getContentLeft(),
          this.getContentTop(),
          this.getContentWidth(),
          this.getContentHeight()
      );
      TooltipWidget tooltip = layout.add(
          new TooltipWidget(this.context.getTooltip()),
          (parent, self) -> self.setDimensionsAndPosition(
              parent.getWidth(),
              parent.getHeight(),
              parent.getX(),
              parent.getY()
          )
      );
      this.addLayout(
          layout,
          (self) -> self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
      );
      this.addDrawable(tooltip);
    }

    protected void createAndAddContent() {
      LinearLayoutWidget layout = LinearLayoutWidget.horizontal(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight()
          )
          .spacing(GuiUtil.PADDING)
          .defaultOffAxisContentAlignCenter();

      layout.add(
          LabelWidget.builder(this.textRenderer, this.context.getName())
              .alignTextLeft()
              .alignTextCenterY()
              .overflowBehavior(LabelWidget.OverflowBehavior.WRAP)
              .maxLines(2)
              .showShadow()
              .hideBackground()
              .build(), (parent, self) -> self.setDimensions(this.getLabelWidth(parent), this.getContentHeight())
      );

      layout.add(
          this.createControlWidget(),
          (parent, self) -> self.setDimensions(this.getControlWidth(parent), this.getContentHeight())
      );

      this.addLayout(
          layout,
          (self) -> self.setPositionAndDimensions(
              this.getContentLeft(),
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
      return Math.max(CONTROL_MIN_WIDTH, Math.round(layout.getWidth() * 0.25f));
    }
  }

  public static class BooleanRuleEntry extends RuleEntry {
    protected BooleanRuleEntry(
        RuleContext context,
        TextRenderer textRenderer,
        int index,
        int left,
        int top,
        int width
    ) {
      super(context, textRenderer, index, left, top, width);
    }

    public Boolean getBooleanValue() {
      return this.getValue().left().orElseThrow();
    }

    @Override
    protected ClickableWidget createControlWidget() {
      var widget = CyclingButtonWidget.onOffBuilder(this.getBooleanValue())
          .omitKeyText()
          .narration((button) -> button.getGenericNarrationMessage()
              .append("\n")
              .append(this.context.getNarrationName()))
          .build(
              0, 0, 1, 1, this.context.getName(), (button, value) -> {
                this.context.setValue(value);
                this.context.markChanged();
              }
          );
      widget.active = this.context.isMutable();
      return widget;
    }

    public static FlowListWidget.EntryFactory<BooleanRuleEntry> factory(
        GameRules gameRules,
        GameRules.Key<GameRules.BooleanRule> key,
        RuleInfo.State state,
        Date changed,
        Runnable onChange,
        TextRenderer textRenderer
    ) {
      return (index, left, top, width) -> new BooleanRuleEntry(
          RuleContext.of(gameRules, key, state, changed, onChange),
          textRenderer,
          index,
          left,
          top,
          width
      );
    }
  }

  public static class IntRuleEntry extends RuleEntry {
    protected IntRuleEntry(RuleContext context, TextRenderer textRenderer, int index, int left, int top, int width) {
      super(context, textRenderer, index, left, top, width);
    }

    public Integer getIntValue() {
      return this.getValue().right().orElseThrow();
    }

    @Override
    protected ClickableWidget createControlWidget() {
      var widget = new TextFieldWidget(
          this.textRenderer,
          CONTROL_MIN_WIDTH,
          20,
          this.context.getName().copy().append(this.context.getNarrationName()).append("\n")
      );
      widget.setText(Integer.toString(IntRuleEntry.this.getIntValue()));
      widget.setChangedListener((value) -> {
        int previousValue = IntRuleEntry.this.getIntValue();
        boolean previousValid = this.context.isValid();

        try {
          int parsed = Integer.parseInt(value);
          this.context.setValue(parsed);
          this.context.setValid(true);
          widget.setEditableColor(GuiUtil.LABEL_COLOR);
        } catch (Exception e) {
          this.context.setValid(false);
          widget.setEditableColor(GuiUtil.ERROR_COLOR);
        }

        if (previousValue != IntRuleEntry.this.getIntValue() || previousValid != this.context.isValid()) {
          this.context.markChanged();
        }
      });
      widget.active = this.context.isMutable();
      return widget;
    }

    public static FlowListWidget.EntryFactory<IntRuleEntry> factory(
        GameRules gameRules,
        GameRules.Key<GameRules.IntRule> key,
        RuleInfo.State state,
        Date changed,
        Runnable onChange,
        TextRenderer textRenderer
    ) {
      return (index, left, top, width) -> new IntRuleEntry(
          RuleContext.of(gameRules, key, state, changed, onChange),
          textRenderer,
          index,
          left,
          top,
          width
      );
    }
  }
}
