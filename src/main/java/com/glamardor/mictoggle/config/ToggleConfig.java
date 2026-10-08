package com.glamardor.mictoggle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain settings holder, kept as JSON in the game's config directory.
 *
 * <p>Free of Cloth Config and Mod Menu types on purpose: both are optional, and the mod has to run
 * with neither installed. Every value is a whole number or a switch, because Cloth's sliders are
 * whole numbers only.
 */
public class ToggleConfig {

	// ── Keys ─────────────────────────────────────────────────────────────────

	/** Master switch. Off means both keys behave exactly as Simple Voice Chat makes them. */
	public boolean enabled = true;

	/** The push-to-talk key. */
	public KeyMode voiceMode = KeyMode.TOGGLE;

	/** The whisper key. */
	public KeyMode whisperMode = KeyMode.TOGGLE;

	/**
	 * In {@link KeyMode#BOTH}, how long a key has to be held before letting go of it switches the
	 * microphone off again. A shorter press than this is a tap and leaves it on.
	 */
	public int holdThresholdMs = 300;

	/**
	 * Keyboard keys do nothing while a screen is open, the chat above all: otherwise typing the
	 * letter the key sits on would switch the microphone on in the middle of a sentence. Mouse
	 * buttons still work, they cannot be typed by accident.
	 */
	public boolean ignoreInScreens = true;

	// ── Feedback ─────────────────────────────────────────────────────────────

	/** A short click as the microphone switches on and off, so the key can be pressed blind. */
	public boolean clickSound = true;

	public int soundVolumePercent = 60;

	/** A line over the hotbar every time the microphone switches. */
	public boolean announce = false;

	// ── Bookkeeping ──────────────────────────────────────────────────────────

	/**
	 * Deliberately starts at zero rather than at the current version: Gson runs the field
	 * initialisers and then overwrites only what the file actually contains, so a default of
	 * "current" would mark every old file as already migrated.
	 */
	public int configVersion = 0;

	private static final int CURRENT_VERSION = 1;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH =
			FabricLoader.getInstance().getConfigDir().resolve("voicechat_mic_toggle.json");

	private static ToggleConfig instance;

	public static ToggleConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static void load() {
		ToggleConfig loaded = null;
		try {
			if (Files.exists(PATH)) {
				try (Reader reader = Files.newBufferedReader(PATH)) {
					JsonElement tree = JsonParser.parseReader(reader);
					if (tree != null && tree.isJsonObject()) {
						loaded = GSON.fromJson(tree, ToggleConfig.class);
					}
				}
			}
		} catch (Exception ignored) {
			loaded = null;
		}
		instance = loaded != null ? loaded : new ToggleConfig();
		instance.sanitize();
		save();
	}

	public static void save() {
		if (instance == null) {
			instance = new ToggleConfig();
		}
		instance.sanitize();
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (Exception ignored) {
		}
	}

	/** Fills in anything a hand-edited or older file left null or out of range. */
	private void sanitize() {
		configVersion = CURRENT_VERSION;
		if (voiceMode == null) {
			voiceMode = KeyMode.TOGGLE;
		}
		if (whisperMode == null) {
			whisperMode = KeyMode.TOGGLE;
		}
		holdThresholdMs = clamp(holdThresholdMs, 100, 1500);
		soundVolumePercent = clamp(soundVolumePercent, 0, 100);
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : Math.min(value, max);
	}

	/** Puts every setting back to its default, in place. */
	public void resetToDefaults() {
		copyInto(new ToggleConfig(), this);
		save();
	}

	/** Copies every public field across. */
	public static void copyInto(ToggleConfig from, ToggleConfig to) {
		for (Field field : ToggleConfig.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			try {
				field.set(to, field.get(from));
			} catch (IllegalAccessException ignored) {
			}
		}
	}

	/** @return a detached copy, for previewing edits that can still be discarded. */
	public ToggleConfig copy() {
		ToggleConfig copy = new ToggleConfig();
		copyInto(this, copy);
		return copy;
	}
}
