package dev.cosmerearcanists.client;
import dev.cosmerearcanists.koloss.KolossEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
/** Supplied model contains the sword; hiding its bone avoids a duplicate held-item layer. */
public final class KolossRenderer extends MobRenderer<KolossEntity,KolossModel> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("cosmerearcanists","textures/entity/koloss.png");
    public KolossRenderer(EntityRendererProvider.Context context){super(context,new KolossModel(context.bakeLayer(KolossModel.LAYER_LOCATION)),.65f);}
    @Override public ResourceLocation getTextureLocation(KolossEntity entity){return TEXTURE;}
}
