package com.glamardor.mictoggle.gui;

import com.glamardor.mictoggle.config.ConfigScreenMarker;
import com.glamardor.mictoggle.config.KeyMode;
import com.glamardor.mictoggle.config.ToggleConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Cloth Config version of the settings screen.
 *
 * <p>Only ever reached by way of {@link ConfigScreenFactory}, which checks that Cloth is installed
 * first: naming these classes anywhere that runs unconditionally would break the mod for everybody
 * who does not have it.
 */
final class ClothConfigScreens {

	/** Widgets to read back into the config every tick, so changes take effect at once. */
	private static final List<Runnable> LIVE = new ArrayList<>();

	private ClothConfigScreens() {
	}

	static Screen build(@Nullable Screen parent) {
		ToggleConfig config = ToggleConfig.get();
		ToggleConfig defaults = new ToggleConfig();
		LIVE.clear();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Text.translatable("voicechat_mic_toggle.config.title"))
				.setSavingRunnable(() -> {
					LivePreview.markSaved();
					ToggleConfig.save();
				});

		builder.setTransparentBackground(true);
		// One long list with the categories down the side, rather than tabs: tabbed, the search box
		// only looks inside the tab you are standing in.
		builder.setGlobalized(true);
		builder.setGlobalizedExpanded(true);

		ConfigEntryBuilder entries = builder.entryBuilder();

		ConfigCategory keys = category(builder, "keys");
		keys.addEntry(toggle(entries, "enabled", config.enabled, defaults.enabled,
				value -> config.enabled = value));
		keys.addEntry(keyMode(entries, "voice_mode", config.voiceMode, defaults.voiceMode,
				value -> config.voiceMode = value));
		keys.addEntry(keyMode(entries, "whisper_mode", config.whisperMode, defaults.whisperMode,
				value -> config.whisperMode = value));
		keys.addEntry(slider(entries, "hold_threshold", config.holdThresholdMs, defaults.holdThresholdMs,
				100, 1500, value -> config.holdThresholdMs = value, Units::millis));
		keys.addEntry(toggle(entries, "ignore_in_screens", config.ignoreInScreens, defaults.ignoreInScreens,
				value -> config.ignoreInScreens = value));

		ConfigCategory feedback = category(builder, "feedback");
		feedback.addEntry(toggle(entries, "click_sound", config.clickSound, defaults.clickSound,
				value -> config.clickSound = value));
		feedback.addEntry(slider(entries, "sound_volume", config.soundVolumePercent, defaults.soundVolumePercent,
				0, 100, value -> config.soundVolumePercent = value, Units::percent));
		feedback.addEntry(toggle(entries, "announce", config.announce, defaults.announce,
				value -> config.announce = value));

		Screen built = ConfigScreenMarker.mark(builder.build());
		LivePreview.start(built, LIVE);
		return built;
	}

	// ── helpers ─────────────────────────────────────────────────────────────

	private static ConfigCategory category(ConfigBuilder builder, String name) {
		return builder.getOrCreateCategory(Text.translatable("voicechat_mic_toggle.category." + name));
	}

	private static Text option(String key) {
		return Text.translatable("voicechat_mic_toggle.option." + key);
	}

	private static Text tooltip(String key) {
		return Text.translatable("voicechat_mic_toggle.option." + key + ".tooltip");
	}

	private static EnumListEntry<KeyMode> keyMode(ConfigEntryBuilder entries, String key, KeyMode current,
			KeyMode fallback, Consumer<KeyMode> setter) {
		var entry = entries.startEnumSelector(option(key), KeyMode.class, current)
				.setDefaultValue(fallback)
				.setEnumNameProvider(value -> Text.translatable(((KeyMode) value).translationKey()))
				.setTooltip(tooltip(key))
				.setSaveConsumer(setter)
				.build();
		LIVE.add(() -> setter.accept(entry.getValue()));
		return entry;
	}

	private static me.shedaniel.clothconfig2.gui.entries.BooleanListEntry toggle(
			ConfigEntryBuilder entries, String key, boolean current, boolean fallback,
			Consumer<Boolean> setter) {
		var entry = entries.startBooleanToggle(option(key), current)
				.setDefaultValue(fallback)
				.setTooltip(tooltip(key))
				.setSaveConsumer(setter)
				.build();
		LIVE.add(() -> setter.accept(entry.getValue()));
		return entry;
	}

	/** Cloth's sliders are whole numbers only, which is why every setting here is one. */
	private static me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry slider(
			ConfigEntryBuilder entries, String key, int current, int fallback, int min, int max,
			Consumer<Integer> setter, Function<Integer, Text> display) {
		var entry = entries.startIntSlider(option(key), clamp(current, min, max), min, max)
				.setDefaultValue(fallback)
				.setTextGetter(display::apply)
				.setTooltip(tooltip(key))
				.setSaveConsumer(setter)
				.build();
		LIVE.add(() -> setter.accept(entry.getValue()));
		return entry;
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : Math.min(value, max);
	}
}
