package com.samod.sacameramod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class PacketCameraPerspective {
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean loggedPerspectiveCandidates = false;
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

            if (trySetCameraType(settings, mode)) {
                return;
            }

            Field perspectiveField = getField(settings.getClass(), "cameraType", "thirdPersonView", "thirdPerson", "viewMode", "thirdPersonView2", "perspective");
            if (perspectiveField != null) {
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
                return;
            }

            if (tryInvokePerspectiveSetter(settings, mode)) {
                return;
            }

            if (!loggedPerspectiveCandidates) {
                logPerspectiveCandidates(settings.getClass());
                loggedPerspectiveCandidates = true;
            }
            LOGGER.warn("Perspective packet failed: no perspective setter/field found on {}", settings.getClass().getName());
        } catch (Throwable t) {
            LOGGER.warn("Perspective packet failed to apply mode {}", mode, t);
        }
    }

    private static boolean trySetCameraType(Object settings, int mode) throws Exception {
        Method getter = getMethod(settings.getClass(), "getCameraType");
        if (getter == null) {
            getter = getMethodByNameAndParamCount(settings.getClass(), "func_243230_g", 0);
        }
        if (getter == null) {
            Field fallbackField = getField(settings.getClass(), "field_243228_bb", "cameraType", "pointOfView");
            if (fallbackField != null && fallbackField.getType().isEnum()) {
                fallbackField.setAccessible(true);
                Object[] constants = fallbackField.getType().getEnumConstants();
                if (mode < 0 || mode >= constants.length) {
                    LOGGER.warn("Perspective packet failed: invalid mode {} for enum {}", mode, fallbackField.getType().getName());
                    return true;
                }
                fallbackField.set(settings, constants[mode]);
                return true;
            }
            return false;
        }

        Class<?> cameraTypeClass = getter.getReturnType();
        if (!cameraTypeClass.isEnum()) {
            return false;
        }

        Object[] constants = cameraTypeClass.getEnumConstants();
        if (mode < 0 || mode >= constants.length) {
            LOGGER.warn("Perspective packet failed: invalid mode {} for enum {}", mode, cameraTypeClass.getName());
            return true;
        }

        Method setter = getMethod(settings.getClass(), "setCameraType", cameraTypeClass);
        if (setter == null) {
            setter = getMethodByNameAndParamCount(settings.getClass(), "func_243229_a", 1);
        }
        if (setter != null) {
            Class<?> paramType = setter.getParameterTypes()[0];
            if (paramType.isAssignableFrom(cameraTypeClass)) {
                setter.invoke(settings, constants[mode]);
                return true;
            }
        }

        Field field = getField(settings.getClass(), "field_243228_bb", "cameraType", "pointOfView");
        if (field != null) {
            field.setAccessible(true);
            field.set(settings, constants[mode]);
            return true;
        }

        Method intSetter = getMethod(settings.getClass(), "setCameraType", int.class);
        if (intSetter != null) {
            intSetter.invoke(settings, mode);
            return true;
        }

        return false;
    }

    private static Method getMethodByNameAndParamCount(Class<?> clazz, String name, int paramCount) {
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == paramCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static boolean tryInvokePerspectiveSetter(Object settings, int mode) throws Exception {
        Method setter = getMethod(settings.getClass(), "setPerspective", int.class);
        if (setter != null) {
            setter.invoke(settings, mode);
            return true;
        }
        return false;
    }

    private static void logPerspectiveCandidates(Class<?> clazz) {
        StringBuilder builder = new StringBuilder();
        builder.append("Perspective candidates on ").append(clazz.getName()).append(" and superclasses:\n");
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            builder.append("  class ").append(current.getName()).append("\n");
            for (Field field : current.getDeclaredFields()) {
                builder.append("    field: ").append(field.getName()).append(" : ").append(field.getType().getName()).append("\n");
            }
            for (Method method : current.getDeclaredMethods()) {
                builder.append("    method: ").append(method.getName()).append("(");
                Class<?>[] params = method.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) builder.append(',');
                    builder.append(params[i].getSimpleName());
                }
                builder.append(") : ").append(method.getReturnType().getName()).append("\n");
            }
        }
        LOGGER.warn(builder.toString());
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

    private static Method getMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        while (clazz != null) {
            try {
                Method method = clazz.getDeclaredMethod(name, paramTypes);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
            }
            clazz = clazz.getSuperclass();
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
