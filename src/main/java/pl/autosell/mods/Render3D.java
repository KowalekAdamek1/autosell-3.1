package pl.autosell.mods;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Render 3D (świat): pełne wypełnione boxy rud i graczy (przez ściany), tracery od celownika,
 * nametagi 3D. Stare 2D ESP zostaje w starym kodzie – to jest dodatek.
 */
public final class Render3D {
    private Render3D() {}

    public static void init() {
        WorldRenderEvents.LAST.register(Render3D::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ModsConfig c = ModsConfig.I;
        if (mc.player == null || mc.world == null) return;

        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        float td = ctx.tickCounter().getTickDelta(false);
        Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();

        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        boolean any = false;

        // ---------- rudy ----------
        if (c.oreEsp3d) {
            double r2 = (double) c.oreRange * c.oreRange;
            for (OreScan.Hit h : OreScan.hits) {
                double dx = h.x() + 0.5 - cp.x, dy = h.y() + 0.5 - cp.y, dz = h.z() + 0.5 - cp.z;
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                Box b = new Box(h.x(), h.y(), h.z(), h.x() + 1, h.y() + 1, h.z() + 1).offset(-cp.x, -cp.y, -cp.z).expand(0.002);
                if (c.oreFill) addFilled(fill, m, b, withA(h.argb(), c.fillAlpha));
                if (c.oreOutline) addOutline(lines, m, b, withA(h.argb(), 255));
                any = true;
            }
        }

        // ---------- gracze ----------
        Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw()).normalize();
        Vec3d tracerStart = look.multiply(0.25); // środek ekranu (celownik), względem kamery
        java.util.List<NameTag> tags = new java.util.ArrayList<>();

        if (c.playerEsp3d || c.tracers || c.nametags3d) {
            for (AbstractClientPlayerEntity pl : mc.world.getPlayers()) {
                if (pl == mc.player || pl.isSpectator()) continue;
                Vec3d lp = pl.getLerpedPos(td); // interpolacja = płynne tracery/boxy
                double dist = lp.distanceTo(mc.player.getLerpedPos(td));
                if (dist > c.playerRadius) continue;
                int col = distColor(dist);

                Box b = pl.getBoundingBox()
                        .offset(lp.x - pl.getX(), lp.y - pl.getY(), lp.z - pl.getZ())
                        .offset(-cp.x, -cp.y, -cp.z);

                if (c.playerEsp3d) {
                    if (c.playerFill) addFilled(fill, m, b, withA(col, c.fillAlpha));
                    addOutline(lines, m, b, withA(col, 255));
                    any = true;
                }
                if (c.tracers) {
                    Vec3d to = b.getCenter();
                    line(lines, m, tracerStart, to, withA(col, 230));
                    any = true;
                }
                if (c.nametags3d) {
                    tags.add(new NameTag(pl, lp, dist));
                }
            }
        }

        if (any) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();   // przez ściany
            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            BuiltBuffer bf = fill.endNullable();
            if (bf != null) BufferRenderer.drawWithGlobalProgram(bf);

            RenderSystem.lineWidth(2.0f);
            BuiltBuffer bl = lines.endNullable();
            if (bl != null) BufferRenderer.drawWithGlobalProgram(bl);

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }

        if (!tags.isEmpty()) drawNameTags(mc, cam, cp, tags);
    }

    // ---------- nametagi ----------
    private record NameTag(AbstractClientPlayerEntity p, Vec3d pos, double dist) {}

    private static void drawNameTags(MinecraftClient mc, Camera cam, Vec3d cp, java.util.List<NameTag> tags) {
        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        for (NameTag t : tags) {
            String s = t.p.getName().getString()
                    + "  " + String.format("%.1f", t.p.getHealth() + t.p.getAbsorptionAmount()) + "HP"
                    + "  " + (int) t.dist + "m";
            MatrixStack ms = new MatrixStack();
            ms.translate(t.pos.x - cp.x, t.pos.y + t.p.getHeight() + 0.55 - cp.y, t.pos.z - cp.z);
            ms.multiply(cam.getRotation());
            float scale = (float) Math.max(0.025, t.dist * 0.0032); // rośnie z dystansem = czytelny
            ms.scale(scale, -scale, scale);
            float w = tr.getWidth(s);
            tr.draw(Text.literal(s), -w / 2f, 0, distColor(t.dist), false,
                    ms.peek().getPositionMatrix(), vcp, TextRenderer.TextLayerType.SEE_THROUGH, 0x70000000, 0xF000F0);
        }
        vcp.draw();
    }

    // ---------- prymitywy ----------
    private static void v(BufferBuilder bb, Matrix4f m, double x, double y, double z, int argb) {
        bb.vertex(m, (float) x, (float) y, (float) z).color(argb);
    }

    private static void line(BufferBuilder bb, Matrix4f m, Vec3d a, Vec3d b, int argb) {
        v(bb, m, a.x, a.y, a.z, argb);
        v(bb, m, b.x, b.y, b.z, argb);
    }

    public static void addFilled(BufferBuilder bb, Matrix4f m, Box b, int argb) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        // dół
        v(bb, m, x1, y1, z1, argb); v(bb, m, x2, y1, z1, argb); v(bb, m, x2, y1, z2, argb); v(bb, m, x1, y1, z2, argb);
        // góra
        v(bb, m, x1, y2, z1, argb); v(bb, m, x1, y2, z2, argb); v(bb, m, x2, y2, z2, argb); v(bb, m, x2, y2, z1, argb);
        // północ
        v(bb, m, x1, y1, z1, argb); v(bb, m, x1, y2, z1, argb); v(bb, m, x2, y2, z1, argb); v(bb, m, x2, y1, z1, argb);
        // południe
        v(bb, m, x1, y1, z2, argb); v(bb, m, x2, y1, z2, argb); v(bb, m, x2, y2, z2, argb); v(bb, m, x1, y2, z2, argb);
        // zachód
        v(bb, m, x1, y1, z1, argb); v(bb, m, x1, y1, z2, argb); v(bb, m, x1, y2, z2, argb); v(bb, m, x1, y2, z1, argb);
        // wschód
        v(bb, m, x2, y1, z1, argb); v(bb, m, x2, y2, z1, argb); v(bb, m, x2, y2, z2, argb); v(bb, m, x2, y1, z2, argb);
    }

    public static void addOutline(BufferBuilder bb, Matrix4f m, Box b, int argb) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        double[][] e = {
                {x1, y1, z1, x2, y1, z1}, {x2, y1, z1, x2, y1, z2}, {x2, y1, z2, x1, y1, z2}, {x1, y1, z2, x1, y1, z1},
                {x1, y2, z1, x2, y2, z1}, {x2, y2, z1, x2, y2, z2}, {x2, y2, z2, x1, y2, z2}, {x1, y2, z2, x1, y2, z1},
                {x1, y1, z1, x1, y2, z1}, {x2, y1, z1, x2, y2, z1}, {x2, y1, z2, x2, y2, z2}, {x1, y1, z2, x1, y2, z2}
        };
        for (double[] s : e) {
            v(bb, m, s[0], s[1], s[2], argb);
            v(bb, m, s[3], s[4], s[5], argb);
        }
    }

    private static int withA(int argb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (argb & 0xFFFFFF);
    }

    /** blisko = czerwony, daleko = zielony */
    public static int distColor(double d) {
        float t = (float) Math.max(0, Math.min(1, d / 64.0));
        int r = (int) (255 * (1 - t) + 80 * t);
        int g = (int) (70 * (1 - t) + 255 * t);
        int b = (int) (70 * (1 - t) + 120 * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
