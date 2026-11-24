package pl.autosell.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.util.Hand;

/** AutoFood: gdy głód <= próg, je jedzenie trzymane w offhandzie. */
public final class AutoFood {
    private static boolean eating = false;

    private AutoFood() {}

    public static boolean isEating() { return eating; }

    public static void tick(MinecraftClient mc) {
        ModsConfig c = ModsConfig.I;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) { eating = false; return; }
        if (!c.foodEnabled) { stop(mc); return; }

        if (eating) {
            if (p.isUsingItem()) {
                mc.options.useKey.setPressed(true); // trzymaj PPM aż do końca jedzenia
                return;
            }
            stop(mc);
        }

        if (mc.currentScreen != null || p.isUsingItem()) return;
        int food = p.getHungerManager().getFoodLevel();
        if (food > c.foodThreshold || food >= 20) return;

        FoodComponent f = p.getOffHandStack().get(DataComponentTypes.FOOD);
        if (f == null || !p.canConsume(f.canAlwaysEat())) return;

        mc.interactionManager.interactItem(p, Hand.OFF_HAND);
        eating = true;
    }

    private static void stop(MinecraftClient mc) {
        if (eating) {
            mc.options.useKey.setPressed(false);
            eating = false;
        }
    }
}
