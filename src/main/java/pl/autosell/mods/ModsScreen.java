package pl.autosell.mods;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Nowy ekran z animacjami: Gencash Miner, AutoFood, ESP 3D.
 * Immediate-mode (jak Twój MainScreen): wszystko rysowane i obsługiwane w render().
 */
public class ModsScreen extends Screen {
    private static final int ACCENT = 0xFF6C8CFF, BG = 0xF2101218, CARD = 0xFF181B24, CARD_HI = 0xFF222633,
            TEXT = 0xFFE8EAF2, DIM = 0xFF8A90A6, OFF = 0xFF3A3F52, GOOD = 0xFF4ADE80;

    private final ModsConfig c = ModsConfig.I;
    private final Anim.Bank anim = new Anim.Bank();
    private long lastNs = System.nanoTime();
    private float dt, open = 0f, scroll = 0f, scrollTarget = 0f;
    private int mx, my, px, py, pw, ph, cy, contentTop, contentBottom;
    private boolean click, down, dirty;
    private String activeSlider = null;
    private float totalH = 0;

    public ModsScreen() { super(Text.literal("AutoSell Mods")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        dt = Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        mx = mouseX; my = mouseY;
        open = Anim.approach(open, 1f, dt, 9f);
        float e = Anim.easeOutBack(Math.min(1f, open));

        // przyciemnione tło (fade)
        g.fill(0, 0, width, height, ((int) (150 * Math.min(1f, open)) << 24));

        pw = Math.min(360, width - 24);
        ph = Math.min(400, height - 24);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        g.getMatrices().push();
        g.getMatrices().translate(width / 2f, height / 2f, 0);
        float sc = 0.92f + 0.08f * e;
        g.getMatrices().scale(sc, sc, 1f);
        g.getMatrices().translate(-width / 2f, -height / 2f, 0);

        // panel
        g.fill(px, py, px + pw, py + ph, BG);
        g.fill(px, py, px + pw, py + 2, ACCENT);
        g.drawText(textRenderer, "AutoSell  •  Mods", px + 12, py + 10, TEXT, true);
        g.drawText(textRenderer, "Gencash / AutoFood / ESP 3D", px + 12, py + 22, DIM, false);

        contentTop = py + 38;
        contentBottom = py + ph - 8;
        scroll = Anim.approach(scroll, scrollTarget, dt, 16f);
        cy = contentTop - Math.round(scroll);
        float startY = cy;

        g.enableScissor(px, contentTop, px + pw, contentBottom);

        // ===== GENCASH =====
        section(g, "Mod na Gencasha (Gencash Miner)");
        boolean b = toggle(g, "gc.on", "Włączony", c.gcEnabled); if (b != c.gcEnabled) { c.gcEnabled = b; dirty = true; }
        int sz = chips(g, "gc.size", "Rozmiar kopania", new int[]{3, 5, 7, 9, 10}, c.gcSize);
        if (sz != c.gcSize) { c.gcSize = sz; dirty = true; }
        label(g, c.gcSize + "x" + c.gcSize + "  (" + (c.gcSize * c.gcSize) + " bloków naraz)");
        c.gcDelayTicks = Math.round(slider(g, "gc.delay", "Opóźnienie (ticki)", c.gcDelayTicks, 0, 20, "%.0f"));
        c.gcBlocksPerTick = Math.round(slider(g, "gc.bpt", "Bloków na serię", c.gcBlocksPerTick, 1, 100, "%.0f"));
        c.gcReach = slider(g, "gc.reach", "Zasięg (range)", c.gcReach, 2, 8, "%.1f");
        b = toggle(g, "gc.afk", "Tryb AFK (sam trzyma LPM)", c.gcAfk); if (b != c.gcAfk) { c.gcAfk = b; dirty = true; }

        // ===== AUTOFOOD =====
        section(g, "AutoFood");
        b = toggle(g, "food.on", "Włączony (jedzenie w offhandzie)", c.foodEnabled); if (b != c.foodEnabled) { c.foodEnabled = b; dirty = true; }
        c.foodThreshold = Math.round(slider(g, "food.thr", "Jedz gdy głód ≤", c.foodThreshold, 0, 19, "%.0f"));

        // ===== ESP 3D =====
        section(g, "ESP 3D");
        b = toggle(g, "ore.on", "ESP rud 3D (pełny box)", c.oreEsp3d); if (b != c.oreEsp3d) { c.oreEsp3d = b; dirty = true; }
        b = toggle(g, "ore.fill", "  Wypełnienie 3D", c.oreFill); if (b != c.oreFill) { c.oreFill = b; dirty = true; }
        b = toggle(g, "ore.out", "  Obrys", c.oreOutline); if (b != c.oreOutline) { c.oreOutline = b; dirty = true; }
        c.oreRange = Math.round(slider(g, "ore.rng", "Zasięg rud", c.oreRange, 8, 48, "%.0f"));
        b = toggle(g, "pl.on", "ESP graczy 3D", c.playerEsp3d); if (b != c.playerEsp3d) { c.playerEsp3d = b; dirty = true; }
        b = toggle(g, "pl.fill", "  Wypełnienie 3D", c.playerFill); if (b != c.playerFill) { c.playerFill = b; dirty = true; }
        b = toggle(g, "pl.tag", "Nametagi 3D", c.nametags3d); if (b != c.nametags3d) { c.nametags3d = b; dirty = true; }
        b = toggle(g, "pl.tr", "Tracery (od celownika)", c.tracers); if (b != c.tracers) { c.tracers = b; dirty = true; }
        c.playerRadius = Math.round(slider(g, "pl.rad", "Promień graczy", c.playerRadius, 8, 256, "%.0f"));
        c.fillAlpha = Math.round(slider(g, "fill.a", "Przezroczystość wypełnienia", c.fillAlpha, 10, 200, "%.0f"));

        g.disableScissor();
        totalH = cy - startY + 10;
        float maxScroll = Math.max(0, totalH - (contentBottom - contentTop));
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));

