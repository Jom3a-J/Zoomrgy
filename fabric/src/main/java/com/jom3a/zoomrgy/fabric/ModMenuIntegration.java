package com.jom3a.zoomrgy.fabric;

import com.jom3a.zoomrgy.ZoomConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Points Mod Menu's gear icon at the shared config screen. */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ZoomConfigScreen::create;
    }
}
