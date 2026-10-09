package pl.neontext.client.anim;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.PlainTextRenderable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * One vanilla glyph, re-rendered with an animated colour and an animated transform.
 *
 * <p>Movement is done by handing {@link TextRenderable#render} a matrix that has an extra
 * translate/rotate/scale/shear around the glyph centre. The caller's matrix is never mutated - we
 * always work on a copy, which matters because the GUI pipeline shares one pose between every glyph
 * of a text run.
 *
 * <p>Colour is done by wrapping the {@link VertexConsumer}, see {@link RecoloringVertexConsumer}.
 */
public final class AnimatedRenderable implements TextRenderable {

    private final TextRenderable delegate;
    private final GlyphStyle style;

    public AnimatedRenderable(TextRenderable delegate, GlyphStyle style) {
        this.delegate = delegate;
        this.style = style;
    }

    public TextRenderable delegate() {
        return delegate;
    }

    public GlyphStyle style() {
        return style;
    }

    @Override
    public void render(Matrix4fc pose, VertexConsumer consumer, int lightCoords, boolean fullBright) {
        VertexConsumer target = consumer;
        if (style.color() != 0xFFFFFFFF && delegate instanceof PlainTextRenderable plain) {
            target = new RecoloringVertexConsumer(consumer, style.color(), plain.color(),
                    plain.shadowColor() != 0);
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

        delegate.render(matrix, target, lightCoords, fullBright);
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
}
