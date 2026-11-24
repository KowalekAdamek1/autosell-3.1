package pl.autosell.mods;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * "Mod na Gencasha": kopanie obszarowe NxN (3,5,7,9,10) w płaszczyźnie prostopadłej do
 * ściany, na którą patrzysz. Prędkość = delay (ticki) + bloków na serię. Tryb AFK trzyma LPM.
 */
public final class GencashMiner {
    private static int cooldown = 0;
    private static boolean afkHolding = false;

    private GencashMiner() {}

    public static void tick(MinecraftClient mc) {
        ModsConfig c = ModsConfig.I;
        ClientPlayerEntity p = mc.player;
        if (!c.gcEnabled || p == null || mc.world == null || mc.interactionManager == null) {
            releaseAfk(mc);
            return;
        }
        if (mc.currentScreen != null) return;

        if (c.gcAfk && !AutoFood.isEating()) {
            mc.options.attackKey.setPressed(true);
            afkHolding = true;
        } else {
            releaseAfk(mc);
        }

        if (!mc.options.attackKey.isPressed() || AutoFood.isEating()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        if (--cooldown > 0) return;
        cooldown = c.gcDelayTicks;

        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        if (nh == null) return;

        Direction side = hit.getSide();
        BlockPos center = hit.getBlockPos();
        int n = c.gcSize;
        int lo = -((n - 1) / 2);   // 3 -> -1, 10 -> -4
        int hi = n / 2;            // 3 ->  1, 10 ->  5

        // dwie osie leżące w płaszczyźnie ściany
        Direction.Axis ax = side.getAxis();
        int[] u, v;
        switch (ax) {
            case Y -> { u = new int[]{1, 0, 0}; v = new int[]{0, 0, 1}; }
            case X -> { u = new int[]{0, 1, 0}; v = new int[]{0, 0, 1}; }
            default -> { u = new int[]{1, 0, 0}; v = new int[]{0, 1, 0}; }
        }

        Vec3d eye = p.getEyePos();
        double reach2 = (double) c.gcReach * c.gcReach;
        int broken = 0;

        for (int a = lo; a <= hi && broken < c.gcBlocksPerTick; a++) {
            for (int b = lo; b <= hi && broken < c.gcBlocksPerTick; b++) {
                BlockPos pos = center.add(u[0] * a + v[0] * b, u[1] * a + v[1] * b, u[2] * a + v[2] * b);
                BlockState st = mc.world.getBlockState(pos);
                if (st.isAir() || !st.getFluidState().isEmpty()) continue;
                if (st.getHardness(mc.world, pos) < 0) continue; // bedrock itp.
                if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > reach2) continue;

                nh.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, side));
                nh.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, side));
                broken++;
            }
        }
        if (broken > 0) p.swingHand(Hand.MAIN_HAND);
    }

    private static void releaseAfk(MinecraftClient mc) {
        if (afkHolding) {
            mc.options.attackKey.setPressed(false);
            afkHolding = false;
        }
    }
}
