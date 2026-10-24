package dev.cosmerearcanists.hemalurgy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
/** Requests carry only coordinates and native menu revision; never trust client items or benefits. */
public final class HemalurgyNetwork {
    public record Request(int action,int container,int state,boolean back,double u,double v,int point) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("cosmerearcanists","hemalurgy"));
        public Request{if(action<0||action>2||!Double.isFinite(u)||!Double.isFinite(v)||u<0||u>1||v<0||v>1||point< -1||point>=13)throw new IllegalArgumentException("Invalid Hemalurgy request");}
        @Override public Type<Request> type(){return TYPE;}
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=new StreamCodec<>(){public Request decode(RegistryFriendlyByteBuf b){return new Request(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readVarInt());}public void encode(RegistryFriendlyByteBuf b,Request p){b.writeVarInt(p.action);b.writeVarInt(p.container);b.writeVarInt(p.state);b.writeBoolean(p.back);b.writeDouble(p.u);b.writeDouble(p.v);b.writeVarInt(p.point);}};
    }
    public static void request(ServerPlayer p,Request packet){if(packet.action()==0){HemalurgyMenu.open(p);return;}if(!(p.containerMenu instanceof HemalurgyMenu menu)||menu.containerId!=packet.container()||!menu.stillValid(p))return;if(menu.getStateId()!=packet.state()){menu.broadcastFullState();return;}if(packet.action()==1)ImplantService.place(p,packet.back(),packet.u(),packet.v());else if(packet.point()>=0&&ImplantRules.POINTS.get(packet.point()).back()==packet.back())ImplantService.remove(p,packet.point());menu.refresh();menu.broadcastFullState();}
    public static void register(PayloadRegistrar r){r.playToServer(Request.TYPE,Request.CODEC,(packet,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer p)request(p,packet);}));}
    private HemalurgyNetwork(){}
}
