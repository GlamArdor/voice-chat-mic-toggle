package com.glamardor.mictoggle.gui;

import com.glamardor.mictoggle.config.ConfigScreenMarker;
import com.glamardor.mictoggle.config.ToggleConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The settings screen used when Cloth Config is not installed.
 *
 * <p>Same shape as the Cloth one on purpose: one long list with the section names in it, a search
 * box that looks through all of them at once, nothing hidden behind tabs, and no save button –
 * every widget writes straight into the live config.
 *
 * <p>Nothing is drawn behind it either: the world stays in view.
 */
public class FallbackConfigScreen extends Screen {

	private static final int ROW_WIDTH = 310;

	@Nullable
	private final Screen parent;
	private final ToggleConfig config = ToggleConfig.get();
	private String filter = "";
	private OptionList list;
	private TextFieldWidget search;

	public FallbackConfigScreen(@Nullable Screen parent) {
		super(Text.translatable("voicechat_mic_toggle.config.title"));
		this.parent = parent;
		ConfigScreenMarker.mark(this);
	}

	/** Leaves the world visible. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	@Override
	protected void init() {
		search = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 20, 200, 18,
				Text.translatable("voicechat_mic_toggle.config.search"));
		search.setPlaceholder(Text.translatable("voicechat_mic_toggle.config.search").copy().formatted(Formatting.DARK_GRAY));
		search.setText(filter);
		search.setChangedListener(value -> {
			if (!value.equals(filter)) {
				filter = value;
				rebuildList();
			}
		});
		addDrawableChild(search);

		list = new OptionList(this.client, this.width, this.height - 96, 44, 25);
		buildOptions();
		addDrawableChild(list);

		ButtonWidget reset = ButtonWidget.builder(Text.translatable("voicechat_mic_toggle.config.reset"), button -> {
			config.resetToDefaults();
			clearAndInit();
		}).dimensions(this.width / 2 - 100, this.height - 54, 200, 20).build();
		reset.setTooltip(Tooltip.of(Text.translatable("voicechat_mic_toggle.config.reset.tooltip")));
		addDrawableChild(reset);

		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
				.dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
				.build());
	}

	/** Rebuilds only the list, so typing in the search box does not steal the caret from itself. */
	private void rebuildList() {
		remove(list);
		list = new OptionList(this.client, this.width, this.height - 96, 44, 25);
		buildOptions();
		addDrawableChild(list);
		setFocused(search);
	}

