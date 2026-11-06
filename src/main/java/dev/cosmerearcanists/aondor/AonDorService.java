package dev.cosmerearcanists.aondor;
import com.google.gson.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.cosmerearcanists.power.*;
public final class AonDorService {
    public static final String POWER="sel:aondor";
    private static final Map<MinecraftServer,List<AonRecognition.Template>> CATALOGS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> CHECKED=new WeakHashMap<>();
    private record Budget(long tick,int count){}
    private static final Map<ServerPlayer,Budget> REQUESTS=new WeakHashMap<>();
    private static boolean requestAllowed(ServerPlayer player){long now=player.level().getGameTime();var old=REQUESTS.get(player);int count=old==null||now<old.tick()||now-old.tick()>=20?1:old.count()+1;REQUESTS.put(player,new Budget(old==null||now<old.tick()||now-old.tick()>=20?now:old.tick(),count));return count<=32;}
    public static boolean permitted(ServerPlayer player,String dimension,int slot){return (slot==0||slot==1)&&allowed(player)&&dimension.equals(player.level().dimension().location().toString())&&POWER.equals(slot==0?PowerData.selected(player):PowerData.secondary(player));}
    private static List<AonRecognition.Template> catalog(ServerPlayer player){return CATALOGS.computeIfAbsent(player.server,AonDorService::load);}
    public static AonRecognition.Match recognize(AonDrawing.Glyph glyph,List<AonRecognition.Template> templates){return AonRecognition.recognize(glyph,templates,AonDorConfig.tolerance());}
    public static boolean learnDrawing(ServerPlayer player,AonDrawing.Glyph glyph,List<AonRecognition.Template> templates){var match=AonRecognition.assess(glyph,templates,AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get());return match.template()!=null&&AonLearning.learn(player,match.template());}
    public static AonRecognition.Template learnedTemplate(ServerPlayer player,String id,List<AonRecognition.Template> templates){return AonLearning.known(player).contains(id)?templates.stream().filter(t->t.id().equals(id)).findFirst().orElse(null):null;}
    private static void library(ServerPlayer player,String message){var known=AonLearning.known(player);var list=catalog(player).stream().filter(t->known.contains(t.id())).map(t->new AonDorNetwork.Known(t.id(),t.name(),t.effect())).toList();PacketDistributor.sendToPlayer(player,new AonDorNetwork.Library(player.level().dimension().location().toString(),list,message));}
    public static void trace(ServerPlayer player,AonDorNetwork.Trace request){if(requestAllowed(player)&&permitted(player,request.dimension(),request.slot())&&learnDrawing(player,request.glyph(),catalog(player)))library(player,"Aon learned.");}
    public static void pick(ServerPlayer player,AonDorNetwork.Pick request){
        if(!requestAllowed(player)||!permitted(player,request.dimension(),request.slot()))return;
        if(request.id().isEmpty()){library(player,"");return;}
        var template=learnedTemplate(player,request.id(),catalog(player));if(template==null)return;
        PacketDistributor.sendToPlayer(player,new AonDorNetwork.Stamp(request.nonce(),player.level().dimension().location().toString(),template.id(),template.name(),new AonDrawing.Glyph(template.effect(),.5,.5,template.strokes())));
    }
    private static final Map<ServerPlayer,Long> PREVIEWED=new WeakHashMap<>();
    public static void preview(ServerPlayer player,AonDorNetwork.Preview request){
        if(!permitted(player,request.dimension(),request.slot())||!request.glyph().effect())return;
        long now=player.level().getGameTime();var last=PREVIEWED.get(player);
        if(request.active()&&last!=null&&now>=last&&now-last<5)return;
        if(!requestAllowed(player))return;
        PREVIEWED.put(player,now);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new AonDorNetwork.Projection(player.getUUID(),request.dimension(),request.active(),request.glyph()));
    }
    public static boolean allowed(ServerPlayer player){return PowerConfig.ENABLED.get()&&AonDorConfig.ENABLED.get()&&player.isAlive()&&!player.isSpectator()&&player.getAttributeValue(PowerAttributes.AONDOR)>0;}
    @SubscribeEvent public void reload(OnDatapackSyncEvent event){if(event.getPlayer()==null)CATALOGS.remove(event.getPlayerList().getServer());}
    private static List<AonRecognition.Template> load(MinecraftServer server){
        var templates=new ArrayList<AonRecognition.Template>();
        var resources=server.getResourceManager().listResources("aondor/aons",id->id.getPath().endsWith(".json"));
        for(var entry:resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).limit(256).toList())try(var reader=entry.getValue().openAsReader()){
            var json=JsonParser.parseReader(reader).getAsJsonObject();String kind=json.get("kind").getAsString();
            if(!kind.equals("effect")&&!kind.equals("argument"))throw new IllegalArgumentException("kind must be effect or argument");
            var strokes=new ArrayList<List<AonDrawing.Point>>();
            for(var stroke:json.getAsJsonArray("strokes")){var points=new ArrayList<AonDrawing.Point>();for(var point:stroke.getAsJsonArray()){var pair=point.getAsJsonArray();if(pair.size()!=2)throw new IllegalArgumentException("Expected x,y pair");points.add(new AonDrawing.Point(pair.get(0).getAsDouble(),pair.get(1).getAsDouble()));}strokes.add(points);}
            String path=entry.getKey().getPath();String id=entry.getKey().getNamespace()+":"+path.substring("aondor/aons/".length(),path.length()-5);
            templates.add(new AonRecognition.Template(id,json.has("name")?json.get("name").getAsString():id,kind.equals("effect"),strokes));
        }catch(Exception exception){com.mojang.logging.LogUtils.getLogger().warn("Skipping invalid Aon template {}: {}",entry.getKey(),exception.getMessage());}
        return List.copyOf(templates);
    }
    public static String validate(AonDrawing drawing,List<AonRecognition.Template> templates){
        if(templates.isEmpty())return "No Aon patterns are configured yet. Your drawing was not cast.";
        if(!drawing.arguments().isEmpty())return "Arguments are disabled for this drawing test. Reset to a single central Aon.";
        var effect=AonRecognition.assess(drawing.main(),templates,AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get());
        if(effect.template()==null)return effect.ambiguous()?"Main Aon is ambiguous; refine its shape.":"Main Aon was not recognized. Refine its shape or undo a stroke.";
        if(!effect.template().effect())return "The central Aon must be an effect.";
        return "Recognized: "+effect.template().name()+" — "+(effect.precise()?"Precise strokes":"Shape only")+"; potency "+Math.round(100*effect.potency(AonDorConfig.FALLBACK_POTENCY.get()))+"%. Ready to cast; no spell was cast.";
    }

    public static String inspect(ServerPlayer player,AonDorNetwork.Check request){
        boolean selected=(request.slot()==0||request.slot()==1)&&POWER.equals(request.slot()==0?PowerData.selected(player):PowerData.secondary(player));
        return !allowed(player)||!selected||!request.dimension().equals(player.level().dimension().location().toString())
            ? "AonDor is no longer available in this power slot."
            :validate(request.drawing(),CATALOGS.computeIfAbsent(player.server,AonDorService::load));
    }
    public static void check(ServerPlayer player,AonDorNetwork.Check request){
        long now=player.level().getGameTime();var previous=CHECKED.get(player);if(previous!=null&&now>=previous&&now-previous<10)return;CHECKED.put(player,now);
        if(permitted(player,request.dimension(),request.slot())){boolean learned=false;for(var glyph:request.drawing().glyphs())learned|=learnDrawing(player,glyph,catalog(player));if(learned)library(player,"Aon learned.");}
        String message=inspect(player,request);
        if(message.length()>2048)message=message.substring(0,2048);
        PacketDistributor.sendToPlayer(player,new AonDorNetwork.Result(request.nonce(),player.level().dimension().location().toString(),message));
    }
    private static final Map<ServerPlayer,Long> CASTED=new WeakHashMap<>();
    public static String execute(ServerPlayer player,AonDorNetwork.Check request){
        if(!permitted(player,request.dimension(),request.slot()))return "AonDor is no longer available in this power slot.";
        if(!request.drawing().arguments().isEmpty())return "Modifiers are not available yet.";
        var match=AonRecognition.assess(request.drawing().main(),catalog(player),AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get());
        if(match.template()==null||!match.template().effect())return "Aon not recognized unambiguously; no spell cast.";
        long now=player.serverLevel().getGameTime();var last=CASTED.get(player);if(last!=null&&now>=last&&now-last<AonDorConfig.CAST_COOLDOWN.get())return "Wait a moment before casting again.";
        CASTED.put(player,now);
        double strength=Math.max(.1,Math.min(4,player.getAttributeValue(PowerAttributes.AONDOR)))*match.potency(AonDorConfig.FALLBACK_POTENCY.get());
        return AonDorEffects.cast(player,match.template().id(),strength);
    }
    public static void cast(ServerPlayer player,AonDorNetwork.Check request){
        if(!requestAllowed(player))return;
        String message=execute(player,request);player.displayClientMessage(net.minecraft.network.chat.Component.literal(message),true);
        PacketDistributor.sendToPlayer(player,new AonDorNetwork.Result(request.nonce(),player.level().dimension().location().toString(),message));
    }
    private AonDorService(){}
    public static AonDorService events(){return new AonDorService();}
}
