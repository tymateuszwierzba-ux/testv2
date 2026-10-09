package pl.neontext.client.anim;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.PlainTextRenderable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * One vanilla glyph, re-rendered with an animated colour and an animated transform.
 *
 * <p>Movement is done by handing {@link TextRenderable#render} a matrix that has an extra
 * translate/rotate/scale/shear around the glyph centre. The caller's matrix is never mutated - we
 * always work on a copy, which matters because the GUI pipeline shares one pose between every glyph
 * of a text run.
 *
 * <p>Colour is done by wrapping the {@link VertexConsumer}, see {@link RecoloringVertexConsumer}.
 * The original body/shadow colours are read from the glyph itself: {@link PlainTextRenderable}
 * exposes them directly, and every other glyph type in 26.3 (the standard sheet glyphs and effect
 * quads are records named {@code color()}/{@code shadowColor()}) is read through record accessors.
 * Without this, colour effects like rainbow were invisible on normal text - the bug that made the
 * mod look dead in game.
 */
public final class AnimatedRenderable implements TextRenderable.Styled {

    private final TextRenderable.Styled delegate;
    private final AnimStyle anim;
    private final int index;
    private final int count;
    private final int seed;
    private final java.util.function.LongSupplier clock;
    private final GlyphStyle style;

    public AnimatedRenderable(TextRenderable.Styled delegate, AnimStyle anim, int index, int count,
                              int seed, java.util.function.LongSupplier clock) {
        this.delegate = delegate;
        this.anim = anim;
        this.index = index;
        this.count = Math.max(1, count);
        this.seed = seed;
        this.clock = clock == null ? () -> 0L : clock;
        this.style = EffectEngine.compute(anim, index, this.count, delegate.left(), delegate.top(),
                this.clock.getAsLong(), seed);
    }

    @Override
    public void render(Matrix4fc pose, VertexConsumer consumer, int lightCoords, boolean fullBright) {
        int originalBody = Colors.bodyOf(delegate);
        int originalShadow = Colors.shadowOf(delegate);

        int effectRgb = style.color() & 0xFFFFFF;
        int baseRgb = effectRgb == 0 ? originalBody & 0xFFFFFF : effectRgb;
        int alpha = ColorUtil.alpha(originalBody) * ColorUtil.alpha(style.color()) / 255;
        int bodyPacked = (alpha << 24) | (ColorUtil.brighten(0xFF000000 | baseRgb, anim.brightness) & 0xFFFFFF);
        int shadowPacked = (alpha << 24) | (ColorUtil.darken(baseRgb & 0xFFFFFF, 0.25f) & 0xFFFFFF);

        if (anim.glow) {
            // cheap neon bloom: one translucent, slightly enlarged copy behind the real glyph
            Matrix4f glowM = new Matrix4f(pose);
            float cx = (delegate.left() + delegate.right()) * 0.5f;
            float cy = (delegate.top() + delegate.bottom()) * 0.5f;
            glowM.translate(cx + style.dx(), cy + style.dy(), 0.0f);
            glowM.scale(1.28f);
            glowM.translate(-cx, -cy, 0.0f);
            delegate.render(glowM, new RecoloringVertexConsumer(consumer, originalBody, originalShadow,
                    bodyPacked, shadowPacked, 55), lightCoords, fullBright);
        }

        Matrix4fc matrix = pose;
        if (!style.isIdentity()) {
            float cx = (delegate.left() + delegate.right()) * 0.5f;
            float cy = (delegate.top() + delegate.bottom()) * 0.5f;
            Matrix4f copy = new Matrix4f(pose);
            copy.translate(cx + style.dx(), cy + style.dy(), 0.0f);
            if (style.rotation() != 0.0f) {
                copy.rotateZ((float) Math.toRadians(style.rotation()));
            }
            if (style.scale() != 1.0f) {
                copy.scale(style.scale());
            }
            if (style.shearX() != 0.0f) {
                // JOML has no shear helper on Matrix4f, so build the 2D shear by hand (column major)
                copy.mul(new Matrix4f(1.0f, 0.0f, 0.0f, 0.0f,
                        style.shearX(), 1.0f, 0.0f, 0.0f,
                        0.0f, 0.0f, 1.0f, 0.0f,
                        0.0f, 0.0f, 0.0f, 1.0f));
            }
            copy.translate(-cx, -cy, 0.0f);
            matrix = copy;
        }

        delegate.render(matrix, new RecoloringVertexConsumer(consumer, originalBody, originalShadow,
                bodyPacked, shadowPacked), lightCoords, fullBright);
    }

    @Override
    public net.minecraft.network.chat.Style style() {
        return delegate.style();
    }

    @Override
    public RenderType renderType(Font.DisplayMode mode) {
        return delegate.renderType(mode);
    }

    @Override
    public com.mojang.renderpearl.api.textures.GpuTextureView textureView() {
        return delegate.textureView();
    }

    @Override
    public com.mojang.renderpearl.api.pipeline.RenderPipeline guiPipeline() {
        return delegate.guiPipeline();
    }

    // bounds grow with the animation so scissor and hover tests keep covering the moving glyph

    @Override
    public float left() {
        return delegate.left() + Math.min(0.0f, style.dx());
    }

    @Override
    public float top() {
        return delegate.top() + Math.min(0.0f, style.dy());
    }

    @Override
    public float right() {
        return delegate.right() + Math.max(0.0f, style.dx());
    }

    @Override
    public float bottom() {
        return delegate.bottom() + Math.max(0.0f, style.dy());
    }

    /**
     * Reads the original body/shadow colours from any glyph implementation in 26.3. The standard
     * glyphs are private records whose public {@code color()}/{@code shadowColor()} accessors we
     * reach with method handles; {@link PlainTextRenderable} is handled directly. If nothing is
     * readable we fall back to "repaint every quad" - still animated, just without the shadow
     * distinction.
     */
    static final class Colors {

        static final int FALLBACK_BODY = 0xFFFFFFFF;
        static final int FALLBACK_SHADOW = 0;

        private static final ClassValue<Accessors> CACHE = new ClassValue<>() {
            @Override
            protected Accessors computeValue(Class<?> type) {
                try {
                    MethodHandle color = lookup(type, "color");
                    MethodHandle shadowColor = lookup(type, "shadowColor");
                    return new Accessors(color, shadowColor);
                } catch (Throwable t) {
                    return new Accessors(null, null);
                }
            }
        };

        private static MethodHandle lookup(Class<?> type, String name) throws Throwable {
            java.lang.reflect.Method m = type.getMethod(name);
            m.setAccessible(true);
            return MethodHandles.lookup().unreflect(m);
        }

        static int bodyOf(TextRenderable renderable) {
            if (renderable instanceof PlainTextRenderable plain) {
                return plain.color();
            }
            return (int) read(renderable, CACHE.get(renderable.getClass()).color, FALLBACK_BODY);
        }

        static int shadowOf(TextRenderable renderable) {
            if (renderable instanceof PlainTextRenderable plain) {
                return plain.shadowColor();
            }
            return (int) read(renderable, CACHE.get(renderable.getClass()).shadowColor, FALLBACK_SHADOW);
        }

        private static long read(Object target, MethodHandle handle, int fallback) {
            if (handle == null) {
                return fallback;
            }
            try {
                return (int) handle.invoke(target);
            } catch (Throwable t) {
                return fallback;
            }
        }

        private record Accessors(MethodHandle color, MethodHandle shadowColor) {
        }
    }
}
