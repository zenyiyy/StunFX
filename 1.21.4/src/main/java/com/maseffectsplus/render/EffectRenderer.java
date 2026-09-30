package com.maseffectsplus.render;

import com.maseffectsplus.config.EffectConfig;
import com.maseffectsplus.config.EffectStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Random;

public class EffectRenderer {
    private static Vec3d cameraPos = Vec3d.ZERO;
    private static final float BLACK_FLASH_BASE_SCALE = 0.7f;

    /** Alpha-blended, position+color, QUADS layer (black needs real alpha blending, additive layers can't draw it). */
    private static RenderLayer getLayer() {
        return RenderLayer.getDebugQuads();
    }

    public static void render(RenderTickCounter tickCounter, Camera camera, Matrix4f worldPositionMatrix) {
        try {
            List<ActiveEffect> effects = EffectManager.getActiveEffects();
            if (effects.isEmpty()) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return;

            float tickDelta = tickCounter != null ? tickCounter.getTickDelta(true) : 1.0f;
            cameraPos = camera.getPos();

            MatrixStack matrices = new MatrixStack();
            Matrix4f viewMat = matrices.peek().getPositionMatrix();
            // Use the exact matrix the world was rendered with (rotation incl. bobbing), then go camera-relative
            viewMat.set(worldPositionMatrix);
            viewMat.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);

            RenderLayer layer = getLayer();
            VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
            VertexConsumer consumer = consumers.getBuffer(layer);

            for (ActiveEffect effect : effects) {
                renderSingleEffect(matrices, consumer, effect, tickDelta);
            }

            consumers.draw(layer);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void renderSingleEffect(MatrixStack matrices, VertexConsumer consumer, ActiveEffect effect, float tickDelta) {
        Vec3d pos = effect.getPosition(tickDelta);
        EffectConfig config = effect.config;
        float progress = effect.getProgress(tickDelta);
        float alpha = effect.getAlpha(tickDelta);

        if (alpha <= 0.001f) return;

        matrices.push();
        Matrix4f mat = matrices.peek().getPositionMatrix();
        mat.translate((float) pos.x, (float) pos.y, (float) pos.z);

        if (config.style == EffectStyle.BLACK_FLASH) {
            // Camera position in the effect's local (world-aligned) space, for camera-facing ribbons
            // Size is fixed in the world and set by the "Size" slider in the Stunslam settings
            float s = BLACK_FLASH_BASE_SCALE * Math.max(0.05f, config.scale);
            mat.scale(s);
            float cx = (float) (cameraPos.x - pos.x) / s;
            float cy = (float) (cameraPos.y - pos.y) / s;
            float cz = (float) (cameraPos.z - pos.z) / s;
            renderBlackFlash(mat, consumer, effect, tickDelta, alpha, cx, cy, cz);
            matrices.pop();
            return;
        }

        float spinAngle = (effect.age + tickDelta) * (config.spin / 20.0f);
        mat.rotateY((float) Math.toRadians(spinAngle));

        float currentRadius = MathHelper.lerp(progress, config.startRadius, config.endRadius);

        float r = config.red;
        float g = config.green;
        float b = config.blue;
        if (config.rainbow) {
            float hue = ((effect.age + tickDelta) * 5.0f) % 360.0f / 360.0f;
            int rgb = MathHelper.hsvToRgb(hue, 0.9f, 1.0f);
            r = ((rgb >> 16) & 0xFF) / 255.0f;
            g = ((rgb >> 8) & 0xFF) / 255.0f;
            b = (rgb & 0xFF) / 255.0f;
        }

        if (config.style == EffectStyle.DOME) {
            renderDome(mat, consumer, config, currentRadius, r, g, b, alpha);
        } else if (config.style == EffectStyle.PILLAR) {
            renderPillar(mat, consumer, config, currentRadius, r, g, b, alpha);
        } else if (config.style == EffectStyle.RING) {
            renderSlam(mat, consumer, effect, tickDelta, r, g, b, alpha);
        } else {
            renderRings(mat, consumer, config, currentRadius, r, g, b, alpha);
        }

        matrices.pop();
    }

    /**
     * Ground shockwave for a normal mace hit: three staggered expanding rings with a soft inner gradient,
     * a bright rim and upright shards shooting up along the wavefront.
     */
    private static void renderSlam(Matrix4f m, VertexConsumer c, ActiveEffect effect, float tickDelta,
                                   float r, float g, float b, float alpha) {
        EffectConfig cfg = effect.config;
        float t = effect.age + tickDelta;
        float life = effect.maxAge;
        int corners = Math.max(24, cfg.corners);
        float endR = Math.max(cfg.endRadius, 3.0f) * 1.1f;
        float startR = cfg.startRadius;

        // bright rim colour: use the secondary colour so a dark body still gets a glowing edge
        float rimR = Math.max(cfg.secondaryRed, r), rimG = Math.max(cfg.secondaryGreen, g), rimB = Math.max(cfg.secondaryBlue, b);
        // make sure the body is visible even for near-black configs
        float bodyR = r, bodyG = g, bodyB = b;

        for (int ring = 0; ring < 3; ring++) {
            float delay = ring * 2.5f;
            float p = (t - delay) / Math.max(1.0f, life - delay - 2.0f);
            if (p <= 0.0f || p >= 1.0f) continue;
            float e = 1.0f - (float) Math.pow(1.0f - p, 3.0); // ease-out
            float rad = MathHelper.lerp(e, startR, endR * (1.0f - ring * 0.18f));
            float w = Math.max(0.15f, cfg.thickness * (2.2f - 1.4f * p)) * (1.0f - ring * 0.15f);
            float a = alpha * (1.0f - p * p) * (1.0f - ring * 0.2f);
            float y = 0.04f + ring * 0.01f;

            float[] rr = {Math.max(0.0f, rad - w), rad - w * 0.12f, rad, rad + w * 0.12f};
            float[][] col = {
                {bodyR, bodyG, bodyB, 0.0f},
                {bodyR, bodyG, bodyB, a * 0.9f},
                {rimR, rimG, rimB, a},
                {rimR, rimG, rimB, 0.0f}
            };
            for (int band = 0; band < 3; band++) {
                for (int i = 0; i < corners; i++) {
                    float a1 = (float) (i * 2 * Math.PI / corners);
                    float a2 = (float) ((i + 1) * 2 * Math.PI / corners);
                    float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                    float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
                    v(m, c, rr[band] * c1, y, rr[band] * s1, col[band][0], col[band][1], col[band][2], col[band][3]);
                    v(m, c, rr[band + 1] * c1, y, rr[band + 1] * s1, col[band + 1][0], col[band + 1][1], col[band + 1][2], col[band + 1][3]);
                    v(m, c, rr[band + 1] * c2, y, rr[band + 1] * s2, col[band + 1][0], col[band + 1][1], col[band + 1][2], col[band + 1][3]);
                    v(m, c, rr[band] * c2, y, rr[band] * s2, col[band][0], col[band][1], col[band][2], col[band][3]);
                }
            }

            // upright shards along the first two wavefronts
            if (ring < 2) {
                Random rng = new Random(System.identityHashCode(effect) * 17L + ring);
                int shards = 22;
                for (int i = 0; i < shards; i++) {
                    float ang = (float) (i * 2 * Math.PI / shards) + rng.nextFloat() * 0.25f;
                    float hgt = (0.5f + rng.nextFloat() * 0.9f) * (1.0f - p);
                    float sw = 0.07f + rng.nextFloat() * 0.06f;
                    float px = rad * (float) Math.cos(ang), pz = rad * (float) Math.sin(ang);
                    // tangent direction for the shard's width
                    float tx = -(float) Math.sin(ang) * sw, tz = (float) Math.cos(ang) * sw;
                    float sa = a * 0.95f;
                    v(m, c, px - tx, y, pz - tz, rimR, rimG, rimB, sa);
                    v(m, c, px + tx, y, pz + tz, rimR, rimG, rimB, sa);
                    v(m, c, px + tx * 0.15f, y + hgt, pz + tz * 0.15f, bodyR, bodyG, bodyB, 0.0f);
                    v(m, c, px - tx * 0.15f, y + hgt, pz - tz * 0.15f, bodyR, bodyG, bodyB, 0.0f);
                }
            }
        }
    }

    // ------------------------------------------------------------------ basic helpers

    private static void v(Matrix4f m, VertexConsumer c, float x, float y, float z, float r, float g, float b, float a) {
        c.vertex(m, x, y, z).color(r, g, b, a);
    }

    private static void renderRings(Matrix4f mat, VertexConsumer c, EffectConfig config, float radius, float r, float g, float b, float a) {
        int corners = Math.max(12, config.corners);
        int rings = Math.max(1, config.rings);
        float thickness = Math.max(0.05f, config.thickness);

        for (int ring = 0; ring < rings; ring++) {
            float outer = radius - (ring * thickness * 0.8f);
            if (outer <= 0) break;
            float inner = Math.max(0, outer - thickness);

            for (int i = 0; i < corners; i++) {
                float a1 = (float) (i * 2 * Math.PI / corners);
                float a2 = (float) ((i + 1) * 2 * Math.PI / corners);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);

                v(mat, c, inner * c1, 0, inner * s1, r, g, b, a);
                v(mat, c, outer * c1, 0, outer * s1, r, g, b, a * 0.4f);
                v(mat, c, outer * c2, 0, outer * s2, r, g, b, a * 0.4f);
                v(mat, c, inner * c2, 0, inner * s2, r, g, b, a);
            }
        }
    }

