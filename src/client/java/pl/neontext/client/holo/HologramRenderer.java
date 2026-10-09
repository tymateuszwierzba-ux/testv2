package pl.neontext.client.holo;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import pl.neontext.client.core.NeonRuntime;

import java.util.List;

/**
 * Draws the client-side holograms.
 *
 * <p>Holograms are projected into screen space and then drawn through the normal GUI text pipeline.
 * That is a deliberate choice: it means a hologram is animated by exactly the same code as chat or a
 * nameplate, so every effect, every colour option and every future improvement applies to holograms
 * for free, and we never have to touch the level renderer or its vertex formats.
 *
 * <p>The trade-off is that holograms always face the camera and draw on top of the world. For text
 * signs floating in the world that is what you want anyway.
 */
public final class HologramRenderer {

    /** Perspective falloff, in blocks, at which a hologram is drawn at its configured scale. */
    private static final float REFERENCE_DISTANCE = 6.0f;

    private HologramRenderer() {
    }

    /** Called from the HUD extraction, once per frame. */
    public static void render(GuiGraphicsExtractor extractor, Font font) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null || !NeonRuntime.masterEnabled()) {
            return;
        }
        if (!NeonRuntime.config().target(pl.neontext.client.anim.AnimTarget.HOLOGRAM).enabled) {
            return;
        }
        Camera camera = mc.gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized()) {
            return;
        }
        Vec3 camPos = camera.position();
        List<Hologram> holograms = HologramManager.visibleFrom(camPos.x, camPos.y, camPos.z);
        if (holograms.isEmpty()) {
            return;
        }

        Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(new Matrix4f());
        int guiScale = Math.max(1, mc.getWindow().getGuiScale());
        int screenWidth = mc.getWindow().getWidth();
        int screenHeight = mc.getWindow().getHeight();

        // refresh the screen-space cache used by click-to-select (crosshair picking)
        for (Hologram known : HologramManager.all()) {
            known.onScreen = false;
        }

        for (Hologram h : holograms) {
            Vector4f clip = new Vector4f((float) (h.x - camPos.x), (float) (h.y - camPos.y),
                    (float) (h.z - camPos.z), 1.0f);
            clip.mul(viewProjection);
            if (clip.w <= 1.0e-4f) {
                continue; // behind the camera
            }
            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            if (ndcX < -1.4f || ndcX > 1.4f || ndcY < -1.4f || ndcY > 1.4f) {
                continue;
            }
            float pixelsX = (ndcX * 0.5f + 0.5f) * screenWidth;
            float pixelsY = (1.0f - (ndcY * 0.5f + 0.5f)) * screenHeight;
            float guiX = pixelsX / guiScale;
            float guiY = pixelsY / guiScale;

            double dx = h.x - camPos.x, dy = h.y - camPos.y, dz = h.z - camPos.z;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float scale = h.scale * REFERENCE_DISTANCE / Math.max(1.0f, distance) / guiScale;
            scale = clamp(scale, 0.05f, 6.0f);

            drawOne(extractor, font, h, guiX, guiY, scale);
        }
    }

    private static void drawOne(GuiGraphicsExtractor extractor, Font font, Hologram h, float guiX, float guiY,
                               float scale) {
        Component component = Component.literal(h.text);
        FormattedCharSequence plain = component.getVisualOrderText();
        float width = font.width(plain);
        float height = font.lineHeight;

        h.onScreen = true;
        h.screenLeft = guiX - width * scale * 0.5f;
        h.screenRight = guiX + width * scale * 0.5f;
        h.screenTop = guiY;
        h.screenBottom = guiY + height * scale;

        Matrix3x2fStack pose = extractor.pose();
        pose.pushMatrix();
        try {
            pose.translate(guiX, guiY);
            pose.scale(scale, scale);

            if (h.background != 0) {
                extractor.fill((int) (-width * 0.5f) - 3, -3, (int) (width * 0.5f) + 3, (int) height + 2,
                        h.background);
            }

            // the marker sequence is how the pipeline recognises this run as hologram text
            extractor.text(font, new HologramText(plain, h), (int) (-width * 0.5f), 0, 0xFFFFFF, h.shadow);
        } finally {
            pose.popMatrix();
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
