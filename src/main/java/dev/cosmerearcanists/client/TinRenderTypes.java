package dev.cosmerearcanists.client;

import java.util.OptionalDouble;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class TinRenderTypes extends RenderType {
    private TinRenderTypes(){super("unused",DefaultVertexFormat.POSITION,VertexFormat.Mode.LINES,256,false,false,()->{},()->{});}
    private static RenderType lines;private static double width;
    public static RenderType lines(){
        if(lines==null || width!=PowerClientConfig.TIN_WIDTH.get()) {
            width=PowerClientConfig.TIN_WIDTH.get();
            lines=create("cosmere_tin_lines",DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,1536,false,false,
                CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER).setLineState(new LineStateShard(OptionalDouble.of(width)))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }return lines;
    }
    private static final RenderType MODELS=create("cosmere_tin_model_wire",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,false,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
        .setTextureState(new TextureStateShard(ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"),false,false))
        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static RenderType models(){return MODELS;}
}
