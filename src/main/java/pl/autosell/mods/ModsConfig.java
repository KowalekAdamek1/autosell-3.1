package pl.autosell.mods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Osobny config nowych modułów: config/autosell-modules.json (nie rusza starego AutoSellConfig). */
public class ModsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autosell-modules.json");
    public static ModsConfig I = new ModsConfig();

    // ---- Gencash Miner ----
    public boolean gcEnabled = false;
    public int gcSize = 3;            // 3, 5, 7, 9, 10
    public int gcDelayTicks = 2;      // 0..20  (0 = każdy tick)
    public int gcBlocksPerTick = 9;   // 1..100 ile bloków na jedną "serię"
    public float gcReach = 5.0f;      // 2..8 kratek
    public boolean gcAfk = false;     // sam trzyma LPM

    // ---- AutoFood ----
    public boolean foodEnabled = false;
    public int foodThreshold = 7;     // je gdy głód <= tej wartości (0..19)

    // ---- ESP 3D ----
    public boolean oreEsp3d = true;
    public boolean oreFill = true;
    public boolean oreOutline = true;
    public int oreRange = 24;         // 8..48
    public boolean playerEsp3d = true;
    public boolean playerFill = true;
    public boolean nametags3d = true;
    public boolean tracers = true;
    public int playerRadius = 64;
    public int fillAlpha = 70;        // 10..200

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                ModsConfig c = GSON.fromJson(Files.readString(FILE), ModsConfig.class);
                if (c != null) I = c;
            }
        } catch (Exception ignored) {}
        I.clamp();
    }

    public void clamp() {
        if (gcSize != 3 && gcSize != 5 && gcSize != 7 && gcSize != 9 && gcSize != 10) gcSize = 3;
        gcDelayTicks = Math.max(0, Math.min(20, gcDelayTicks));
        gcBlocksPerTick = Math.max(1, Math.min(100, gcBlocksPerTick));
        gcReach = Math.max(2f, Math.min(8f, gcReach));
        foodThreshold = Math.max(0, Math.min(19, foodThreshold));
        oreRange = Math.max(8, Math.min(48, oreRange));
        playerRadius = Math.max(8, Math.min(256, playerRadius));
        fillAlpha = Math.max(10, Math.min(200, fillAlpha));
    }

    public void save() {
        try {
            clamp();
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (Exception ignored) {}
    }
}
