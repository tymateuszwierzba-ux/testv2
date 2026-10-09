package pl.neontext.client.anim;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Feeds the GPU the same geometry as vanilla, but repaints every quad with the animated colour.
 *
 * <p>Colour detection is exact-value based: vanilla emits {@code setColor(bodyColor)} for letter
 * quads and {@code setColor(shadowColor)} for the shadow quads (two of each when the style is bold).
 * Matching the packed value keeps bold shadows dark and - crucially - leaves any other colour
 * (chat background plates, underlines) completely untouched. This works for every glyph
 * implementation in 26.3, not just {@code PlainTextRenderable}.
 */
public final class RecoloringVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int originalBody;
    private final int originalShadow;
    private final int body;
    private final int shadow;
    private final int alphaMul;

    public RecoloringVertexConsumer(VertexConsumer delegate, int originalBody, int originalShadow,
                                    int body, int shadow) {
        this(delegate, originalBody, originalShadow, body, shadow, 255);
    }

    public RecoloringVertexConsumer(VertexConsumer delegate, int originalBody, int originalShadow,
                                    int body, int shadow, int alphaMul) {
        this.delegate = delegate;
        this.originalBody = originalBody;
        this.originalShadow = originalShadow;
        this.body = body;
        this.shadow = shadow;
        this.alphaMul = Math.max(0, Math.min(255, alphaMul));
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        delegate.setUv3(u, v);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int packed) {
        int out;
        if (packed == originalBody) {
            out = body;
        } else if (originalShadow != 0 && packed == originalShadow) {
            out = shadow;
        } else {
            out = packed;
        }
        if (alphaMul < 255) {
            int a = ColorUtil.alpha(out) * alphaMul / 255;
            out = (a << 24) | (out & 0xFFFFFF);
        }
        delegate.setColor(out);
        return this;
    }

    @Override
    public VertexConsumer setLight(int uv) {
        delegate.setLight(uv);
        return this;
    }

    @Override
    public VertexConsumer setOverlay(int uv) {
        delegate.setOverlay(uv);
        return this;
    }
}
