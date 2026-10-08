package com.glamardor.mictoggle.svcmixin;

import com.glamardor.mictoggle.toggle.Hooked;
import com.glamardor.mictoggle.toggle.ToggleState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Simple Voice Chat's own key handler: it reads its two keys after every key and mouse event and
 * keeps whether each is down. Those two fields are watched for presses here, and the three getters
 * the microphone asks answer with the switches instead.
 *
 * <p>Reached by name, since SVC is not on the compile classpath. Its config is not required, so if
 * a future SVC reshapes this class the mod is left doing nothing (and says so in the log) rather
 * than stopping the game from starting.
 */
@Pseudo
@Mixin(targets = "de.maxhenkel.voicechat.voice.client.PTTKeyHandler", remap = false)
public abstract class PTTKeyHandlerMixin implements Hooked {

	@Shadow
	private boolean pttKeyDown;

	@Shadow
	private boolean whisperKeyDown;

	@Inject(method = "onKeyboardEvent", at = @At("TAIL"))
	private void mictoggle$afterKey(long window, int key, int scancode, CallbackInfo ci) {
		ToggleState.update(pttKeyDown, whisperKeyDown, true);
	}

	@Inject(method = "onMouseEvent", at = @At("TAIL"))
	private void mictoggle$afterMouse(long window, int button, int action, int mods, CallbackInfo ci) {
		ToggleState.update(pttKeyDown, whisperKeyDown, false);
	}

	@Inject(method = "isPTTDown", at = @At("HEAD"), cancellable = true)
	private void mictoggle$voice(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(ToggleState.voice(pttKeyDown));
	}

	@Inject(method = "isWhisperDown", at = @At("HEAD"), cancellable = true)
	private void mictoggle$whisper(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(ToggleState.whisper(whisperKeyDown));
	}

	@Inject(method = "isAnyDown", at = @At("HEAD"), cancellable = true)
	private void mictoggle$any(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(ToggleState.voice(pttKeyDown) || ToggleState.whisper(whisperKeyDown));
	}
}
