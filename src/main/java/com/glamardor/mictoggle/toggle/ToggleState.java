package com.glamardor.mictoggle.toggle;

import com.glamardor.mictoggle.MicToggle;
import com.glamardor.mictoggle.config.KeyMode;
import com.glamardor.mictoggle.config.ToggleConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Turns Simple Voice Chat's two held keys into switches.
 *
 * <p>SVC itself keeps doing all the work of reading the keys: after every key or mouse event it
 * stores whether its push-to-talk and whisper keys are down. That stored state is what this class
 * watches for presses and releases ({@link #update}), and what it answers instead of when the
 * microphone asks whether to send ({@link #voice}, {@link #whisper}).
 *
 * <p>Presses arrive on the render thread, the questions come from SVC's microphone thread, hence
 * the volatile switches.
 */
public final class ToggleState {

	private static final class Key {
		final String name;
		/** The key as SVC last saw it, to tell a press from a release. */
		boolean down;
		volatile boolean on;
		long pressedAt;
		/** This press is the one that switched it on, so in BOTH mode a long hold switches it off again. */
		boolean switchedOnByPress;
		/** Pressed while typing: its release must not count either. */
		volatile boolean ignored;

		Key(String name) {
			this.name = name;
		}
	}

	private static final Key VOICE = new Key("voice");
	private static final Key WHISPER = new Key("whisper");

	private static KeyMode lastVoiceMode;
	private static KeyMode lastWhisperMode;
	private static boolean lastEnabled;

	private ToggleState() {
	}

	/** Called by the mixin after every key and mouse event SVC has looked at. */
	public static void update(boolean voiceDown, boolean whisperDown, boolean fromKeyboard) {
		ToggleConfig config = ToggleConfig.get();
		edge(VOICE, WHISPER, voiceDown, config.voiceMode, fromKeyboard, config);
		edge(WHISPER, VOICE, whisperDown, config.whisperMode, fromKeyboard, config);
	}

	private static void edge(Key key, Key other, boolean down, KeyMode mode, boolean fromKeyboard,
			ToggleConfig config) {
		if (down == key.down) {
			return;
		}
		key.down = down;
		if (!config.enabled) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		long now = System.currentTimeMillis();

		if (down) {
			if (fromKeyboard && config.ignoreInScreens && client.currentScreen != null) {
				key.ignored = true;
				return;
			}
			key.ignored = false;
			// Talking and whispering at once makes no sense: either key, held or tapped, ends the
			// other one's switch. Silently, since this key's own click follows straight away.
			if (other.on) {
				other.on = false;
				other.switchedOnByPress = false;
			}
			if (mode == KeyMode.HOLD) {
				return;
			}
			key.pressedAt = now;
			if (key.on) {
				switchTo(client, key, false);
				key.switchedOnByPress = false;
			} else {
				switchTo(client, key, true);
				key.switchedOnByPress = true;
			}
			return;
		}

		if (key.ignored) {
			key.ignored = false;
			return;
		}
		if (mode == KeyMode.BOTH && key.switchedOnByPress && key.on
				&& now - key.pressedAt >= config.holdThresholdMs) {
			switchTo(client, key, false);
		}
		key.switchedOnByPress = false;
	}

	private static void switchTo(MinecraftClient client, Key key, boolean on) {
		key.on = on;
		feedback(client, key, on);
	}

	private static void feedback(MinecraftClient client, Key key, boolean on) {
		ToggleConfig config = ToggleConfig.get();
		if (config.clickSound && config.soundVolumePercent > 0) {
			try {
				Identifier id = Identifier.of(MicToggle.MOD_ID, key.name + (on ? ".on" : ".off"));
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(id), 1.0f,
						config.soundVolumePercent / 100.0f));
			} catch (Throwable error) {
				MicToggle.LOGGER.warn("toggle sound failed", error);
			}
		}
		if (config.announce && client.inGameHud != null) {
			client.inGameHud.setOverlayMessage(
					Text.translatable("voicechat_mic_toggle.announce." + key.name + (on ? ".on" : ".off")), false);
		}
	}

	/** What SVC's {@code isPTTDown} answers: the switch, or the bare key where the key is held. */
	public static boolean voice(boolean keyDown) {
		ToggleConfig config = ToggleConfig.get();
		if (!config.enabled) {
			return keyDown;
		}
		return VOICE.on || (config.voiceMode == KeyMode.HOLD && keyDown && !VOICE.ignored);
	}

	/** What SVC's {@code isWhisperDown} answers. */
	public static boolean whisper(boolean keyDown) {
		ToggleConfig config = ToggleConfig.get();
		if (!config.enabled) {
			return keyDown;
		}
		return WHISPER.on || (config.whisperMode == KeyMode.HOLD && keyDown && !WHISPER.ignored);
	}

	/** Switches both off without a sound: leaving a server, or the rules changing under the switch. */
	public static void reset() {
		for (Key key : new Key[] {VOICE, WHISPER}) {
			key.on = false;
			key.switchedOnByPress = false;
			key.ignored = false;
		}
	}

	/**
	 * Every tick. A switch left on while its key was set to "hold" (or the mod switched off) would
	 * keep the microphone open with no key that can close it, so changing the settings starts over.
	 */
	public static void tick() {
		ToggleConfig config = ToggleConfig.get();
		if (config.voiceMode != lastVoiceMode || config.whisperMode != lastWhisperMode
				|| config.enabled != lastEnabled) {
			lastVoiceMode = config.voiceMode;
			lastWhisperMode = config.whisperMode;
			lastEnabled = config.enabled;
			reset();
		}
	}
}
