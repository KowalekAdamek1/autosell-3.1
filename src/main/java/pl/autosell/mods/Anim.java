package pl.autosell.mods;

import java.util.HashMap;
import java.util.Map;

/** Animacje: easingi, sprężyna (spring) i animowane wartości po kluczu – do użycia w GUI. */
public final class Anim {
    private Anim() {}

    public static float easeOutCubic(float t) { t = clamp(t); float u = 1 - t; return 1 - u * u * u; }
    public static float easeInOutCubic(float t) { t = clamp(t); return t < .5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }
    public static float easeOutBack(float t) {
        t = clamp(t); float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }
    public static float clamp(float v) { return v < 0 ? 0 : Math.min(v, 1); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Wygładzone dążenie do celu, niezależne od FPS (dt w sekundach). */
    public static float approach(float cur, float target, float dt, float speed) {
        float k = 1f - (float) Math.exp(-speed * dt);
        return cur + (target - cur) * k;
    }

    /** Sprężyna z lekkim "odbiciem" (damped spring). */
    public static final class Spring {
        public float value, velocity;
        public Spring(float start) { value = start; }
        public void update(float target, float dt, float stiffness, float damping) {
            float f = -stiffness * (value - target) - damping * velocity;
            velocity += f * dt;
            value += velocity * dt;
        }
    }

    /** Mapa animowanych floatów po kluczu (hover, stan togglea, itp.). */
    public static final class Bank {
        private final Map<String, Float> m = new HashMap<>();
        public float get(String key, float target, float dt, float speed) {
            float cur = m.getOrDefault(key, target);
            cur = approach(cur, target, dt, speed);
            m.put(key, cur);
            return cur;
        }
    }
}
