package pl.autosell.mods;

import net.fabricmc.api.ClientModInitializer;

/** Osobny mod-dodatek: działa obok starego autosell-2.0.0.jar, nie wymaga zmian w jego kodzie. */
public class ModsEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Modules.init();
    }
}
