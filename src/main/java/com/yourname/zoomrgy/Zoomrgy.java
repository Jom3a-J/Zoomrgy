package com.yourname.zoomrgy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Zoomrgy implements ClientModInitializer {

    public static final String MOD_ID = "zoomrgy";

    @Override
    public void onInitializeClient() {
        ZoomConfig.load();
        ZoomKeyBindings.register();
        ZoomHandler.register();
    }
}
