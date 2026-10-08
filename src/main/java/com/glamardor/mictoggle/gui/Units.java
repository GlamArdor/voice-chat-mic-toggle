package com.glamardor.mictoggle.gui;

import net.minecraft.text.Text;

/** Captions for slider values, shared by both settings screens. */
final class Units {

	private Units() {
	}

	static Text percent(int value) {
		return Text.translatable("voicechat_mic_toggle.unit.percent", value);
	}

	static Text millis(int value) {
		return Text.translatable("voicechat_mic_toggle.unit.millis", value);
	}
}
