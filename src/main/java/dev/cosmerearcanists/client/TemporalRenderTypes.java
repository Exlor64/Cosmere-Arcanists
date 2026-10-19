package dev.cosmerearcanists.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class TemporalRenderTypes extends RenderType {
    private TemporalRenderTypes(){super("unused",DefaultVertexFormat.POSITION,VertexFormat.Mode.LINES,256,false,false,()->{},()->{});}
    private static final RenderType GHOST=create("cosmere_future_shadow",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
        .setTextureState(new TextureStateShard(ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"),false,false))
        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static RenderType ghost(){return GHOST;}
}
