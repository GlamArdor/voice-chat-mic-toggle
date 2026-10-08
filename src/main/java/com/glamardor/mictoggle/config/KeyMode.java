package com.glamardor.mictoggle.config;

import java.util.Locale;

/** What a voice chat key does when it is pressed. */
public enum KeyMode {
	/** Every press switches it on or off. */
	TOGGLE,
	/** A tap switches it on until the next tap; holding it works only while held. */
	BOTH,
	/** Only while the key is held down: exactly what Simple Voice Chat does on its own. */
	HOLD;

	public String translationKey() {
		return "voicechat_mic_toggle.key_mode." + name().toLowerCase(Locale.ROOT);
	}

	public KeyMode next() {
		KeyMode[] all = values();
		return all[(ordinal() + 1) % all.length];
	}
}
