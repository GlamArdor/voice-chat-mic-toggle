package com.glamardor.mictoggle.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Puts the settings behind the Mod Menu button, for the players who look for them there. */
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return com.glamardor.mictoggle.gui.ConfigScreenFactory::create;
	}
}
