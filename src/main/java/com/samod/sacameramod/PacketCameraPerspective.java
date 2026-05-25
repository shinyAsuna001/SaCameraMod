package com.samod.sacameramod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public class PacketCameraPerspective {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int mode;

    public PacketCameraPerspective(int mode) {
        this.mode = mode;
    }

    public static PacketCameraPerspective create(int mode) {
        return new PacketCameraPerspective(mode);
    }

    public static void encode(PacketCameraPerspective msg, PacketBuffer buf) {
        buf.writeInt(msg.mode);
    }

    public static PacketCameraPerspective decode(PacketBuffer buf) {
        int mode = buf.readInt();
        return new PacketCameraPerspective(mode);
    }

    public static void handle(PacketCameraPerspective msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> setPerspective(msg.mode));
        ctx.get().setPacketHandled(true);
    }

    private static void setPerspective(int mode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            LOGGER.warn("Perspective packet received but Minecraft instance is null");
            return;
        }

        try {
            Object settings = mc.options;
            if (settings == null) {
                LOGGER.warn("Perspective packet failed: no game settings object found");
                return;
            }

            Field perspectiveField = getField(settings.getClass(), "cameraType", "thirdPersonView", "thirdPerson", "viewMode", "thirdPersonView2", "perspective");
            if (perspectiveField == null) {
                LOGGER.warn("Perspective packet failed: no perspective field found on {}", settings.getClass().getName());
                return;
            }
            perspectiveField.setAccessible(true);
            Class<?> fieldType = perspectiveField.getType();
            if (fieldType.isEnum()) {
                Object[] constants = fieldType.getEnumConstants();
                if (mode < 0 || mode >= constants.length) {
                    LOGGER.warn("Perspective packet failed: invalid mode {} for enum {}", mode, fieldType.getName());
                    return;
                }
                perspectiveField.set(settings, constants[mode]);
            } else {
                perspectiveField.setInt(settings, mode);
            }
        } catch (Throwable t) {
            LOGGER.warn("Perspective packet failed to apply mode {}", mode, t);
        }
    }


    private static Object getField(Object instance, Class<?> clazz, String... names) {
        for (String name : names) {
            Field field = getField(clazz, name);
            if (field != null) {
                try {
                    return field.get(instance);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Field getField(Class<?> clazz, String... names) {
        for (String name : names) {
            Field field = getFieldRecursive(clazz, name);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    private static Field getFieldRecursive(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
