package com.jom3a.zoomrgy.neoforge;

import com.jom3a.zoomrgy.Platform;
import com.jom3a.zoomrgy.ZoomConfigScreen;
import com.jom3a.zoomrgy.ZoomHandler;
import com.jom3a.zoomrgy.ZoomKeyBindings;
import com.jom3a.zoomrgy.Zoomrgy;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Path;

/** NeoForge entry point: the counterpart to ZoomrgyFabric, doing the same wiring its own way. */
@Mod(value = Zoomrgy.MOD_ID, dist = Dist.CLIENT)
public class ZoomrgyNeoForge {

    public ZoomrgyNeoForge(IEventBus modBus, ModContainer container) {
        Platform.set(new NeoForgePlatform());
        Zoomrgy.init();

        modBus.addListener(this::registerKeyMappings);

        // Key bindings first, so the zoom handler sees this tick's input rather than last tick's.
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class,
            event -> ZoomKeyBindings.onClientTick(net.minecraft.client.Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class,
            event -> ZoomHandler.onClientTick(net.minecraft.client.Minecraft.getInstance()));

        // NeoForge's equivalent of the Mod Menu hook: the gear beside the mod in the mods list.
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (c, parent) -> ZoomConfigScreen.create(parent));
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : ZoomKeyBindings.all()) {
            event.register(mapping);
        }
    }

    private static class NeoForgePlatform implements Platform {
        @Override
        public Path configDir() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public Path gameDir() {
            return FMLPaths.GAMEDIR.get();
        }
    }
}
