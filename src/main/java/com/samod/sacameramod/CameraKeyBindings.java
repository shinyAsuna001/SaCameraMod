package com.samod.sacameramod;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class CameraKeyBindings {
    public static KeyBinding TOGGLE_CAMERA;

    public static void register() {
        TOGGLE_CAMERA = new KeyBinding("key.sacameramod.toggle_camera", GLFW.GLFW_KEY_C, "key.categories.sacameramod");
        ClientRegistry.registerKeyBinding(TOGGLE_CAMERA);
    }
}
