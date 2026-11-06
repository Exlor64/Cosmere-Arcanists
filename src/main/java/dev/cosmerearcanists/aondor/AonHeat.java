package dev.cosmerearcanists.aondor;

import java.util.*;
import java.util.function.Supplier;
import dev.cosmerearcanists.filing.Filings;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.BaseFireBlock;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/** Server-owned heat work. Resolve every recipe/stack limit before changing any target. */
public final class AonHeat {
    private record Work(String description,int units,Runnable validate,Runnable apply){}
    private static boolean reaches(double power,double threshold){return AonHeatPower.reaches(power,threshold);}
    private static boolean hostileAllowed(ServerPlayer p,Entity target){return target==p||!p.isAlliedTo(target)&&(!(target instanceof ServerPlayer other)||p.canHarmPlayer(other));}
    private static Work prepare(ServerPlayer p,Entity target,double strength,double power){
        if(target instanceof ItemEntity item){
            var input=item.getItem();if(input.isEmpty()||Filings.protectedMaterial(input))return null;
            var recipe=p.serverLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(input.copyWithCount(1)),p.serverLevel());if(recipe.isEmpty())return null;
            var result=recipe.get().value().assemble(new SingleRecipeInput(input.copyWithCount(1)),p.registryAccess());if(result.isEmpty()||ItemStack.isSameItemSameComponents(input,result))return null;
            boolean food=result.has(DataComponents.FOOD);
            if(food?(!AonDorConfig.HEAT_COOK.get()||!reaches(power,AonDorConfig.HEAT_COOK_POWER.get())):(!AonDorConfig.HEAT_SMELT.get()||!reaches(power,AonDorConfig.HEAT_SMELT_POWER.get())))return null;
            long count=(long)input.getCount()*result.getCount();if(input.getCount()>AonDorConfig.HEAT_ITEMS.get()||count>result.getMaxStackSize())throw new IllegalArgumentException("Heat item stack is too large; split it before casting");
            AonCost.add(food?"cooking":"smelting",input.getCount()*power);
            var output=result.copyWithCount((int)count);var original=input.copy();return new Work(food?"cooked":"smelted",input.getCount(),()->{if(!ItemStack.matches(item.getItem(),original))throw new IllegalArgumentException("Heat input changed during payment");},()->{item.setItem(output);item.clearFire();});
        }
        if(!(target instanceof LivingEntity living))return null;
        boolean ignite=AonDorConfig.HEAT_IGNITE.get()&&reaches(power,AonDorConfig.HEAT_IGNITE_POWER.get())&&hostileAllowed(p,target)&&!target.fireImmune()&&!living.hasEffect(MobEffects.FIRE_RESISTANCE);
        double damage=hostileAllowed(p,target)&&AonDorConfig.HEAT_DAMAGE.get()?AonHeatPower.damage(power,AonDorConfig.HEAT_DAMAGE_POWER.get(),AonDorConfig.HEAT_DAMAGE_RATE.get(),AonDorConfig.HEAT_DAMAGE_CAP.get()):0;
        if(living.getTicksFrozen()<=0&&!ignite&&damage<=0)return null;
        AonCost.add("heat",Math.max(0,Math.min(living.getTicksFrozen(),AonDorConfig.THAW.get()*strength))/140.0+damage+ (ignite?power+Math.min(30,2*power)*.1:0));
        return new Work("warmed",1,()->{},()->{AonDorEffects.warm(living,strength);if(damage>0)living.hurt(new DamageSource(p.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.IN_FIRE),p),(float)damage);if(ignite&&living.isAlive())living.igniteForSeconds((float)Math.min(30,2*power));});
    }
    public static String entities(ServerPlayer p,List<Entity> targets,double strength,double power){return planEntities(p,targets,strength,power).get().message();}
    static Supplier<AonCastPlan.Outcome> planEntities(ServerPlayer p,List<Entity> targets,double strength,double power){
        var work=new ArrayList<Work>();for(var target:new LinkedHashSet<>(targets)){AonEvaluator.checkPosition(p,target.position());if(!target.isAlive()||target.isSpectator()||target.level()!=p.level())throw new IllegalArgumentException("A selected entity is no longer available");var plan=prepare(p,target,strength,power);if(plan!=null)work.add(plan);}
        if(work.isEmpty())return AonCastPlan.none("Heat: this entity has no basic warming interaction at this power.");
        long warmed=work.stream().filter(w->w.description().equals("warmed")).count();int cooked=work.stream().filter(w->w.description().equals("cooked")).mapToInt(Work::units).sum(),smelted=work.stream().filter(w->w.description().equals("smelted")).mapToInt(Work::units).sum();
        return ()->{for(var target:targets){AonEvaluator.checkPosition(p,target.position());if(!target.isAlive()||target.isSpectator()||target.level()!=p.level())throw new IllegalArgumentException("Heat target changed during payment");}for(var plan:work)plan.validate().run();for(var plan:work)plan.apply().run();return new AonCastPlan.Outcome("Heat: warmed "+warmed+" target(s), cooked "+cooked+", smelted "+smelted+" item(s).",true);};
    }
    public static String block(ServerPlayer p,BlockPos block,double strength,double power){return planBlock(p,block,strength,power).get().message();}
    static Supplier<AonCastPlan.Outcome> planBlock(ServerPlayer p,BlockPos block,double strength,double power){
        AonEvaluator.checkPosition(p,block.getCenter());var level=p.serverLevel();if(!p.mayBuild()||!level.mayInteract(p,block))return AonCastPlan.none("Heat: building is not allowed here.");
        var state=level.getBlockState(block);boolean warmable=state.is(net.minecraft.world.level.block.Blocks.SNOW)||state.is(net.minecraft.world.level.block.Blocks.ICE)||state.is(net.minecraft.world.level.block.Blocks.FROSTED_ICE)||state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock&&!state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)&&!state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
        if(warmable){AonCost.add("heat_block",1+power);return ()->{boolean changed=AonDorEffects.warmBlock(p,block);return new AonCastPlan.Outcome(changed?"Heat: block warmed.":"Heat: block could not be warmed.",changed);};}
        if(AonDorConfig.HEAT_IGNITE.get()&&reaches(power,AonDorConfig.HEAT_IGNITE_POWER.get()))for(var face:List.of(Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST,Direction.DOWN)){
            var pos=block.relative(face);if(!level.hasChunkAt(pos)||!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(p,pos)||!p.mayUseItemAt(pos,face,p.getMainHandItem()))continue;
            if(pos.getCenter().distanceToSqr(p.getEyePosition())>AonDorConfig.RANGE.get()*AonDorConfig.RANGE.get()||!level.isEmptyBlock(pos)||!BaseFireBlock.canBePlacedAt(level,pos,face))continue;
            AonCost.add("ignition",power);
            return ()->{var snapshot=BlockSnapshot.create(level.dimension(),level,pos);if(!level.isEmptyBlock(pos)||!level.setBlock(pos,BaseFireBlock.getState(level,pos),3))return new AonCastPlan.Outcome("Heat: no clear space for fire.",false);
                if(EventHooks.onBlockPlace(p,snapshot,face)){snapshot.restore();return new AonCastPlan.Outcome("Heat: ignition was denied.",false);}return new AonCastPlan.Outcome("Heat: block ignited.",true);};
        }
        return AonCastPlan.none("Heat: this block cannot be warmed or ignited here.");
    }
    private AonHeat(){}
}
