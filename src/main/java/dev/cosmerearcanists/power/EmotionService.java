package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.investiture.EmotionalInfluenceEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EmotionService {
    private static final TagKey<EntityType<?>> IMMUNE=TagKey.create(Registries.ENTITY_TYPE,id("emotionally_immune"));
    private static final TagKey<Item> SHIELD=TagKey.create(Registries.ITEM,id("emotion_shielding"));
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final Set<Mob> GOALS=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Mob,LivingEntity> ANGER_TARGETS=new WeakHashMap<>();
    private static final Map<net.minecraft.world.entity.ai.Brain<?>,java.lang.ref.WeakReference<Mob>> ANGER_BRAINS=new WeakHashMap<>();
    private static final Set<UUID> SENT=new HashSet<>();
    private record Key(UUID source,String power,Emotion emotion) { }
    private record Influence(ServerPlayer source,String power,Emotion emotion,double amount,double allomanticStrength,long expires) { }
    private static final class State {
        final LivingEntity target;final double[] baseline={.20,.20,.30,.20,.20},levels=new double[5];
        final Map<Key,Influence> influences=new HashMap<>();Emotion dominant;ServerPlayer source;double intensity,complianceStrength;long burstUntil,burstCooldown;boolean inducedLove;
        State(LivingEntity target){this.target=target;if(target instanceof Mob mob && (mob.getTarget()!=null || mob instanceof net.minecraft.world.entity.monster.Enemy))baseline[0]=.6;if(target instanceof Animal animal && animal.isInLove())baseline[Emotion.LOVE.ordinal()]=.6;}
    }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,path);}
    public static boolean configured(String power){return Emotion.power(power) && EmotionConfig.ENABLED.get() && (power.equals("a:zinc")?EmotionConfig.ZINC.get():EmotionConfig.BRASS.get()) && PowerConfig.ENABLED.get() && ServerConfig.ALLOMANCY.enabled().get() && ServerConfig.ALLOMANCY.metals().get(power.equals("a:zinc")?Metal.ZINC:Metal.BRASS).get();}
    public static double strength(ServerPlayer p,String power) {
        if(!configured(power)||!p.isAlive()||p.isSpectator()||PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,power.substring(2))<=0)return 0;
        int state=PowerData.state(p,power);if(state==3&&!PowerConfig.FLARING.get())state=2;
        return PowerAttributes.effectiveStrength(p,power)*PowerRules.levelMultiplier(state,PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());
    }
    public static int mode(ServerPlayer p,String power){return Math.floorMod(PowerData.data(p).getInt("emotion_"+power.substring(2)),5);}
    public static void mode(ServerPlayer p,String power,int value){if(Emotion.power(power) && value>=0 && value<5)PowerData.data(p).putInt("emotion_"+power.substring(2),value);}
    public static void tickFuel(ServerPlayer p){for(String power:List.of("a:zinc","a:brass")) {
        if(strength(p,power)>0 && PowerConfig.CONSUMPTION.get()) {
            int state=PowerData.state(p,power);if(state==3&&!PowerConfig.FLARING.get()){state=2;PowerData.state(p,power,2);}
            EnhancementService.burn(p,power.substring(2),EmotionConfig.DRAIN.get()/20*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));
        }
        if(PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,power.substring(2))<=0 && PowerData.state(p,power)>0)PowerData.state(p,power,0);
    }}
    public static boolean eligible(LivingEntity target) {
        return target.isAlive() && !target.isSpectator() && !target.getType().is(IMMUNE) && !(target instanceof net.minecraft.world.entity.decoration.ArmorStand)
            && (target instanceof Player?EmotionConfig.PLAYERS.get():target instanceof Mob && EmotionConfig.MOBS.get() && (!(target instanceof Animal)||EmotionConfig.ANIMALS.get()));
    }
    static boolean shielded(LivingEntity target){return EmotionConfig.ALUMINUM.get() && (target.getItemBySlot(EquipmentSlot.HEAD).is(SHIELD) || target.level() instanceof net.minecraft.server.level.ServerLevel level && MetalDetector.containsAluminum(level.getServer(),target.getItemBySlot(EquipmentSlot.HEAD)));}
    public static void activate(ServerPlayer p,String power,AbilityDispatcher.Phase phase) {
        if(phase==AbilityDispatcher.Phase.STOP || strength(p,power)<=0)return;
        double range=Math.min(EmotionConfig.RANGE_CAP.get(),EmotionConfig.RANGE.get()*Math.sqrt(strength(p,power)));
        boolean area=EmotionConfig.AREA.get() && PowerService.holdingAlternateAbility(p,power);
        var targets=p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range),e->e!=p && eligible(e) && e.distanceToSqr(p)<=range*range);
        targets.sort(Comparator.comparingDouble(e->e.distanceToSqr(p)));
        if(!area) {
            var from=p.getEyePosition();var to=from.add(p.getLookAngle().scale(range));
            targets.removeIf(e->!e.getBoundingBox().inflate(.3).contains(from) && e.getBoundingBox().inflate(.3).clip(from,to).isEmpty());
        }
        int count=0;for(var target:targets){apply(p,target,power,Emotion.values()[mode(p,power)]);if(++count>= (area?EmotionConfig.TARGETS.get():1))break;}
    }
    public static boolean apply(ServerPlayer source,LivingEntity target,String power,Emotion emotion) {
        double strength=strength(source,power);if(strength<=0 || !eligible(target)||source==target||source.level()!=target.level()||!EmotionConfig.effect(emotion)||shielded(target))return false;
        double range=Math.min(EmotionConfig.RANGE_CAP.get(),EmotionConfig.RANGE.get()*Math.sqrt(strength));double distance=target.distanceTo(source);if(distance>range)return false;
        double amount=strength*EmotionMath.falloff(distance,range)*(target instanceof Animal?EmotionConfig.ANIMAL_FACTOR.get():1);
        if(!EmotionalInfluenceEvent.allowed(target,strength))return false;
        var state=STATES.get(target.getUUID());if(state==null){if(STATES.size()>=EmotionConfig.AFFECTED.get())return false;state=new State(target);STATES.put(target.getUUID(),state);}
        state.influences.put(new Key(source.getUUID(),power,emotion),new Influence(source,power,emotion,(power.equals("a:zinc")?1:-1)*amount,strength,target.level().getGameTime()+EmotionConfig.DURATION.get()));
        refresh(state);if(target instanceof Mob mob && angerSuppressed(state))pacify(mob);
        if(emotion==Emotion.COMPLIANCE && state.dominant==Emotion.COMPLIANCE && state.source==source && amount>0 && power.equals("a:zinc"))DominationService.attempt(source,target,power,state.complianceStrength,strength);
        return true;
    }
    private static void refresh(State state) {
        long now=state.target.level().getGameTime();
        if(!eligible(state.target))state.influences.clear();
        state.influences.values().removeIf(i->now>=i.expires || !i.source.isAlive() || i.source.isRemoved() || i.source.level()!=state.target.level() || !configured(i.power) || !EmotionConfig.effect(i.emotion) || shielded(state.target) || !EmotionalInfluenceEvent.allowed(state.target,i.allomanticStrength));
        System.arraycopy(state.baseline,0,state.levels,0,5);var leading=new double[5];var owners=new ServerPlayer[5];double strongest=0;ServerPlayer fallback=null;
        for(var influence:state.influences.values()) {
            double value=influence.amount*Math.clamp((influence.expires-now)/(double)EmotionConfig.DURATION.get(),0,1);int n=influence.emotion.ordinal();state.levels[n]+=value;
            if(value>leading[n]){leading[n]=value;owners[n]=influence.source;}
            if(Math.abs(value)>strongest){strongest=Math.abs(value);fallback=influence.source;}
        }
        state.complianceStrength=Math.max(0,state.levels[Emotion.COMPLIANCE.ordinal()]-state.baseline[Emotion.COMPLIANCE.ordinal()]);
        for(int i=0;i<5;i++)state.levels[i]=Math.clamp(state.levels[i],0,16);
        Emotion old=state.dominant;state.dominant=state.influences.isEmpty()?null:Emotion.values()[EmotionMath.dominant(state.levels)];
        if(state.dominant!=null && state.levels[state.dominant.ordinal()]<=.001)state.dominant=null;
        int n=state.dominant==null?0:state.dominant.ordinal();state.source=owners[n]==null?fallback:owners[n];state.intensity=state.dominant==null?0:Math.max(0,state.levels[n]-state.baseline[n]);
        if(state.dominant==Emotion.FEAR && old!=Emotion.FEAR && now>=state.burstCooldown){state.burstUntil=now+EmotionConfig.FEAR_BURST.get();state.burstCooldown=now+EmotionConfig.FEAR_COOLDOWN.get();}
        if(state.dominant!=Emotion.FEAR)state.burstUntil=0;
        if(state.target instanceof Animal animal && state.dominant!=Emotion.LOVE && (state.inducedLove || !state.influences.isEmpty() && state.levels[Emotion.LOVE.ordinal()]<state.baseline[Emotion.LOVE.ordinal()]-1e-6)) {
            animal.resetLove();state.inducedLove=false;
        }
    }
    public static Emotion dominant(LivingEntity target){var s=STATES.get(target.getUUID());if(s==null)return null;refresh(s);return s.dominant;}
    public static double intensity(LivingEntity target){var s=STATES.get(target.getUUID());if(s==null)return 0;refresh(s);return s.intensity;}
    /** Soothing natural anger is behavioral suppression, not a source-only truce. */
    private static boolean angerSuppressed(State s){return s.dominant!=Emotion.ANGER && !s.influences.isEmpty() && s.levels[Emotion.ANGER.ordinal()]<s.baseline[Emotion.ANGER.ordinal()]-1e-6;}
    public static boolean pacified(LivingEntity target){var s=STATES.get(target.getUUID());if(s==null)return false;refresh(s);return angerSuppressed(s);}
    private static boolean inducedAggression(State s){return s.dominant==Emotion.ANGER && (s.intensity>1e-6 || s.baseline[Emotion.ANGER.ordinal()]<.6-1e-6);}
    public static boolean hasNativeCombat(Mob mob){
        if(mob instanceof net.minecraft.world.entity.monster.Enemy || mob instanceof net.minecraft.world.entity.monster.RangedAttackMob || mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null)return true;
        if(mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET,net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED))return true;
        return ((dev.cosmerearcanists.mixin.MobGoalsAccessor)mob).cosmerearcanists$goals().getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal || g.getGoal() instanceof net.minecraft.world.entity.ai.goal.RangedAttackGoal);
    }
    public static boolean nativeAnger(Mob mob){
        if(!EmotionConfig.NATIVE_ANGER.get() || mob.isNoAi() || DominationService.owner(mob)!=null || !hasNativeCombat(mob))return false;
        var s=STATES.get(mob.getUUID());if(s==null)return false;refresh(s);return inducedAggression(s);
    }
    private static Mob nearestAngerTarget(Mob mob){
        double range=EmotionConfig.ANGER_RANGE.get();
        return mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(range),e->e!=mob && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(mob) && mob.canAttack(e) && e.distanceToSqr(mob)<=range*range).stream()
            .min(Comparator.<Mob>comparingDouble(e->e.distanceToSqr(mob)).thenComparingInt(Mob::getId)).orElse(null);
    }
    public record AngerBrainOverride(LivingEntity target) { }
    public static AngerBrainOverride angerBrainTarget(net.minecraft.world.entity.ai.Brain<?> brain){
        var ref=ANGER_BRAINS.get(brain);var mob=ref==null?null:ref.get();
        if(mob==null || !nativeAnger(mob))return null;var target=nearestAngerTarget(mob);if(target!=null)ANGER_TARGETS.put(mob,target);return new AngerBrainOverride(target);
    }
    public static LivingEntity redirectAnger(Mob mob){
        var target=nearestAngerTarget(mob);if(mob.getTarget()!=target)mob.setTarget(target);
        var brain=mob.getBrain();if(nativeAnger(mob))ANGER_BRAINS.put(brain,new java.lang.ref.WeakReference<>(mob));if(brain.checkMemory(MemoryModuleType.ATTACK_TARGET,net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)){
            if(target==null)brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);else brain.setMemory(MemoryModuleType.ATTACK_TARGET,target);
        }
        if(nativeAnger(mob)){if(target==null)ANGER_TARGETS.remove(mob);else ANGER_TARGETS.put(mob,target);}
        return target;
    }
    public static void releaseAngerTarget(Mob mob){
        ANGER_BRAINS.remove(mob.getBrain());var old=ANGER_TARGETS.remove(mob);if(old==null)return;
        if(mob.getTarget()==old)mob.setTarget(null);
        if(mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==old)mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }
    public static boolean forcesBehavior(Mob mob){if(DominationService.owner(mob)!=null)return true;var s=STATES.get(mob.getUUID());if(s==null)return false;refresh(s);return inducedAggression(s) && !nativeAnger(mob) || s.intensity>1e-6 && (s.dominant==Emotion.FEAR||s.dominant==Emotion.COMPLIANCE);}
    private static void pacify(Mob mob){
        boolean attacking=mob.getTarget()!=null || mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
        mob.setTarget(null);mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        if(attacking){mob.getNavigation().stop();mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);}
        if(mob instanceof net.minecraft.world.entity.monster.Creeper creeper && !creeper.isIgnited())creeper.setSwellDir(-1);
    }
    public static boolean trusts(LivingEntity target,LivingEntity other){var s=STATES.get(target.getUUID());return s!=null && dominant(target)==Emotion.TRUST && s.source==other;}
    public static boolean calmPanic(Mob target){var s=STATES.get(target.getUUID());return s!=null && dominant(target)==Emotion.TRUST && !target.isOnFire();}
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,String name,double amount){var a=p.getAttribute(attribute);if(a==null)return;var key=id("emotion_"+name);var old=a.getModifier(key);if(old!=null && old.amount()==amount)return;a.removeModifier(key);if(amount!=0)a.addTransientModifier(new AttributeModifier(key,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    private static void playerEffects(ServerPlayer p,State s) {
        Emotion e=s==null?null:s.dominant;double n=s==null?0:s.intensity;
        modifier(p,Attributes.ATTACK_DAMAGE,"damage",e==Emotion.ANGER?EmotionMath.capped(n,EmotionConfig.ANGER_DAMAGE.get(),.75):e==Emotion.FEAR?-EmotionMath.capped(n,EmotionConfig.FEAR_DAMAGE.get(),.9):0);
        modifier(p,Attributes.ATTACK_SPEED,"attack_speed",e==Emotion.FEAR?-EmotionMath.capped(n,EmotionConfig.FEAR_ATTACK_SPEED.get(),.9):0);
        modifier(p,Attributes.MOVEMENT_SPEED,"speed",e==Emotion.FEAR && p.level().getGameTime()<s.burstUntil?EmotionMath.capped(n,EmotionConfig.FEAR_SPEED.get(),1):0);
        if(DominationService.owner(p)==null && e==Emotion.COMPLIANCE && s.source!=null && p.level().getGameTime()%4==0 && p.distanceToSqr(s.source)>4) {
            var direction=s.source.position().subtract(p.position()).multiply(1,0,1).normalize();var impulse=direction.scale(Math.min(EmotionConfig.COMPLIANCE_CAP.get(),EmotionConfig.COMPLIANCE_FORCE.get()*n));
            p.setDeltaMovement(p.getDeltaMovement().add(impulse));
            PacketDistributor.sendToPlayer(p,new PowerNetwork.Impulse(p.level().dimension().location().toString(),impulse.x,0,impulse.z,100,false));
        }
    }
    public static void updatePlayer(ServerPlayer p){var s=STATES.get(p.getUUID());if(s!=null)refresh(s);playerEffects(p,s);}
    public static double loveReduction(ServerPlayer attacker,LivingEntity victim){var s=STATES.get(attacker.getUUID());return s!=null && dominant(attacker)==Emotion.LOVE && s.source==victim?EmotionMath.capped(s.intensity,EmotionConfig.LOVE_REDUCTION.get(),.75):0;}
    public static void navigate(Mob mob,Vec3 target,double speed) {
        int padding=(int)Math.clamp(mob.getAttributeValue(Attributes.FOLLOW_RANGE)+8,24,128);var pos=mob.blockPosition();
        if(mob.level().hasChunksAt(pos.offset(-padding,-8,-padding),pos.offset(padding,8,padding)) && mob.level().hasChunkAt(BlockPos.containing(target)))mob.getNavigation().moveTo(target.x,target.y,target.z,speed);
    }
    public static void behave(Mob mob) {
        if(DominationService.behave(mob))return;
        var state=STATES.get(mob.getUUID());if(state==null)return;refresh(state);var source=state.source;var emotion=state.dominant;if(mob.isNoAi())return;
        if(angerSuppressed(state))pacify(mob);
        if(source==null||emotion==null)return;
        if(inducedAggression(state)) {
            var target=redirectAnger(mob);if(target==null)return;
            if(nativeAnger(mob))return; // Target only: leave movement, range, aiming and damage to native combat AI.
            mob.getLookControl().setLookAt(target,30,30);
            if(mob.distanceToSqr(target)>4)navigate(mob,target.position(),1);else if(mob.tickCount%20==0){if(mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null)mob.doHurtTarget(target);else target.hurt(mob.damageSources().mobAttack(mob),(float)(2*(mob instanceof Animal?EmotionConfig.ANIMAL_FACTOR.get():1)));}
        } else if(emotion==Emotion.FEAR) {
            mob.setTarget(null);var away=mob.position().subtract(source.position()).multiply(1,0,1).normalize();if(away.lengthSqr()<.01)away=new Vec3(1,0,0);navigate(mob,mob.position().add(away.scale(8)),1.25);
        } else if(emotion==Emotion.COMPLIANCE) {mob.setTarget(null);if(mob.distanceToSqr(source)>6.25)navigate(mob,source.position(),.45);else mob.getNavigation().stop();}
        else if(emotion==Emotion.TRUST) {
            if(mob.getTarget()==source)mob.setTarget(null);
            if(mob instanceof Villager){mob.getBrain().eraseMemory(MemoryModuleType.HURT_BY);mob.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);mob.getBrain().eraseMemory(MemoryModuleType.AVOID_TARGET);}
        } else if(emotion==Emotion.LOVE && EmotionConfig.BREEDING.get() && mob instanceof Animal animal && !animal.isBaby() && animal.getAge()==0 && state.intensity>=.25 && !animal.isInLove()){animal.setInLove(source);state.inducedLove=true;}
        else if(emotion==Emotion.LOVE && EmotionConfig.BREEDING.get() && mob instanceof Villager villager && villager.canBreed()) {
            var mates=mob.level().getEntitiesOfClass(Villager.class,mob.getBoundingBox().inflate(8),v->v!=villager && v.canBreed() && dominant(v)==Emotion.LOVE);
            if(!mates.isEmpty()){villager.getBrain().setMemory(MemoryModuleType.BREED_TARGET,mates.getFirst());villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET,mates.getFirst());}
        }
    }
    public static void discount(Villager villager,Player customer){var s=STATES.get(villager.getUUID());if(!EmotionConfig.TRADES.get()||!trusts(villager,customer)||s==null)return;double fraction=EmotionMath.capped(Math.max(s.intensity,.1),EmotionConfig.DISCOUNT.get(),.5);for(var offer:villager.getOffers())offer.addToSpecialPriceDiff(-(int)Math.floor(offer.getBaseCostA().getCount()*fraction));}
    @SubscribeEvent public void targeting(LivingChangeTargetEvent event){var owner=DominationService.owner(event.getEntity());if(owner!=null){if(!DominationService.permits(event.getEntity(),event.getNewAboutToBeSetTarget()))event.setNewAboutToBeSetTarget(null);return;}var s=STATES.get(event.getEntity().getUUID());if(s==null)return;refresh(s);if(event.getEntity() instanceof Mob mob && nativeAnger(mob)){event.setNewAboutToBeSetTarget(nearestAngerTarget(mob));return;}if(angerSuppressed(s) || s.dominant==Emotion.FEAR||s.dominant==Emotion.COMPLIANCE || (s.dominant==Emotion.TRUST||s.dominant==Emotion.LOVE) && event.getNewAboutToBeSetTarget()==s.source)event.setNewAboutToBeSetTarget(null);}
    @SubscribeEvent(priority=EventPriority.HIGH) public void loveDamage(LivingDamageEvent.Pre event){if(event.getSource().getEntity() instanceof LivingEntity attacker){var owner=DominationService.owner(attacker);if(owner!=null && (event.getEntity()==owner || DominationService.owner(event.getEntity())==owner)){event.setNewDamage(0);return;}}if(event.getSource().getEntity() instanceof ServerPlayer attacker)event.setNewDamage((float)(event.getNewDamage()*(1-loveReduction(attacker,event.getEntity()))));}
    @SubscribeEvent public void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide && event.getEntity() instanceof Mob mob && GOALS.add(mob)){
        var goals=(dev.cosmerearcanists.mixin.MobGoalsAccessor)mob;goals.cosmerearcanists$goals().addGoal(0,new EmotionalGoal(mob));goals.cosmerearcanists$targets().addGoal(0,new AngerTargetGoal(mob));
    }}
    @SubscribeEvent public void tick(ServerTickEvent.Post event) {
        DominationService.tick();
        for(var it=STATES.values().iterator();it.hasNext();) {
            var s=it.next();refresh(s);
            if(!s.target.isAlive()||s.target.isRemoved()||s.influences.isEmpty()){if(s.target instanceof ServerPlayer p)playerEffects(p,null);if(s.target instanceof Mob mob)releaseAngerTarget(mob);it.remove();continue;}
            if(s.target instanceof ServerPlayer p)playerEffects(p,s);else if(s.target instanceof Mob mob && mob.tickCount%4==0)behave(mob);
        }
        if(event.getServer().getTickCount()%4==0)for(var p:event.getServer().getPlayerList().getPlayers()) {
            boolean relevant=PowerAttributes.strength(p,"a:zinc")>0||PowerAttributes.strength(p,"a:brass")>0||STATES.containsKey(p.getUUID());
            if(relevant||SENT.remove(p.getUUID()))sync(p);if(relevant)SENT.add(p.getUUID());
        }
    }
    public record Inspection(int entity,int emotion) { }
    /** Private, bounded nearby readout, available only while actually burning an emotional metal. */
    public static List<Inspection> inspect(ServerPlayer p){
        double power=Math.max(strength(p,"a:zinc"),strength(p,"a:brass"));
        if(!EmotionConfig.INDICATOR.get() || power<=0 || !p.isAlive() || p.isSpectator())return List.of();
        double range=Math.min(EmotionConfig.RANGE_CAP.get(),EmotionConfig.RANGE.get()*Math.sqrt(power));
        var result=new java.util.ArrayList<Inspection>();var from=p.getEyePosition();
        var targets=p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range),e->e!=p && eligible(e) && e.distanceToSqr(p)<=range*range);
        targets.sort(java.util.Comparator.comparingDouble(e->e.distanceToSqr(p)));
        for(var target:targets){
            if(result.size()>=256)break;
            if(shielded(target) || !EmotionalInfluenceEvent.allowed(target,power))continue;
            var hit=target.getEyePosition();var delta=hit.subtract(from);int steps=(int)Math.ceil(delta.length()*4);boolean loaded=true;
            for(int i=0;i<=steps;i++)if(!p.serverLevel().hasChunkAt(BlockPos.containing(from.add(delta.scale(steps==0?0:(double)i/steps))))){loaded=false;break;}
            if(!loaded || p.serverLevel().clip(new net.minecraft.world.level.ClipContext(from,hit,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS)continue;
            if(DominationService.owner(target)!=null){result.add(new Inspection(target.getId(),Emotion.COMPLIANCE.ordinal()));continue;}
            var state=STATES.get(target.getUUID());if(state!=null)refresh(state);
            int emotion=state!=null&&!state.influences.isEmpty()?(state.dominant==null?-1:state.dominant.ordinal()):EmotionMath.dominant(new State(target).baseline);
            result.add(new Inspection(target.getId(),emotion));
        }
        return List.copyOf(result);
    }
    public static void sync(ServerPlayer p){var s=STATES.get(p.getUUID());if(s!=null)refresh(s);PacketDistributor.sendToPlayer(p,new EmotionPacket(p.level().dimension().location().toString(),mode(p,"a:zinc"),mode(p,"a:brass"),s==null||s.dominant==null?-1:s.dominant.ordinal(),(float)(s==null?0:s.intensity),inspect(p)));}
    public static void clear(LivingEntity target){STATES.remove(target.getUUID());if(target instanceof Mob mob)releaseAngerTarget(mob);if(target instanceof ServerPlayer p)playerEffects(p,null);}
    private static void removeSource(UUID source){for(var state:STATES.values())state.influences.entrySet().removeIf(e->e.getKey().source.equals(source));}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){DominationService.remove(e.getEntity());clear(e.getEntity());removeSource(e.getEntity().getUUID());SENT.remove(e.getEntity().getUUID());}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){DominationService.remove(e.getEntity());clear(e.getEntity());removeSource(e.getEntity().getUUID());SENT.remove(e.getEntity().getUUID());}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){DominationService.clear();for(var s:STATES.values())if(s.target instanceof ServerPlayer p)playerEffects(p,null);STATES.clear();SENT.clear();GOALS.clear();ANGER_TARGETS.clear();ANGER_BRAINS.clear();}
}