	private void buildOptions() {
		Section keys = new Section("keys");
		keys.add(toggle("enabled", () -> config.enabled, value -> config.enabled = value));
		keys.add(cycle("voice_mode", () -> Text.translatable(config.voiceMode.translationKey()),
				() -> config.voiceMode = config.voiceMode.next()));
		keys.add(cycle("whisper_mode", () -> Text.translatable(config.whisperMode.translationKey()),
				() -> config.whisperMode = config.whisperMode.next()));
		keys.add(slider("hold_threshold", config.holdThresholdMs, 100, 1500,
				value -> config.holdThresholdMs = value, Units::millis));
		keys.add(toggle("ignore_in_screens", () -> config.ignoreInScreens,
				value -> config.ignoreInScreens = value));
		keys.flush();

		Section feedback = new Section("feedback");
		feedback.add(toggle("click_sound", () -> config.clickSound, value -> config.clickSound = value));
		feedback.add(slider("sound_volume", config.soundVolumePercent, 0, 100,
				value -> config.soundVolumePercent = value, Units::percent));
		feedback.add(toggle("announce", () -> config.announce, value -> config.announce = value));
		feedback.flush();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);
		if (!ConfigScreenFactory.isClothPresent()) {
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.translatable("voicechat_mic_toggle.config.no_cloth"), this.width / 2,
					this.height - 66, 0xFF9A9A9A);
		}
	}

	@Override
	public void close() {
		ToggleConfig.save();
		MinecraftClient.getInstance().setScreen(parent);
	}

	// ── Building blocks ──────────────────────────────────────────────────────

	/**
	 * One section of the list, held back until it is known whether the search left anything in it –
	 * a heading over nothing is worse than no heading.
	 */
	private final class Section {
		private final String name;
		private final List<ClickableWidget> kept = new ArrayList<>();

		private Section(String name) {
			this.name = name;
		}

		void add(Option option) {
			if (matches(option.key())) {
				kept.add(option.widget());
			}
		}

		void flush() {
			if (kept.isEmpty()) {
				return;
			}
			list.addHeader(Text.translatable("voicechat_mic_toggle.category." + name));
			for (ClickableWidget widget : kept) {
				list.addWidget(widget);
			}
		}
	}

	/** A widget together with the key it was built from, which is what the search looks at. */
	private record Option(String key, ClickableWidget widget) {
	}

	private boolean matches(String key) {
		if (filter.isBlank()) {
			return true;
		}
		String needle = filter.toLowerCase(Locale.ROOT).trim();
		String label = Text.translatable("voicechat_mic_toggle.option." + key).getString().toLowerCase(Locale.ROOT);
		String tooltip = Text.translatable("voicechat_mic_toggle.option." + key + ".tooltip")
				.getString().toLowerCase(Locale.ROOT);
		return label.contains(needle) || tooltip.contains(needle) || key.contains(needle);
	}

	private Option toggle(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return cycle(key, () -> getter.get() ? ScreenTexts.ON : ScreenTexts.OFF,
				() -> setter.accept(!getter.get()));
	}

	private Option cycle(String key, Supplier<Text> value, Runnable onClick) {
		ButtonWidget button = ButtonWidget.builder(label(key, value.get()), widget -> {
			onClick.run();
			widget.setMessage(label(key, value.get()));
		}).dimensions(0, 0, ROW_WIDTH, 20).build();
		button.setTooltip(Tooltip.of(Text.translatable("voicechat_mic_toggle.option." + key + ".tooltip")));
		return new Option(key, button);
	}

	private Option slider(String key, int current, int min, int max, Consumer<Integer> setter,
			Function<Integer, Text> display) {
		return new Option(key, new OptionSlider(key, current, min, max, setter, display));
	}

	private static Text label(String key, Text value) {
		return Text.translatable("voicechat_mic_toggle.option." + key).copy().append(": ").append(value);
	}

	/** Slider over a whole-number range that writes straight back into the config object. */
	private static class OptionSlider extends SliderWidget {
		private final String key;
		private final int min;
		private final int max;
		private final Consumer<Integer> setter;
		private final Function<Integer, Text> display;

		OptionSlider(String key, int current, int min, int max, Consumer<Integer> setter,
				Function<Integer, Text> display) {
			super(0, 0, ROW_WIDTH, 20, Text.empty(),
					MathHelper.clamp((current - min) / (double) (max - min), 0.0, 1.0));
			this.key = key;
			this.min = min;
			this.max = max;
			this.setter = setter;
			this.display = display;
			setTooltip(Tooltip.of(Text.translatable("voicechat_mic_toggle.option." + key + ".tooltip")));
			updateMessage();
		}

		private int currentValue() {
			return (int) Math.round(min + (max - min) * this.value);
		}

		@Override
		protected void updateMessage() {
			setMessage(label(key, display.apply(currentValue())));
		}

		@Override
		protected void applyValue() {
			setter.accept(currentValue());
		}
	}

	/** The list itself: rows are either a widget or a section heading. */
	private static class OptionList extends ElementListWidget<OptionList.Entry> {
		OptionList(MinecraftClient client, int width, int height, int y, int itemHeight) {
			super(client, width, height, y, itemHeight);
		}

		void addWidget(ClickableWidget widget) {
			addEntry(new WidgetEntry(widget));
		}

		void addHeader(Text text) {
			addEntry(new HeaderEntry(text));
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		abstract static class Entry extends ElementListWidget.Entry<Entry> {
		}

		static class WidgetEntry extends Entry {
			private final ClickableWidget widget;

			WidgetEntry(ClickableWidget widget) {
				this.widget = widget;
			}

			@Override
			public void render(DrawContext context, int index, int y, int x, int entryWidth,
					int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
				widget.setX(x);
				widget.setY(y);
				widget.setWidth(entryWidth);
				widget.render(context, mouseX, mouseY, tickDelta);
			}

			@Override
			public List<? extends Element> children() {
				return List.of(widget);
			}

			@Override
			public List<? extends Selectable> selectableChildren() {
				return List.of(widget);
			}
		}

		static class HeaderEntry extends Entry {
			private final Text text;

			HeaderEntry(Text text) {
				this.text = text;
			}

			@Override
			public void render(DrawContext context, int index, int y, int x, int entryWidth,
					int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
				MinecraftClient client = MinecraftClient.getInstance();
				context.drawCenteredTextWithShadow(client.textRenderer, text, x + entryWidth / 2, y + 8, 0xFFE0C070);
			}

			@Override
			public List<? extends Element> children() {
				return List.of();
			}

			@Override
			public List<? extends Selectable> selectableChildren() {
				return List.of();
			}
		}
	}
}
