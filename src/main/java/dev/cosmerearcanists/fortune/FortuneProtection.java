package dev.cosmerearcanists.fortune;

import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.mixin.FortuneExplosionAccess;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Physical near misses and last-second TNT duds, funded by ordinary ongoing Fortune tapping. */
public final class FortuneProtection {
    private static final String PROJECTILE_KEY="cosmerearcanists_fortune_protection";
    private static final String TNT_CHECKED="cosmerearcanists_fortune_tnt_checked",TNT_DUD="cosmerearcanists_fortune_tnt_dud";
    public static double projectileChance(ServerPlayer p){return FortuneConfig.DEFLECTION.get()?FortuneProtectionMath.chance(ChromiumFeruchemy.fundedLevels(p),FortuneConfig.DEFLECT_STEP.get(),FeruchemicalEnhancement.fractionCap(p,"chromium",FortuneConfig.DEFLECT_CAP.get())):0;}
    public static double tntChance(ServerPlayer p){return FortuneConfig.TNT_DUDS.get()?FortuneProtectionMath.chance(ChromiumFeruchemy.fundedLevels(p),FortuneConfig.TNT_STEP.get(),FeruchemicalEnhancement.fractionCap(p,"chromium",FortuneConfig.TNT_CAP.get())):0;}
    private static Vec3 motion(ServerPlayer p){return p.getDeltaMovement().lengthSqr()<=16?p.getDeltaMovement():Vec3.ZERO;}
    private static boolean uncertain(List<TemporalService.Frame> frames){return frames.isEmpty()||frames.stream().anyMatch(f->(f.warning()&TemporalPacket.UNCERTAIN)!=0);}
    private static boolean threatens(Projectile projectile,ServerPlayer p,List<TemporalService.Frame> frames){
        if(uncertain(frames))return false;var previous=projectile.position();int tick=0;for(var frame:frames){tick++;var box=p.getBoundingBox().move(motion(p).scale(tick)).inflate(.08+projectile.getBbWidth()*.5);if(box.contains(previous)||box.clip(previous,frame.position()).isPresent())return true;previous=frame.position();}return false;
    }
    /** Find a small clear trajectory without moving the entity, canceling impacts or changing speed. */
    public static Optional<Vec3> deflection(Projectile projectile,ServerPlayer player){
        var velocity=projectile.getDeltaMovement();int horizon=FortuneConfig.DEFLECT_HORIZON.get();if(velocity.lengthSqr()<.0025||velocity.lengthSqr()>256||!threatens(projectile,player,TemporalService.trace(projectile,velocity,horizon)))return Optional.empty();
        var side=velocity.cross(new Vec3(0,1,0));if(side.lengthSqr()<1e-8)side=velocity.cross(new Vec3(1,0,0));side=side.normalize();double away=side.dot(projectile.position().subtract(player.getBoundingBox().getCenter()));if(away<0||Math.abs(away)<1e-8&&(projectile.getUUID().getLeastSignificantBits()&1)!=0)side=side.scale(-1);
        for(double fraction:new double[]{.25,.5,.75,1})for(int sign:new int[]{1,-1}){double[] turned=FortuneProtectionMath.turn(velocity.x,velocity.y,velocity.z,side.x*sign,side.y*sign,side.z*sign,Math.toRadians(FortuneConfig.DEFLECT_ANGLE.get()*fraction));var candidate=new Vec3(turned[0],turned[1],turned[2]);var frames=TemporalService.trace(projectile,candidate,horizon);if(!uncertain(frames)&&!threatens(projectile,player,frames))return Optional.of(candidate);}return Optional.empty();
    }
    private static boolean checked(Projectile projectile,UUID player){var list=projectile.getPersistentData().getCompound(PROJECTILE_KEY).getList("players",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++)if(list.getCompound(i).hasUUID("id")&&list.getCompound(i).getUUID("id").equals(player))return true;return list.size()>=32;}
    private static void mark(Projectile projectile,UUID player){var root=projectile.getPersistentData();var data=root.getCompound(PROJECTILE_KEY);var list=data.getList("players",Tag.TAG_COMPOUND);var tag=new CompoundTag();tag.putUUID("id",player);list.add(tag);data.put("players",list);root.put(PROJECTILE_KEY,data);}
    public static boolean tryDeflect(Projectile projectile,ServerPlayer player){
        if(projectile.level().isClientSide||projectile.isRemoved()||projectile instanceof FishingHook||projectile.noPhysics||projectile.getOwner()==player||projectile.level()!=player.level()||projectile.getPersistentData().getCompound(PROJECTILE_KEY).getBoolean("turned")||checked(projectile,player.getUUID())||projectile.distanceToSqr(player)>Math.pow(FortuneConfig.DEFLECT_RANGE.get(),2))return false;
        double chance=projectileChance(player);if(chance<=0)return false;var direction=deflection(projectile,player);if(direction.isEmpty())return false;
        mark(projectile,player.getUUID());if(player.getRandom().nextDouble()>=chance)return false;
        projectile.setDeltaMovement(direction.get());projectile.hasImpulse=true;projectile.hurtMarked=true;var data=projectile.getPersistentData().getCompound(PROJECTILE_KEY);data.putBoolean("turned",true);projectile.getPersistentData().put(PROJECTILE_KEY,data);return true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void projectileTick(EntityTickEvent.Pre e){
        if(!FortuneConfig.DEFLECTION.get()||!ChromiumFeruchemy.configured()||!(e.getEntity() instanceof Projectile projectile)||!(projectile.level() instanceof ServerLevel level)||projectile.isRemoved()||projectile instanceof FishingHook||projectile.noPhysics||projectile.getDeltaMovement().lengthSqr()<.0025||projectile.getPersistentData().getCompound(PROJECTILE_KEY).getBoolean("turned"))return;
        var players=level.getEntitiesOfClass(ServerPlayer.class,projectile.getBoundingBox().inflate(FortuneConfig.DEFLECT_RANGE.get()),p->projectileChance(p)>0&&projectile.getOwner()!=p&&!checked(projectile,p.getUUID()));players.sort(Comparator.comparingDouble(projectile::distanceToSqr));for(var p:players.subList(0,Math.min(8,players.size())))if(tryDeflect(projectile,p))break;
    }
    private static boolean loaded(ServerLevel level,AABB area){int x0=(int)Math.floor(area.minX)>>4,x1=(int)Math.floor(area.maxX)>>4,z0=(int)Math.floor(area.minZ)>>4,z1=(int)Math.floor(area.maxZ)>>4;if((long)(x1-x0+1)*(z1-z0+1)>64)return false;for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(!level.hasChunkAt(new BlockPos(x<<4,0,z<<4)))return false;return true;}
    public static boolean inBlastZone(Explosion explosion,ServerPlayer player){
        if(!(player.level() instanceof ServerLevel level))return false;var access=(FortuneExplosionAccess)explosion;double radius=access.cosmerearcanists$radius()*2.0;if(!Double.isFinite(radius)||radius<=0||radius>FortuneConfig.TNT_RANGE_CAP.get())return false;var center=new Vec3(access.cosmerearcanists$x(),access.cosmerearcanists$y(),access.cosmerearcanists$z());if(player.distanceToSqr(center)>radius*radius)return false;var area=player.getBoundingBox().minmax(new AABB(center,center));return loaded(level,area)&&Explosion.getSeenPercent(center,player)>0;
    }
    /** One shared roll at the actual explosion boundary, using the strongest eligible player's chance. */
    public static boolean tryDud(Explosion explosion,List<? extends ServerPlayer> players){
        var access=(FortuneExplosionAccess)explosion;if(!(access.cosmerearcanists$source() instanceof PrimedTnt tnt)||!(tnt.level() instanceof ServerLevel level)||tnt.getFuse()>0)return false;
        var data=tnt.getPersistentData();if(data.getBoolean(TNT_DUD))return true;if(data.getBoolean(TNT_CHECKED)||!FortuneConfig.TNT_DUDS.get())return false;
        double chance=0;ServerPlayer beneficiary=null;for(var player:players){double candidate=tntChance(player);if(candidate>chance&&player.level()==level&&inBlastZone(explosion,player)){chance=candidate;beneficiary=player;}}
        data.putBoolean(TNT_CHECKED,true);if(beneficiary==null||chance<=0||beneficiary.getRandom().nextDouble()>=chance)return false;
        Item item=tnt.getBlockState().getBlock().asItem();if(item==Items.AIR)item=Items.TNT;var drop=new ItemEntity(level,access.cosmerearcanists$x(),access.cosmerearcanists$y(),access.cosmerearcanists$z(),new ItemStack(item));drop.setDefaultPickUpDelay();if(!level.addFreshEntity(drop))return false;
        data.putBoolean(TNT_DUD,true);tnt.discard();level.playSound(null,drop.blockPosition(),SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.7f,1.3f);return true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void explosion(ExplosionEvent.Start e){if(e.getLevel() instanceof ServerLevel level&&tryDud(e.getExplosion(),level.players()))e.setCanceled(true);}
}
