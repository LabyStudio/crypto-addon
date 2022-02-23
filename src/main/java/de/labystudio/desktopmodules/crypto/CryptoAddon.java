package de.labystudio.desktopmodules.crypto;

import de.labystudio.desktopmodules.core.addon.Addon;
import de.labystudio.desktopmodules.crypto.modules.CryptoModule;

public class CryptoAddon extends Addon {

    @Override
    public void onInitialize() throws Exception {
        this.registerModule(CryptoModule.class);
    }

    @Override
    public void onEnable() throws Exception {

    }

    @Override
    public void onDisable() throws Exception {

    }
}