    private static void renderDome(Matrix4f mat, VertexConsumer c, EffectConfig config, float radius, float r, float g, float b, float a) {
        int corners = Math.max(12, config.corners);
        int rings = Math.max(3, config.rings);

        for (int j = 0; j < rings; j++) {
            float phi1 = (float) (j * (Math.PI / 2.0) / rings);
            float phi2 = (float) ((j + 1) * (Math.PI / 2.0) / rings);
            float y1 = (float) Math.sin(phi1) * config.height;
            float r1 = (float) Math.cos(phi1) * radius;
            float y2 = (float) Math.sin(phi2) * config.height;
            float r2 = (float) Math.cos(phi2) * radius;

            for (int i = 0; i < corners; i++) {
                float t1 = (float) (i * 2 * Math.PI / corners);
                float t2 = (float) ((i + 1) * 2 * Math.PI / corners);

                v(mat, c, r1 * (float) Math.cos(t1), y1, r1 * (float) Math.sin(t1), r, g, b, a);
                v(mat, c, r2 * (float) Math.cos(t1), y2, r2 * (float) Math.sin(t1), r, g, b, a * 0.6f);
                v(mat, c, r2 * (float) Math.cos(t2), y2, r2 * (float) Math.sin(t2), r, g, b, a * 0.6f);
                v(mat, c, r1 * (float) Math.cos(t2), y1, r1 * (float) Math.sin(t2), r, g, b, a);
            }
        }
    }

