package com.glamardor.mictoggle.mixin;

import com.glamardor.mictoggle.config.ConfigScreenMarker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves the world visible behind our own settings screen.
 *
 * <p>Strictly for our screen: every other menu in the game, this mod's or anyone else's, keeps the
 * background it always had. The mod's own screen does not need this – it simply draws no background
 * – but the Cloth Config one is not ours to override, and this is the only way at it.
 *
 * <p>Both injections are optional: on a version where the methods are shaped differently the right
 * outcome is "the background is blurred again", not a game that will not start.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

	@Inject(method = "applyBlur(Lnet/minecraft/client/gui/DrawContext;)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void mictoggle$noBlur(DrawContext context, CallbackInfo ci) {
		if (ConfigScreenMarker.isOurs((Screen) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "renderDarkening(Lnet/minecraft/client/gui/DrawContext;)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void mictoggle$noDarkening(DrawContext context, CallbackInfo ci) {
		if (ConfigScreenMarker.isOurs((Screen) (Object) this)) {
			ci.cancel();
		}
	}
}
