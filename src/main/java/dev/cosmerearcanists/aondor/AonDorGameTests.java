package dev.cosmerearcanists.aondor;

import com.mojang.authlib.GameProfile;
import java.util.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonDorGameTests {
    private static FakePlayer player(GameTestHelper h){return new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonDrawer"));}
    private static List<List<AonDrawing.Point>> horizontal(){return List.of(List.of(new AonDrawing.Point(0,0),new AonDrawing.Point(1,0)));}
    private static List<List<AonDrawing.Point>> vertical(){return List.of(List.of(new AonDrawing.Point(0,0),new AonDrawing.Point(0,1)));}
    private static AonDrawing drawing(){return new AonDrawing(List.of(new AonDrawing.Glyph(true,.5,.5,horizontal()),new AonDrawing.Glyph(false,.5,.1,vertical())));}
    @GameTest(template="empty") public static void aondorSelectionAndCloneDoNotGrantMetallicPowers(GameTestHelper h){
        var p=player(h);h.assertTrue(!PowerData.available(p).contains(AonDorService.POWER),"AonDor is not granted by default");
        p.getAttribute(PowerAttributes.AONDOR).setBaseValue(1);
        h.assertTrue(PowerData.available(p).contains(AonDorService.POWER)&&PowerData.assignSlot(p,AonDorService.POWER,0),"Persistent AonDor appears in existing power selector");
        h.assertTrue(!PowerAttributes.keys().contains(AonDorService.POWER)&&PowerData.storeLevels(p,AonDorService.POWER)==0&&PowerData.tapLevels(p,AonDorService.POWER)==0,"AonDor is not treated as a random Metallic Art or metalmind");
        var replacement=player(h);PowerData.clonePlayer(p,replacement);
        h.assertTrue(replacement.getAttributeValue(PowerAttributes.AONDOR)==1&&PowerData.available(replacement).contains(AonDorService.POWER),"AonDor ownership survives respawn cloning");
        PowerAttributes.reset(replacement);h.assertTrue(replacement.getAttributeValue(PowerAttributes.AONDOR)==0,"Administrative reset clears AonDor too");
        h.succeed();
    }
    @GameTest(template="empty") public static void drawingCheckEnforcesOwnershipSelectionAndDimension(GameTestHelper h){
        var p=player(h);var request=new AonDorNetwork.Check(UUID.randomUUID(),h.getLevel().dimension().location().toString(),0,drawing());
        h.assertTrue(AonDorService.inspect(p,request).contains("no longer available"),"Unowned AonDor requests are rejected");
        p.getAttribute(PowerAttributes.AONDOR).setBaseValue(1);PowerData.assignSlot(p,AonDorService.POWER,0);
        var foreign=new AonDorNetwork.Check(UUID.randomUUID(),"cosmerearcanists:wrong_dimension",0,drawing());
        h.assertTrue(AonDorService.inspect(p,foreign).contains("no longer available"),"Stale dimension requests are rejected");
        var otherSlot=new AonDorNetwork.Check(UUID.randomUUID(),h.getLevel().dimension().location().toString(),1,drawing());
        h.assertTrue(AonDorService.inspect(p,otherSlot).contains("no longer available"),"Wrong-slot requests are rejected");
        boolean enabled=AonDorConfig.ENABLED.get();try{AonDorConfig.ENABLED.set(false);h.assertTrue(!PowerData.available(p).contains(AonDorService.POWER),"Server disable removes AonDor access");}finally{AonDorConfig.ENABLED.set(enabled);}
        h.succeed();
    }
    @GameTest(template="empty") public static void drawingValidationUsesTemplateRolesAndNeverExecutesEffects(GameTestHelper h){
        var effect=new AonRecognition.Template("test:effect",true,horizontal());var argument=new AonRecognition.Template("test:argument",false,vertical());
        String result=AonDorService.validate(new AonDrawing(List.of(drawing().main())),List.of(effect,argument));
        h.assertTrue(result.contains("test:effect")&&result.contains("100%")&&result.contains("no spell was cast"),"Server recognizes ordered spell structure without inventing effect behavior");
        h.assertTrue(AonDorService.validate(drawing(),List.of()).contains("No Aon patterns"),"Empty symbol library cannot silently cast");
        h.assertTrue(AonDorService.validate(new AonDrawing(List.of(drawing().main())),List.of(new AonRecognition.Template("test:not_effect",false,horizontal()),argument)).contains("must be an effect"),"Central glyph must have an effect role");
        h.assertTrue(!AonDorService.validate(drawing(),List.of(effect,argument)).isEmpty(),"Invalid argument structure returns a diagnostic");
        h.succeed();
    }
    @GameTest(template="empty") public static void onlyRecognizedUnambiguousAonsAreLearnedAndSurviveRespawn(GameTestHelper h){
        var p=player(h);p.getAttribute(PowerAttributes.AONDOR).setBaseValue(1);
        var effect=new AonRecognition.Template("test:effect","Effect name",true,horizontal());var argument=new AonRecognition.Template("test:argument","Argument name",false,vertical());
        var templates=List.of(effect,argument);var glyph=drawing().main();
        h.assertTrue(AonLearning.known(p).isEmpty()&&AonDorService.learnedTemplate(p,effect.id(),templates)==null,"Undrawn Aons cannot be fetched from the palette");
        h.assertTrue(AonDorService.learnDrawing(p,glyph,templates)&&!AonDorService.learnDrawing(p,glyph,templates),"Correct first drawing learns once");
        var reversed=new AonDrawing.Glyph(true,.5,.5,List.of(List.of(new AonDrawing.Point(0,1),new AonDrawing.Point(0,0))));
        h.assertTrue(AonDorService.learnDrawing(p,reversed,templates),"A clear shape-only drawing also unlocks the Aon");
        var duplicate=new AonRecognition.Template("test:duplicate",true,horizontal());
        h.assertTrue(!AonDorService.learnDrawing(p,glyph,List.of(effect,duplicate)),"Ambiguous drawings never unlock a new entry");
        var fallback=AonRecognition.assess(reversed,templates,AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get());
        h.assertTrue(!fallback.precise()&&fallback.potency(.75)==.75,"Reversed strokes retain identity with reduced test potency");
        var clone=player(h);PowerData.clonePlayer(p,clone);
        h.assertTrue(AonLearning.known(clone).equals(Set.of(effect.id(),argument.id()))&&AonDorService.learnedTemplate(clone,effect.id(),templates)!=null,"Learned knowledge survives the normal player clone path");
        h.assertTrue(AonDorService.learnedTemplate(clone,effect.id(),List.of())==null&&AonLearning.known(clone).contains(effect.id()),"Temporarily missing patterns stay learned but are unavailable to pick");
        h.succeed();
    }
    @GameTest(template="empty") public static void paletteAndSizedGlyphPacketsRoundTrip(GameTestHelper h){
        var glyph=new AonDrawing.Glyph(false,.5,.25,.4,vertical());
        var packet=new AonDorNetwork.Stamp(UUID.randomUUID(),h.getLevel().dimension().location().toString(),"test:argument","Argument name",glyph);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{AonDorNetwork.Stamp.CODEC.encode(buffer,packet);h.assertTrue(packet.equals(AonDorNetwork.Stamp.CODEC.decode(buffer)),"Palette names, directional strokes and display size round-trip");}
        finally{buffer.release();}
        var preview=new AonDorNetwork.Preview(h.getLevel().dimension().location().toString(),0,true,glyph);
        var previewBuffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{AonDorNetwork.Preview.CODEC.encode(previewBuffer,preview);h.assertTrue(preview.equals(AonDorNetwork.Preview.CODEC.decode(previewBuffer)),"Live drawing geometry and active state round-trip");}finally{previewBuffer.release();}
        var projection=new AonDorNetwork.Projection(UUID.randomUUID(),preview.dimension(),true,glyph);
        var projectionBuffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{AonDorNetwork.Projection.CODEC.encode(projectionBuffer,projection);h.assertTrue(projection.equals(AonDorNetwork.Projection.CODEC.decode(projectionBuffer)),"Projection owner, dimension and ink round-trip");}finally{projectionBuffer.release();}
        var cast=new AonDorNetwork.Cast(new AonDorNetwork.Check(UUID.randomUUID(),preview.dimension(),0,new AonDrawing(List.of(new AonDrawing.Glyph(true,.5,.5,horizontal())))));
        var castBuffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{AonDorNetwork.Cast.CODEC.encode(castBuffer,cast);h.assertTrue(cast.equals(AonDorNetwork.Cast.CODEC.decode(castBuffer)),"Cast raw drawing and request context round-trip");}finally{castBuffer.release();}
        var library=new AonDorNetwork.Library(h.getLevel().dimension().location().toString(),List.of(new AonDorNetwork.Known("test:argument","Argument name",false)),"Learned");
        var listBuffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{AonDorNetwork.Library.CODEC.encode(listBuffer,library);h.assertTrue(library.equals(AonDorNetwork.Library.CODEC.decode(listBuffer)),"Only the bounded learned-name list is transmitted with the library");}
        finally{listBuffer.release();}
        h.succeed();
    }

}