    private static void renderPillar(Matrix4f mat, VertexConsumer c, EffectConfig config, float radius, float r, float g, float b, float a) {
        int corners = Math.max(12, config.corners);
        float h = config.height;

        for (int i = 0; i < corners; i++) {
            float a1 = (float) (i * 2 * Math.PI / corners);
            float a2 = (float) ((i + 1) * 2 * Math.PI / corners);
            float x1 = radius * (float) Math.cos(a1), z1 = radius * (float) Math.sin(a1);
            float x2 = radius * (float) Math.cos(a2), z2 = radius * (float) Math.sin(a2);

            v(mat, c, x1, 0, z1, r, g, b, a);
            v(mat, c, x1, h, z1, r, g, b, a * 0.2f);
            v(mat, c, x2, h, z2, r, g, b, a * 0.2f);
            v(mat, c, x2, 0, z2, r, g, b, a);
        }
    }

    // ------------------------------------------------------------------ Black Flash (Jujutsu Kaisen)

    private static float[] norm(float x, float y, float z) {
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        if (l < 1.0e-6f) return new float[]{1, 0, 0};
        return new float[]{x / l, y / l, z / l};
    }

    private static float[] cross(float ax, float ay, float az, float bx, float by, float bz) {
        return new float[]{ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
    }

    /**
     * Camera-facing ribbon along a polyline. Width tapers to a point at both ends (brush-stroke look).
     * {@code lift} pushes the ribbon towards the camera so stacked layers don't z-fight.
     */
    private static void ribbon(Matrix4f m, VertexConsumer c, float[][] pts, int count, float maxWidth,
                               float cx, float cy, float cz, float lift,
                               float r, float g, float b, float a) {
        ribbon(m, c, pts, count, maxWidth, cx, cy, cz, lift, r, g, b, a, false);
    }

    /** {@code thorn}: fat at the root, pointed at the tip (like a thorn/tendril) instead of symmetric. */
    private static void ribbon(Matrix4f m, VertexConsumer c, float[][] pts, int count, float maxWidth,
                               float cx, float cy, float cz, float lift,
                               float r, float g, float b, float a, boolean thorn) {
        if (count < 2 || a <= 0.002f) return;
        float[][] left = new float[count][];
        float[][] right = new float[count][];
        for (int i = 0; i < count; i++) {
            float[] p = pts[i];
            float[] p0 = pts[Math.max(0, i - 1)];
            float[] p1 = pts[Math.min(count - 1, i + 1)];
            float[] tan = norm(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]);
            float[] view = norm(cx - p[0], cy - p[1], cz - p[2]);
            float[] side = cross(tan[0], tan[1], tan[2], view[0], view[1], view[2]);
            float[] s = norm(side[0], side[1], side[2]);

            float u = (i + 0.5f) / count;
            float uu = Math.min(1.0f, Math.max(0.0f, u));
            float w = thorn
                    ? maxWidth * (float) Math.pow(1.0f - uu, 0.9)
                    : maxWidth * (float) Math.pow(Math.sin(Math.PI * uu), 0.6);

            float lx = p[0] + view[0] * lift, ly = p[1] + view[1] * lift, lz = p[2] + view[2] * lift;
            left[i] = new float[]{lx + s[0] * w, ly + s[1] * w, lz + s[2] * w};
            right[i] = new float[]{lx - s[0] * w, ly - s[1] * w, lz - s[2] * w};
        }
        for (int i = 0; i < count - 1; i++) {
            v(m, c, left[i][0], left[i][1], left[i][2], r, g, b, a);
            v(m, c, right[i][0], right[i][1], right[i][2], r, g, b, a);
            v(m, c, right[i + 1][0], right[i + 1][1], right[i + 1][2], r, g, b, a);
            v(m, c, left[i + 1][0], left[i + 1][1], left[i + 1][2], r, g, b, a);
        }
    }

