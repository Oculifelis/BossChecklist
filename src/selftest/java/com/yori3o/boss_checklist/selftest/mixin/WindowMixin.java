package com.yori3o.boss_checklist.selftest.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Self-test runs only (-Pselftest): the test client opens off screen and never takes focus. */
@Mixin(Window.class)
public abstract class WindowMixin {
    private static final int OFF = -4000;

    @Shadow
    public abstract long handle();

    @Shadow
    private boolean fullscreenRequested;

    private static boolean bctest$noFocus() {
        return Boolean.getBoolean("boss_checklist.selftest");
    }

    @Inject(method = "createWindow", at = @At("HEAD"))
    private void bctest$noFocusHints(CallbackInfoReturnable<Long> cir) {
        if (!bctest$noFocus()) {
            return;
        }
        SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_SHOWN, "0");
        SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_RAISED, "0");
    }

    @Inject(method = "setMode", at = @At("HEAD"))
    private void bctest$noFullscreen(CallbackInfo ci) {
        if (bctest$noFocus()) {
            this.fullscreenRequested = false;
        }
    }

    @ModifyArg(method = "setWindowSizeAndPosition", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_SetWindowPosition(JII)Z", remap = false), index = 1)
    private int bctest$offX(int x) {
        return bctest$noFocus() ? OFF : x;
    }

    @ModifyArg(method = "setWindowSizeAndPosition", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_SetWindowPosition(JII)Z", remap = false), index = 2)
    private int bctest$offY(int y) {
        return bctest$noFocus() ? OFF : y;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bctest$offScreen(CallbackInfo ci) {
        if (!bctest$noFocus()) {
            return;
        }
        SDLVideo.SDL_SetWindowPosition(handle(), OFF, OFF);
    }
}
