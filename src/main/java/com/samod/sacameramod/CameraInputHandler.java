package com.samod.sacameramod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaCameraMod.MODID, value = Dist.CLIENT)
public class CameraInputHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        // 直接使用按键码与动作来兼容不同 mappings
        if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS && event.getKey() == org.lwjgl.glfw.GLFW.GLFW_KEY_C) {
            if (CameraController.get().isActive()) CameraController.get().deactivate();
            else CameraController.get().activate(net.minecraft.util.math.vector.Vector3d.ZERO, 0f, 0f);
        }
    }
}
