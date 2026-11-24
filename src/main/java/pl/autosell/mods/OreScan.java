package pl.autosell.mods;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Skanuje rudy wokół gracza co ~1 s. Wyniki czyta Render3D. */
public final class OreScan {
    public record Hit(int x, int y, int z, int argb) {}

    public static volatile List<Hit> hits = List.of();
    private static int timer = 0;

    private OreScan() {}

    public static void tick(MinecraftClient mc) {
        ModsConfig c = ModsConfig.I;
        if (!c.oreEsp3d || mc.player == null || mc.world == null) { hits = List.of(); return; }
        if (--timer > 0) return;
        timer = 20;

        int r = c.oreRange;
        BlockPos o = mc.player.getBlockPos();
        List<Hit> out = new ArrayList<>();
        BlockPos.Mutable m = new BlockPos.Mutable();
        int minY = mc.world.getBottomY(), maxY = mc.world.getTopYInclusive();
        for (int x = o.getX() - r; x <= o.getX() + r; x++)
            for (int z = o.getZ() - r; z <= o.getZ() + r; z++)
                for (int y = Math.max(minY, o.getY() - r); y <= Math.min(maxY, o.getY() + r); y++) {
                    m.set(x, y, z);
                    BlockState s = mc.world.getBlockState(m);
                    if (s.isAir() || !s.isIn(ConventionalBlockTags.ORES)) continue;
                    out.add(new Hit(x, y, z, colorOf(Registries.BLOCK.getId(s.getBlock()).getPath())));
                }
        hits = out;
    }

    private static int colorOf(String id) {
        if (id.contains("diamond")) return 0xFF33E6E6;
        if (id.contains("emerald")) return 0xFF22DD55;
        if (id.contains("gold")) return 0xFFFFD233;
        if (id.contains("iron")) return 0xFFE8B898;
        if (id.contains("copper")) return 0xFFE07A4A;
        if (id.contains("redstone")) return 0xFFFF3333;
        if (id.contains("lapis")) return 0xFF3355FF;
        if (id.contains("coal")) return 0xFF999999;
        if (id.contains("quartz")) return 0xFFF2EAE0;
        if (id.contains("debris")) return 0xFF8B5A3C;
        return 0xFFFFFFFF;
    }
}
