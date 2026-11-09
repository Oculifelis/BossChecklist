package com.yori3o.boss_checklist.selftest.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
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

    private static boolean bctest$noFocus() {
        return Boolean.getBoolean("boss_checklist.selftest");
    }

    @Inject(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
    private static void bctest$noFocusHints(CallbackInfoReturnable<Long> cir) {
        if (!bctest$noFocus()) {
            return;
        }
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_POSITION_X, OFF);
        GLFW.glfwWindowHint(GLFW.GLFW_POSITION_Y, OFF);
    }

    @ModifyArg(method = "setMode", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V", remap = false), index = 1)
    private long bctest$noFullscreen(long monitor) {
        return bctest$noFocus() ? 0L : monitor;
    }

    @ModifyArg(method = "setMode", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V", remap = false), index = 2)
    private int bctest$offX(int x) {
        return bctest$noFocus() ? OFF : x;
    }

    @ModifyArg(method = "setMode", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V", remap = false), index = 3)
    private int bctest$offY(int y) {
        return bctest$noFocus() ? OFF : y;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bctest$showOffScreen(CallbackInfo ci) {
        if (!bctest$noFocus()) {
            return;
        }
        GLFW.glfwSetWindowPos(handle(), OFF, OFF);
        GLFW.glfwShowWindow(handle());
    }
}
