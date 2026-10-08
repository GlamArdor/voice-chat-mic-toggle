package com.glamardor.mictoggle.gui;

import com.glamardor.mictoggle.MicToggle;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Hands out whichever settings screen the player can actually run: the Cloth Config one when that
 * mod is present, our own otherwise.
 */
public final class ConfigScreenFactory {

	private ConfigScreenFactory() {
	}

	public static boolean isClothPresent() {
		FabricLoader loader = FabricLoader.getInstance();
		return loader.isModLoaded("cloth-config") || loader.isModLoaded("cloth-config2");
	}

	public static Screen create(@Nullable Screen parent) {
		if (isClothPresent()) {
			try {
				return ClothConfigScreens.build(parent);
			} catch (Throwable error) {
				// A Cloth major version bump degrades to our own screen rather than taking the game
				// with it.
				MicToggle.LOGGER.warn("Cloth Config screen failed, using the built-in one", error);
			}
		}
		return new FallbackConfigScreen(parent);
	}
}
