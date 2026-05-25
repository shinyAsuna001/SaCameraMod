package com.samod.sacameramod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SaCameraMod.MODID)
public class SaCameraMod {
    public static final String MODID = "sacameramod";

    public SaCameraMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        PacketHandler.register();
    }
}
