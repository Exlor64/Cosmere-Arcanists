package dev.cosmerearcanists.power;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.common.util.FakePlayer;
/** GameTest fixture bypasses fresh fake-player join/PvP guards, retaining vanilla/NeoForge actual damage processing. */
public class DamageableTestPlayer extends FakePlayer {
    public DamageableTestPlayer(ServerLevel level,GameProfile profile){super(level,profile);}
    @Override public boolean isInvulnerableTo(DamageSource source){return false;}
    @Override public boolean hurt(DamageSource source,float amount){damageContainers.push(new net.neoforged.neoforge.common.damagesource.DamageContainer(source,amount));try{actuallyHurt(source,amount);return true;}finally{damageContainers.pop();}}
}
