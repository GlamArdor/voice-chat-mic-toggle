package com.glamardor.mictoggle;

import com.glamardor.mictoggle.config.ToggleConfig;
import com.glamardor.mictoggle.gui.ConfigScreenFactory;
import com.glamardor.mictoggle.gui.LivePreview;
import com.glamardor.mictoggle.toggle.Hooked;
import com.glamardor.mictoggle.toggle.ToggleState;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** Wires the mod up. Everything it does is client side; the server is never told it exists. */
public class MicToggleClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ToggleConfig.load();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ToggleState.tick();
			LivePreview.tick(client);
		});

		// A microphone left open must not follow you to the next server.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ToggleState.reset());

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> checkHooked());

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
			dispatcher.register(settings("mictoggle"));
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> settings(String name) {
		return literal(name).executes(context -> {
			MinecraftClient client = context.getSource().getClient();
			// Straight from a command, the chat screen is still open; the settings have to wait for it
			// to close or they will be closed with it.
			client.execute(() -> client.setScreen(ConfigScreenFactory.create(null)));
			return 1;
		});
	}

	private static void checkHooked() {
		try {
			Class<?> handler = Class.forName("de.maxhenkel.voicechat.voice.client.PTTKeyHandler");
			if (!Hooked.class.isAssignableFrom(handler)) {
				MicToggle.LOGGER.error("Simple Voice Chat's key handler could not be hooked;"
						+ " the keys work as they always did");
			}
		} catch (Throwable error) {
			MicToggle.LOGGER.error("Simple Voice Chat's key handler was not found;"
					+ " the keys work as they always did", error);
		}
	}
}