    /** Camera-facing quad centered at (x,y,z) with half extents along the camera's right/up axes. */
    private static void billboard(Matrix4f m, VertexConsumer c, float x, float y, float z,
                                  float[] right, float[] up, float hw, float hh, float lift,
                                  float cx, float cy, float cz,
                                  float r, float g, float b, float a) {
        float[] view = norm(cx - x, cy - y, cz - z);
        x += view[0] * lift;
        y += view[1] * lift;
        z += view[2] * lift;
        v(m, c, x - right[0] * hw - up[0] * hh, y - right[1] * hw - up[1] * hh, z - right[2] * hw - up[2] * hh, r, g, b, a);
        v(m, c, x + right[0] * hw - up[0] * hh, y + right[1] * hw - up[1] * hh, z + right[2] * hw - up[2] * hh, r, g, b, a);
        v(m, c, x + right[0] * hw + up[0] * hh, y + right[1] * hw + up[1] * hh, z + right[2] * hw + up[2] * hh, r, g, b, a);
        v(m, c, x - right[0] * hw + up[0] * hh, y - right[1] * hw + up[1] * hh, z - right[2] * hw + up[2] * hh, r, g, b, a);
    }

    private static float[][] blade(Random rng, float ox, float oy, float oz, float length, int n, float curve) {
        float theta = rng.nextFloat() * (float) (Math.PI * 2);
        float phi = (rng.nextFloat() - 0.35f) * 1.3f;
        float[] dir = norm((float) (Math.cos(phi) * Math.cos(theta)), (float) Math.sin(phi), (float) (Math.cos(phi) * Math.sin(theta)));
        float[] side = norm(-dir[2], 0.0f, dir[0]);
        float turn = (rng.nextBoolean() ? 1.0f : -1.0f) * curve;
        float step = length / n;
        float[][] pts = new float[n][];
        float x = ox + dir[0] * 0.12f, y = oy + dir[1] * 0.12f, z = oz + dir[2] * 0.12f;
        for (int i = 0; i < n; i++) {
            pts[i] = new float[]{x, y, z};
            x += dir[0] * step;
            y += dir[1] * step;
            z += dir[2] * step;
            dir = norm(dir[0] + side[0] * turn, dir[1] + (0.15f * turn), dir[2] + side[2] * turn);
        }
        return pts;
    }

    private static float[][] bolt(Random rng, float ox, float oy, float oz, float length, int n, float jag) {
        float theta = rng.nextFloat() * (float) (Math.PI * 2);
        float phi = (rng.nextFloat() - 0.3f) * 1.4f;
        float[] dir = norm((float) (Math.cos(phi) * Math.cos(theta)), (float) Math.sin(phi), (float) (Math.cos(phi) * Math.sin(theta)));
        return boltFrom(rng, ox, oy, oz, dir, length, n, jag);
    }

