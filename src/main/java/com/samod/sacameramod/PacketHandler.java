package com.samod.sacameramod;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public class PacketHandler {
    public static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(ExampleMod.MODID, "camera");
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(CHANNEL_NAME, () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
        CHANNEL.registerMessage(0, PacketCameraEffect.class, PacketCameraEffect::encode, PacketCameraEffect::decode, PacketCameraEffect::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, PacketCameraCinematic.class, PacketCameraCinematic::encode, PacketCameraCinematic::decode, PacketCameraCinematic::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, PacketCameraPerspective.class, PacketCameraPerspective::encode, PacketCameraPerspective::decode, PacketCameraPerspective::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
