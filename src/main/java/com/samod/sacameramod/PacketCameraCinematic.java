package com.samod.sacameramod;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketCameraCinematic {
    private final int duration;
    private final String payload;

    public PacketCameraCinematic(int duration, String payload) {
        this.duration = duration;
        this.payload = payload;
    }

    public static PacketCameraCinematic create(int duration, String payload) {
        return new PacketCameraCinematic(duration, payload);
    }

    public static void encode(PacketCameraCinematic msg, PacketBuffer buf) {
        buf.writeInt(msg.duration);
        buf.writeUtf(msg.payload);
    }

    public static PacketCameraCinematic decode(PacketBuffer buf) {
        int duration = buf.readInt();
        String payload = buf.readUtf(32767);
        return new PacketCameraCinematic(duration, payload);
    }

    public static void handle(PacketCameraCinematic msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CameraController.get().startCinematic(msg.duration, msg.payload);
        });
        ctx.get().setPacketHandled(true);
    }
}