        // pasek przewijania
        if (maxScroll > 0) {
            int trackH = contentBottom - contentTop;
            int th = Math.max(20, (int) (trackH * (trackH / totalH)));
            int ty = contentTop + (int) ((trackH - th) * (scroll / maxScroll));
            g.fill(px + pw - 5, ty, px + pw - 2, ty + th, ACCENT);
        }

        g.getMatrices().pop();
        click = false;
    }

    // ---------------- widgety ----------------
    private void section(DrawContext g, String title) {
        cy += 8;
        g.drawText(textRenderer, title, px + 12, cy, ACCENT, true);
        g.fill(px + 12, cy + 11, px + pw - 12, cy + 12, OFF);
        cy += 18;
    }

    private void label(DrawContext g, String s) {
        g.drawText(textRenderer, s, px + 14, cy, GOOD, false);
        cy += 14;
    }

    private boolean inClip(int y1, int y2) { return my >= contentTop && my <= contentBottom && my >= y1 && my <= y2; }

    private boolean toggle(DrawContext g, String id, String text, boolean val) {
        int x = px + 12, w = pw - 24, h = 20, y = cy;
        boolean hov = mx >= x && mx <= x + w && inClip(y, y + h);
        float hv = anim.get(id + ".h", hov ? 1 : 0, dt, 14f);
        float on = anim.get(id + ".o", val ? 1 : 0, dt, 12f);

        g.fill(x, y, x + w, y + h, mixC(CARD, CARD_HI, hv));
        g.drawText(textRenderer, text, x + 8, y + 6, TEXT, false);

        int sw = 30, sh = 12, sx = x + w - sw - 8, sy = y + 4;
        g.fill(sx, sy, sx + sw, sy + sh, mixC(OFF, ACCENT, on));
        int kx = sx + 2 + Math.round((sw - sh) * on);
        g.fill(kx, sy + 2, kx + 8, sy + sh - 2, 0xFFFFFFFF);

        cy += h + 3;
        if (click && hov) return !val;
        return val;
    }

    private float slider(DrawContext g, String id, String text, float val, float min, float max, String fmt) {
        int x = px + 12, w = pw - 24, h = 30, y = cy;
        int tx = x + 8, tw = w - 16, ty = y + 21;
        boolean hov = mx >= x && mx <= x + w && inClip(y, y + h);
        float hv = anim.get(id + ".h", hov || id.equals(activeSlider) ? 1 : 0, dt, 14f);

        if (click && hov) activeSlider = id;
        if (!down) activeSlider = null;
        if (id.equals(activeSlider)) {
            float t = Math.max(0, Math.min(1, (mx - tx) / (float) tw));
            val = min + (max - min) * t;
            dirty = true;
        }
        float t = (val - min) / (max - min);
        float at = anim.get(id + ".v", t, dt, 22f);

        g.fill(x, y, x + w, y + h, mixC(CARD, CARD_HI, hv));
        g.drawText(textRenderer, text, x + 8, y + 5, TEXT, false);
        String vs = String.format(fmt, val);
        g.drawText(textRenderer, vs, x + w - 8 - textRenderer.getWidth(vs), y + 5, ACCENT, false);
        g.fill(tx, ty, tx + tw, ty + 3, OFF);
        g.fill(tx, ty, tx + Math.round(tw * at), ty + 3, ACCENT);
        int kx = tx + Math.round(tw * at);
        int ks = 4 + Math.round(2 * hv);
        g.fill(kx - ks / 2, ty + 1 - ks / 2, kx + ks / 2 + 1, ty + 2 + ks / 2, 0xFFFFFFFF);

        cy += h + 3;
        return val;
    }

    private int chips(DrawContext g, String id, String text, int[] opts, int cur) {
        int x = px + 12, w = pw - 24;
        g.drawText(textRenderer, text, x + 2, cy + 2, DIM, false);
        cy += 14;
        int gap = 4, cw = (w - gap * (opts.length - 1)) / opts.length, h = 20;
        int result = cur;
        for (int i = 0; i < opts.length; i++) {
            int cx = x + i * (cw + gap);
            boolean hov = mx >= cx && mx <= cx + cw && inClip(cy, cy + h);
            float sel = anim.get(id + "." + opts[i], opts[i] == cur ? 1 : 0, dt, 14f);
            float hv = anim.get(id + "." + opts[i] + "h", hov ? 1 : 0, dt, 14f);
            int col = mixC(mixC(CARD, CARD_HI, hv), ACCENT, sel);
            g.fill(cx, cy, cx + cw, cy + h, col);
            String s = opts[i] + "x" + opts[i];
            g.drawText(textRenderer, s, cx + (cw - textRenderer.getWidth(s)) / 2, cy + 6, TEXT, false);
            if (click && hov) result = opts[i];
        }
        cy += h + 4;
        return result;
    }

    private static int mixC(int a, int b, float t) {
        t = Anim.clamp(t);
        int ar = a >> 24 & 255, rr = a >> 16 & 255, gg = a >> 8 & 255, bb = a & 255;
        int br = b >> 24 & 255, r2 = b >> 16 & 255, g2 = b >> 8 & 255, b2 = b & 255;
        return ((int) (ar + (br - ar) * t) << 24) | ((int) (rr + (r2 - rr) * t) << 16)
                | ((int) (gg + (g2 - gg) * t) << 8) | (int) (bb + (b2 - bb) * t);
    }

    // ---------------- input ----------------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { click = true; down = true; }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) { down = false; activeSlider = null; }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return true; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        scrollTarget -= (float) (v * 28);
        return true;
    }

    @Override
    public void close() {
        if (dirty) c.save();
        super.close();
    }
}