    /** Jagged polyline starting at (ox,oy,oz) heading along dir; used for main tendrils and their branches. */
    private static float[][] boltFrom(Random rng, float ox, float oy, float oz, float[] dir, float length, int n, float jag) {
        float step = length / n;
        float[][] pts = new float[n][];
        float x = ox + dir[0] * 0.2f, y = oy + dir[1] * 0.2f, z = oz + dir[2] * 0.2f;
        for (int i = 0; i < n; i++) {
            pts[i] = new float[]{x, y, z};
            x += dir[0] * step;
            y += dir[1] * step;
            z += dir[2] * step;
            dir = norm(dir[0] + (rng.nextFloat() - 0.5f) * jag,
                       dir[1] + (rng.nextFloat() - 0.5f) * jag,
                       dir[2] + (rng.nextFloat() - 0.5f) * jag);
        }
        return pts;
    }

    /** Removes most of the component pointing towards the camera so nothing covers the player. */
    private static float[] awayFromCamera(float[] d, float[] view) {
        float dot = d[0] * view[0] + d[1] * view[1] + d[2] * view[2];
        if (dot <= 0.35f) return d;
        // push it behind the player (hidden by his body, visible around the edges) instead of flattening it
        return norm(d[0] - view[0] * (dot + 0.45f), d[1] - view[1] * (dot + 0.45f), d[2] - view[2] * (dot + 0.45f));
    }

    private static void drawTendril(Matrix4f m, VertexConsumer c, float[][] pts, float grow, float a, float w,
                                    float cx, float cy, float cz, float L, float[] edge, float[] core) {
        int visible = (int) Math.ceil(pts.length * grow);
        if (visible < 2 || a <= 0.0f) return;
        ribbon(m, c, pts, visible, w * 3.0f + 0.03f, cx, cy, cz, L * 4, edge[0], edge[1], edge[2], 0.12f * a, true);  // soft glow
        ribbon(m, c, pts, visible, w * 1.5f + 0.015f, cx, cy, cz, L * 5, edge[0], edge[1], edge[2], a, true);        // outline
        ribbon(m, c, pts, visible, w, cx, cy, cz, L * 6, core[0], core[1], core[2], a, true);                        // dark core
    }

