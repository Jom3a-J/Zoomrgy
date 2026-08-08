package com.jom3a.zoomrgy.fabric;

import com.jom3a.zoomrgy.Platform;
import com.jom3a.zoomrgy.ZoomHandler;
import com.jom3a.zoomrgy.ZoomKeyBindings;
import com.jom3a.zoomrgy.Zoomrgy;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

import java.nio.file.Path;

/** Fabric entry point: sets the platform, builds the shared state, and wires up the tick. */
@Environment(EnvType.CLIENT)
public class ZoomrgyFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Platform.set(new FabricPlatform());
        Zoomrgy.init();

        for (KeyMapping mapping : ZoomKeyBindings.all()) {
            KeyMappingHelper.registerKeyMapping(mapping);
        }

        // Key bindings first, so the zoom handler sees this tick's input rather than last tick's.
        ClientTickEvents.END_CLIENT_TICK.register(ZoomKeyBindings::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(ZoomHandler::onClientTick);
    }

    private static class FabricPlatform implements Platform {
        @Override
        public Path configDir() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public Path gameDir() {
            return FabricLoader.getInstance().getGameDir();
        }
    }
}
