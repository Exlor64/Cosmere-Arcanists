package dev.cosmerearcanists.client;

import java.util.OptionalDouble;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class TinRenderTypes extends RenderType {
    private TinRenderTypes(){super("unused",DefaultVertexFormat.POSITION,VertexFormat.Mode.LINES,256,false,false,()->{},()->{});}
    private static net.minecraft.client.renderer.ShaderInstance blurShader;
    public static void registerShaders(net.neoforged.neoforge.client.event.RegisterShadersEvent event) {
        try{event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("cosmerearcanists","tin_blur"),DefaultVertexFormat.POSITION_TEX_COLOR),shader->blurShader=shader);}
        catch(java.io.IOException e){throw new java.io.UncheckedIOException("Unable to load tin echo shader",e);}
    }
    private static final RenderType BLUR=create("cosmere_tin_soft_echo",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,4096,false,false,
        CompositeState.builder().setShaderState(new ShaderStateShard(()->blurShader)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static RenderType blur(){return BLUR;}
    private static final RenderType SOFT_MODEL=create("cosmere_tin_soft_silhouette",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,false,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
        .setTextureState(new TextureStateShard(ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"),false,false))
        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setDepthTestState(NO_DEPTH_TEST).setCullState(CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static RenderType softModel(){return SOFT_MODEL;}
    private static final RenderType SOFT_FILL=create("cosmere_tin_soft_surface",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,4096,false,false,
        CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static RenderType softFill(){return SOFT_FILL;}
    private static RenderType contour(double width){return create("cosmere_tin_contour_"+(int)width,DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,4096,false,false,
        CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER).setLineState(new LineStateShard(OptionalDouble.of(width)))
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
    private static final RenderType SOFT_EDGE=contour(1.75),SOFT_HALO=contour(5);
    public static RenderType softEdge(){return SOFT_EDGE;}
    public static RenderType softHalo(){return SOFT_HALO;}
    private static RenderType lines;private static double width;
    public static RenderType lines(){
        if(lines==null || width!=echoWidth()) {
            width=echoWidth();
            lines=create("cosmere_tin_lines",DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,1536,false,false,
                CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER).setLineState(new LineStateShard(OptionalDouble.of(width)))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }return lines;
    }
    private static double echoWidth(){return Math.min(5,PowerClientConfig.TIN_WIDTH.get()*PowerClientConfig.TIN_WIDTH_SCALE.get());}
    private static RenderType model;private static double modelWidth;
    public static RenderType models(){double current=echoWidth();if(model==null || current!=modelWidth){modelWidth=current;model=create("cosmere_tin_model_wire",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,false,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
        .setTextureState(new TextureStateShard(ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"),false,false))
        .setLineState(new LineStateShard(OptionalDouble.of(modelWidth))).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }return model;
    }
}
