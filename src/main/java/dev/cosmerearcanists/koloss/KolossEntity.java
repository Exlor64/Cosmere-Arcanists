package dev.cosmerearcanists.koloss;
import dev.cosmerearcanists.ScadrialWorld;
import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
/** A native natural-spawn group shares one leader; no manually added mobs or bypassed mob caps. */
public class KolossEntity extends Monster {
    private UUID packId,leaderId;
    private boolean leader;
    public KolossEntity(EntityType<? extends KolossEntity> type,Level level){super(type,level);xpReward=15;setCanPickUpLoot(false);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,32).add(Attributes.ARMOR,4).add(Attributes.KNOCKBACK_RESISTANCE,.4).add(Attributes.SCALE,1);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.0,false));
        goalSelector.addGoal(3,new FollowPackLeaderGoal(this));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,12));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,AbstractVillager.class,false));
    }
    public static boolean spawnDimension(LevelAccessor level){return level instanceof WorldGenLevel world&&world.getLevel().dimension().location().toString().equals(ScadrialWorld.ID);}
    public static boolean canSpawn(EntityType<KolossEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){return KolossConfig.SPAWNING.get()&&spawnDimension(level)&&level.canSeeSky(pos)&&Monster.checkMonsterSpawnRules(type,level,reason,pos,random);}
    public static final class PackData implements SpawnGroupData {
        public final UUID id=UUID.randomUUID();
        public final UUID leader;
        public final float leaderScale;
        private int members;
        PackData(UUID leader,float scale){this.leader=leader;leaderScale=scale;}
    }
    @Nullable @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,@Nullable SpawnGroupData data){
        super.finalizeSpawn(level,difficulty,reason,data);
        var pack=data instanceof PackData p?p:new PackData(getUUID(),1.6f+random.nextFloat()*.2f);
        packId=pack.id;leaderId=pack.leader;leader=pack.members==0;
        // Cycling bands guarantee variety in a successfully spawned pack; every follower is smaller.
        float scale=leader?pack.leaderScale:.8f+((pack.members-1)%4)*.18f+random.nextFloat()*.1f;
        pack.members++;configureSize(scale);setLeftHanded(false);setCanPickUpLoot(false);
        setItemSlot(EquipmentSlot.MAINHAND,leader?new ItemStack(KolossContent.SWORD.get()):ItemStack.EMPTY);
        setDropChance(EquipmentSlot.MAINHAND,(float)(KolossConfig.SWORD_DROP.get()>=1?2:KolossConfig.SWORD_DROP.get()));
        return pack;
    }
    private void configureSize(float scale){
        getAttribute(Attributes.SCALE).setBaseValue(scale);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(KolossConfig.HEALTH.get()*scale*scale);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(KolossConfig.DAMAGE.get()*scale);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(KolossConfig.SPEED.get());
        getAttribute(Attributes.ARMOR).setBaseValue(KolossConfig.ARMOR.get());
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(Math.min(.85,.4*scale));
        setHealth(getMaxHealth());refreshDimensions();
    }
    @Override public boolean checkSpawnObstruction(LevelReader level){
        var box=getBoundingBox();
        // Natural spawning checks obstruction before finalizeSpawn chooses scale.
        if(packId==null)box=box.inflate(.44,0,.44).expandTowards(0,2.15,0);
        return super.checkSpawnObstruction(level)&&level.noCollision(this,box);
    }
    public boolean isPackLeader(){return leader;}
    @Nullable public UUID packId(){return packId;}
    @Nullable public KolossEntity packLeader(){
        if(leader||leaderId==null||!(level() instanceof ServerLevel server))return null;
        var e=server.getEntity(leaderId);return e instanceof KolossEntity k&&k.isAlive()&&Objects.equals(k.packId,packId)?k:null;
    }
    @Override public int getMaxSpawnClusterSize(){return 7;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(packId!=null)tag.putUUID("KolossPack",packId);if(leaderId!=null)tag.putUUID("KolossLeader",leaderId);tag.putBoolean("KolossPackLeader",leader);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);packId=tag.hasUUID("KolossPack")?tag.getUUID("KolossPack"):null;leaderId=tag.hasUUID("KolossLeader")?tag.getUUID("KolossLeader"):null;leader=tag.getBoolean("KolossPackLeader");setCanPickUpLoot(false);}
}
