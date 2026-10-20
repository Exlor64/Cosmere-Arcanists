package dev.cosmerearcanists.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
public final class BubbleRenderType extends RenderType {
    private BubbleRenderType(){super("unused",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    public static final RenderType SHELL=create("cosmere_time_bubble",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,65536,false,true,CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
}
