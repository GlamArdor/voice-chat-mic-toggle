package com.glamardor.mictoggle.config;

import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Remembers which screens are ours, so the blur can be cancelled for those and only those.
 *
 * <p>Held weakly: a screen that has been closed is rubbish, and this must not be the reason it
 * stays alive.
 */
public final class ConfigScreenMarker {

	private static final Set<Screen> OURS =
			Collections.newSetFromMap(new WeakHashMap<>());

	private ConfigScreenMarker() {
	}

	public static Screen mark(Screen screen) {
		OURS.add(screen);
		return screen;
	}

	public static boolean isOurs(@Nullable Screen screen) {
		return screen != null && OURS.contains(screen);
	}
}
