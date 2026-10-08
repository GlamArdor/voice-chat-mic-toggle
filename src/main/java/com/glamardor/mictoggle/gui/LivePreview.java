package com.glamardor.mictoggle.gui;

import com.glamardor.mictoggle.MicToggle;
import com.glamardor.mictoggle.config.ToggleConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Makes the Cloth settings screen take effect while it is open.
 *
 * <p>Cloth hands a value over only when the player presses save. Here the widgets are read every
 * tick and written straight into the live config instead, the same as in every other settings
 * screen of these mods, so a key set to "toggle" can be tried with the menu still open behind it.
 *
 * <p>That leaves the promise Cloth makes – that escape discards your edits – which is worth
 * keeping. The config is copied when the screen opens and copied back when it closes without a
 * save, so a preview stays a preview.
 */
public final class LivePreview {

	/**
	 * How long another screen may sit on top before the preview gives up and reverts. Cloth puts a
	 * confirmation dialog in front of its own screen when you leave with unsaved changes, and
	 * reverting the moment that appears would undo everything the player is being asked about.
	 */
	private static final long AWAY_MILLIS = 3000L;

	@Nullable
	private static Screen owner;
	@Nullable
	private static ToggleConfig snapshot;
	private static List<Runnable> appliers = List.of();
	private static boolean saved;
	private static long awaySince;

	private LivePreview() {
	}

	/** Takes over: from now until this screen closes, its widgets drive the config. */
	public static void start(Screen screen, List<Runnable> widgets) {
		finish();
		owner = screen;
		appliers = List.copyOf(widgets);
		saved = false;
		awaySince = 0L;
		snapshot = ToggleConfig.get().copy();
	}

	/** The player pressed save, so the preview is now the real thing. */
	public static void markSaved() {
		saved = true;
		snapshot = ToggleConfig.get().copy();
	}

	public static void tick(MinecraftClient client) {
		if (owner == null) {
			return;
		}
		if (client.currentScreen == owner) {
			awaySince = 0L;
			for (Runnable applier : appliers) {
				try {
					applier.run();
				} catch (Throwable error) {
					MicToggle.LOGGER.warn("a settings widget could not be previewed", error);
				}
			}
			return;
		}
		long now = System.currentTimeMillis();
		if (awaySince == 0L) {
			awaySince = now;
			return;
		}
		if (client.currentScreen == null || now - awaySince > AWAY_MILLIS) {
			finish();
		}
	}

	private static void finish() {
		if (owner != null && !saved && snapshot != null) {
			ToggleConfig.copyInto(snapshot, ToggleConfig.get());
		}
		owner = null;
		snapshot = null;
		appliers = List.of();
		saved = false;
		awaySince = 0L;
	}
}
