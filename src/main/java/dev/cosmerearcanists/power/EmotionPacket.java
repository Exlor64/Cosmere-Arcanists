package dev.cosmerearcanists.power;

import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dev.cosmerearcanists.CosmereArcanists;

public record EmotionPacket(String dimension,int zincMode,int brassMode,int dominant,float level,int target,int targetDominant) implements CustomPacketPayload {
    public EmotionPacket(String dimension,int zincMode,int brassMode,int dominant,float level){this(dimension,zincMode,brassMode,dominant,level,-1,-1);}
    public static Consumer<EmotionPacket> receiver=p->{};
    public static final Type<EmotionPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"emotional_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EmotionPacket> CODEC=new StreamCodec<>() {
        public EmotionPacket decode(RegistryFriendlyByteBuf b){String dim=b.readUtf(128);int z=b.readVarInt(),r=b.readVarInt(),d=b.readInt();float strength=b.readFloat();int target=b.readVarInt(),targetDominant=b.readInt();if(target< -1||targetDominant< -1||targetDominant>=5||target== -1&&targetDominant!= -1||z<0||z>=5||r<0||r>=5||d< -1||d>=5||!Float.isFinite(strength)||strength<0||strength>16)throw new IllegalArgumentException("Invalid emotional state");return new EmotionPacket(dim,z,r,d,strength,target,targetDominant);}
        public void encode(RegistryFriendlyByteBuf b,EmotionPacket p){b.writeUtf(p.dimension,128);b.writeVarInt(p.zincMode);b.writeVarInt(p.brassMode);b.writeInt(p.dominant);b.writeFloat(p.level);b.writeVarInt(p.target);b.writeInt(p.targetDominant);}
    };
    @Override public Type<EmotionPacket> type(){return TYPE;}
}
