package pl.autosell.mods;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Jedyny punkt wejścia: wywołaj Modules.init() na końcu AutoSellMod.onInitializeClient(). */
public final class Modules {
    private Modules() {}

    public static void init() {
        ModsConfig.load();

        KeyBinding open = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "AutoSell: Mods (Gencash/AutoFood/ESP)", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "AutoSell"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (open.wasPressed()) mc.setScreen(new ModsScreen());
            GencashMiner.tick(mc);
            AutoFood.tick(mc);
            OreScan.tick(mc);
        });

        Render3D.init();
    }
}
