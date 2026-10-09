package pl.neontext.client.anim;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Replaces the colour of every emitted glyph vertex with the animated colour.
 *
 * <p>Baked glyphs are immutable records, so the colour cannot be swapped from the outside. What we
 * can do is intercept the vertices on their way into the mesh: vanilla always emits a glyph as
 * groups of four vertices ({@code addVertex -> setColor -> setUv -> setLight}) and, when the glyph
 * has a shadow, it emits the shadow group first. Counting groups therefore tells us exactly which
 * vertices belong to the body and which to the shadow, so the shadow keeps its dark colour and only
 * the body is animated.
 */
public final class RecoloringVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int rgb;
    private final int alpha;
    private final boolean hasShadow;

    private int group;
    private int indexInGroup;

    /**
     * @param delegate    the real consumer
     * @param animated    packed ARGB the glyph body should get
     * @param original    the glyph's own packed ARGB, used to carry over its alpha
     * @param hasShadow   whether vanilla will emit a shadow group before the body
     */
    public RecoloringVertexConsumer(VertexConsumer delegate, int animated, int original, boolean hasShadow) {
        this.delegate = delegate;
        this.rgb = animated & 0x00FFFFFF;
        // a widget that is fading out carries its alpha in the glyph colour - keep that
        int oa = (original >>> 24) & 0xFF;
        int aa = (animated >>> 24) & 0xFF;
        this.alpha = oa == 255 ? aa : (oa * aa) / 255;
        this.hasShadow = hasShadow;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        indexInGroup++;
        return delegate.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return delegate.setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer setColor(int packed) {
        if (isBodyGroup()) {
            return delegate.setColor((alpha << 24) | rgb);
        }
        return delegate.setColor(packed);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        if (indexInGroup >= 4) {
            indexInGroup = 0;
            group++;
        }
        return delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        return delegate.setUv3(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return delegate.setNormal(x, y, z);
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return delegate.setLineWidth(width);
    }

    /**
     * Group 0 is the shadow when there is one. Bold text emits two body groups, and everything from
     * group 1 on is body, so the check stays correct for bold and italic glyphs too.
     */
    private boolean isBodyGroup() {
        return hasShadow ? group >= 1 : true;
    }
}
