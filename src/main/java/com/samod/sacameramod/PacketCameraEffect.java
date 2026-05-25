package com.samod.sacameramod;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketCameraEffect {
    public static final int TYPE_SHAKE_POSITIONED = 0;
    public static final int TYPE_SHAKE_ROTATIONED = 1;
    public static final int TYPE_MOVE = 2;
    public static final int TYPE_ROTATE_X = 3;
    public static final int TYPE_ROTATE_Y = 4;
    public static final int TYPE_ROTATE_Z = 5;
    public static final int TYPE_CANCEL = 6;

    private final int type;
    private final float x;
    private final float y;
    private final float z;
    private final boolean relative;
    private final boolean usePitch;
    private final float intensity;
    private final int fadeIn;
    private final int hold;
    private final int fadeOut;

    public PacketCameraEffect(int type, float x, float y, float z, boolean relative, boolean usePitch, float intensity, int fadeIn, int hold, int fadeOut) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.relative = relative;
        this.usePitch = usePitch;
        this.intensity = intensity;
        this.fadeIn = fadeIn;
        this.hold = hold;
        this.fadeOut = fadeOut;
    }

    public static PacketCameraEffect createShake(boolean rotationed, float intensity, int duration) {
        return new PacketCameraEffect(rotationed ? TYPE_SHAKE_ROTATIONED : TYPE_SHAKE_POSITIONED, 0f, 0f, 0f, false, false, intensity, duration, 0, 0);
    }

    public static PacketCameraEffect createMove(float dx, float dy, float dz, boolean relative, int fadeIn, int hold, int fadeOut) {
        return new PacketCameraEffect(TYPE_MOVE, dx, dy, dz, relative, false, 0f, fadeIn, hold, fadeOut);
    }

    public static PacketCameraEffect createMoveForward(float dx, float dy, float dz, int fadeIn, int hold, int fadeOut) {
        // forward move is relative and should consider pitch
        return new PacketCameraEffect(TYPE_MOVE, dx, dy, dz, true, true, 0f, fadeIn, hold, fadeOut);
    }

    public static PacketCameraEffect createRotate(String axis, float angle, boolean relative, int fadeIn, int hold, int fadeOut) {
        int type;
        switch (axis) {
            case "x": type = TYPE_ROTATE_X; break;
            case "y": type = TYPE_ROTATE_Y; break;
            default: type = TYPE_ROTATE_Z; break;
        }
        return new PacketCameraEffect(type, angle, 0f, 0f, relative, false, 0f, fadeIn, hold, fadeOut);
    }

    public static void encode(PacketCameraEffect msg, PacketBuffer buf) {
        buf.writeInt(msg.type);
        buf.writeFloat(msg.x);
        buf.writeFloat(msg.y);
        buf.writeFloat(msg.z);
        buf.writeBoolean(msg.relative);
        buf.writeBoolean(msg.usePitch);
        buf.writeFloat(msg.intensity);
        buf.writeInt(msg.fadeIn);
        buf.writeInt(msg.hold);
        buf.writeInt(msg.fadeOut);
    }

    public static PacketCameraEffect decode(PacketBuffer buf) {
        int type = buf.readInt();
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        boolean relative = buf.readBoolean();
        boolean usePitch = buf.readBoolean();
        float intensity = buf.readFloat();
        int fadeIn = buf.readInt();
        int hold = buf.readInt();
        int fadeOut = buf.readInt();
        return new PacketCameraEffect(type, x, y, z, relative, usePitch, intensity, fadeIn, hold, fadeOut);
    }

    public static void handle(PacketCameraEffect msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (msg.type == TYPE_CANCEL) {
                CameraController.get().deactivate();
            } else {
                CameraController.CameraEffect eff = msg.toEffect();
                if (eff != null) {
                    CameraController.get().addEffect(eff);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public CameraController.CameraEffect toEffect() {
        switch (type) {
            case TYPE_SHAKE_POSITIONED:
                return new CameraController.ShakeEffect(false, intensity, fadeIn);
            case TYPE_SHAKE_ROTATIONED:
                return new CameraController.ShakeEffect(true, intensity, fadeIn);
            case TYPE_MOVE:
                return new CameraController.MoveEffect(x, y, z, relative, usePitch, fadeIn, hold, fadeOut);
            case TYPE_ROTATE_X:
                return new CameraController.RotateEffect(CameraController.RotateEffect.Axis.X, x, fadeIn, hold, fadeOut);
            case TYPE_ROTATE_Y:
                return new CameraController.RotateEffect(CameraController.RotateEffect.Axis.Y, x, fadeIn, hold, fadeOut);
            default:
                return new CameraController.RotateEffect(CameraController.RotateEffect.Axis.Z, x, fadeIn, hold, fadeOut);
        }
    }
}
