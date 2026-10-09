package dev.cosmerearcanists.client;

import java.util.OptionalDouble;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

public final class MetalLineRenderType extends RenderType {
    private static RenderType cached;
    private static double width;
    private static boolean through;
    private MetalLineRenderType() { super("unused", DefaultVertexFormat.POSITION, VertexFormat.Mode.LINES, 256, false, false, () -> { }, () -> { }); }
    public static RenderType get() {
        double w = PowerClientConfig.LINE_WIDTH.get(); boolean t = PowerClientConfig.THROUGH_WALLS.get();
        if (cached == null || w != width || through != t) {
            width = w; through = t;
            cached = create("cosmere_metal_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, false, false,
                CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(w)))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(t ? NO_DEPTH_TEST : LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }
        return cached;
    }
}