    private static void renderBlackFlash(Matrix4f m, VertexConsumer c, ActiveEffect effect, float tickDelta, float alpha,
                                         float cx, float cy, float cz) {
        float t = effect.age + tickDelta; // effect time in ticks
        Random rng = new Random(System.identityHashCode(effect) * 31L + 7L);
        float ox = 0.0f, oy = 0.6f, oz = 0.0f; // impact center, roughly chest height

        // Colours come from the effect settings (presets just change these); density saves FPS
        EffectConfig cfg = effect.config;
        float[] core = {cfg.red, cfg.green, cfg.blue};
        float[] edge = {cfg.secondaryRed, cfg.secondaryGreen, cfg.secondaryBlue};
        float dens = Math.max(0.2f, Math.min(1.0f, cfg.density));
        if (EffectManager.activeCount() > 6) dens *= 0.6f; // crowded fight: automatically draw less
        int shardCount = Math.max(2, Math.round(8 * dens));
        int debrisCount = Math.max(6, Math.round(36 * dens));

        float dist = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float L = 0.004f * Math.max(1.0f, dist * 0.3f); // z-fight lift unit, grows with distance

        // camera-facing basis at the impact center
        float[] view = norm(cx - ox, cy - oy, cz - oz);
        float[] right = cross(0, 1, 0, view[0], view[1], view[2]);
        right = norm(right[0], right[1], right[2]);
        float[] up = cross(view[0], view[1], view[2], right[0], right[1], right[2]);

        // The player must stay visible: nothing is drawn on top of the body. Tendrils start at the body's edge
        // and are steered so they never point straight at the camera.

        // Black tendrils with glowing red outline (they linger after the flash),
        // radiating from the player, each with thorn-like side branches (like the manga panel)
        int majors = Math.max(3, Math.round(14 * dens));
        float boltA = (t < 14.0f ? 1.0f : Math.max(0.0f, 1.0f - (t - 14.0f) / 10.0f)) * alpha;
        float boltGrow = Math.min(1.0f, Math.max(0.0f, (t - 0.3f) / 2.5f));
        for (int i = 0; i < majors; i++) {
            // spread the tendrils evenly around the impact (golden-angle spiral), slightly biased upward
            float yy = 0.9f - 1.5f * (i + 0.5f) / majors + (rng.nextFloat() - 0.5f) * 0.25f;
            float ring = (float) Math.sqrt(Math.max(0.0f, 1.0f - yy * yy));
            float phi = i * 2.399963f + rng.nextFloat() * 0.5f;
            float[] d0 = awayFromCamera(norm(ring * (float) Math.cos(phi), yy, ring * (float) Math.sin(phi)), view);
            float len = 2.4f + rng.nextFloat() * 2.4f;
            float w = 0.07f + rng.nextFloat() * 0.04f;
            // grow out of the player's body: root sits on the body surface at a random height along it
            float[] hd = norm(d0[0], 0.0f, d0[2]);
            float rootY = -0.15f + rng.nextFloat() * 1.2f;
            float[][] main = boltFrom(rng, hd[0] * 0.12f, rootY, hd[2] * 0.12f, d0, len, 12, 1.5f);
            drawTendril(m, c, main, boltGrow, boltA, w, cx, cy, cz, L, edge, core);

            int branches = Math.max(1, Math.round((3 + rng.nextInt(3)) * dens));
            for (int k = 0; k < branches; k++) {
                int idx = 2 + rng.nextInt(main.length - 4);
                float[] p = main[idx];
                float[] tan = norm(main[idx + 1][0] - main[idx - 1][0], main[idx + 1][1] - main[idx - 1][1], main[idx + 1][2] - main[idx - 1][2]);
                float[] dir = norm(tan[0] + (rng.nextFloat() - 0.5f) * 2.4f,
                                   tan[1] + (rng.nextFloat() - 0.5f) * 2.4f,
                                   tan[2] + (rng.nextFloat() - 0.5f) * 2.4f);
                float[][] br = boltFrom(rng, p[0], p[1], p[2], dir, len * (0.12f + rng.nextFloat() * 0.2f), 4, 1.3f);
                // branches sprout a little later than the main tendril
                float g = Math.min(1.0f, Math.max(0.0f, (t - 1.5f) / 3.0f));
                drawTendril(m, c, br, g, boltA, w * 0.6f, cx, cy, cz, L, edge, core);
            }
        }

        // 4) Red shards floating around
        for (int i = 0; i < shardCount; i++) {
            float[] d = awayFromCamera(norm(rng.nextFloat() - 0.5f, rng.nextFloat() - 0.3f, rng.nextFloat() - 0.5f), view);
            float rad = 0.7f + rng.nextFloat() * 1.0f;
            float size = 0.09f + rng.nextFloat() * 0.07f;
            float appear = rng.nextFloat() * 3.0f;
            if (t < appear || boltA <= 0.0f) continue;
            float px = ox + d[0] * rad, py = oy + d[1] * rad + t * 0.015f, pz = oz + d[2] * rad;
            billboard(m, c, px, py, pz, right, up, size * 0.6f, size, L * 7, cx, cy, cz, edge[0], edge[1], edge[2], boltA * 0.9f);
            billboard(m, c, px, py, pz, right, up, size * 0.3f, size * 0.5f, L * 8, cx, cy, cz, core[0], core[1], core[2], boltA * 0.9f);
        }

        // 5) Black pixel debris flying outward
        for (int i = 0; i < debrisCount; i++) {
            float[] d = awayFromCamera(norm(rng.nextFloat() - 0.5f, rng.nextFloat() - 0.4f, rng.nextFloat() - 0.5f), view);
            float rad = 0.6f + rng.nextFloat() * 1.3f;
            float size = 0.05f + rng.nextFloat() * 0.07f;
            float speed = 0.01f + rng.nextFloat() * 0.03f;
            if (boltA <= 0.0f) continue;
            float px = ox + d[0] * (rad + t * speed * 4.0f);
            float py = oy + d[1] * (rad + t * speed * 4.0f) - t * t * 0.0008f;
            float pz = oz + d[2] * (rad + t * speed * 4.0f);
            billboard(m, c, px, py, pz, right, up, size, size * 0.33f, L * 9, cx, cy, cz, core[0], core[1], core[2], boltA * 0.9f);
            billboard(m, c, px, py, pz, right, up, size * 0.33f, size, L * 9, cx, cy, cz, core[0], core[1], core[2], boltA * 0.9f);
        }
    }
}
