package com.samod.sacameramod;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void init() {
        // client init
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        CameraKeyBindings.register();
    }
}
