package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.filing.Filings;
import dev.cosmerearcanists.power.PowerService;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Server-owned held heat work. One physical input is consumed per completed recipe. */
public final class BrassHands {
    private record Work(InteractionHand hand,ItemStack input,ItemStack output,double progress,boolean food){}
    private record Candidate(ItemStack output,boolean food){}
    private static final Map<ServerPlayer,Work> WORK=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> FIRE=new WeakHashMap<>();
    public static void clear(ServerPlayer p){WORK.remove(p);FIRE.remove(p);}
    private static double heldLevel(ServerPlayer p){return PowerService.holdingAbility(p,"f:brass")?Math.max(0,BrassFeruchemy.levels(p)):0;}
    private static boolean reaches(double level,double threshold){return level>0&&level+1e-7>=threshold;}
    private static boolean ore(ItemStack stack){return stack.getTags().anyMatch(t->{var id=t.location();return id.getNamespace().equals("c")&&(id.getPath().equals("ores")||id.getPath().startsWith("ores/")||id.getPath().equals("raw_materials")||id.getPath().startsWith("raw_materials/"));});}
    private static Candidate candidate(ServerPlayer p,ItemStack input,double level){
        if(input.isEmpty()||Filings.protectedMaterial(input))return null;
        boolean food=FeruchemyConfig.BRASS_COOK.get()&&reaches(level,FeruchemyConfig.BRASS_COOK_LEVEL.get())&&input.has(DataComponents.FOOD);
        boolean mineral=FeruchemyConfig.BRASS_SMELT.get()&&reaches(level,FeruchemyConfig.BRASS_SMELT_LEVEL.get())&&ore(input);
        if(!food&&!mineral)return null;
        var recipe=p.serverLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(input.copyWithCount(1)),p.serverLevel());
        if(recipe.isEmpty())return null;var result=recipe.get().value().assemble(new SingleRecipeInput(input.copyWithCount(1)),p.registryAccess());
        if(result.isEmpty()||ItemStack.isSameItemSameComponents(input,result))return null;
        if(food&&!result.has(DataComponents.FOOD))return mineral?new Candidate(result,false):null;
        return new Candidate(result,food);
    }
    public static void tick(ServerPlayer p){
        double level=heldLevel(p);if(level<=0){WORK.remove(p);return;}
        for(var hand:InteractionHand.values()){
            if(p.isUsingItem()&&p.getUsedItemHand()==hand)continue;var input=p.getItemInHand(hand);var next=candidate(p,input,level);if(next==null)continue;
            var old=WORK.get(p);double progress=old!=null&&old.hand()==hand&&old.food()==next.food()&&ItemStack.isSameItemSameComponents(old.input(),input)&&ItemStack.matches(old.output(),next.output())?old.progress():0;
            double threshold=next.food()?FeruchemyConfig.BRASS_COOK_LEVEL.get():FeruchemyConfig.BRASS_SMELT_LEVEL.get();int ticks=next.food()?FeruchemyConfig.BRASS_COOK_TICKS.get():FeruchemyConfig.BRASS_SMELT_TICKS.get();progress+=Math.min(8,level/threshold);
            if(progress+1e-7<ticks){WORK.put(p,new Work(hand,input.copyWithCount(1),next.output().copy(),progress,next.food()));return;}
            // Re-evaluated the current held stack and live recipe this tick; no stale inventory references.
            input.shrink(1);var output=next.output().copy();if(input.isEmpty())p.setItemInHand(hand,output);else if(!p.getInventory().add(output)&&!output.isEmpty())p.drop(output,false);
            p.getInventory().setChanged();p.containerMenu.broadcastChanges();WORK.remove(p);
            p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.FIRE_EXTINGUISH,SoundSource.PLAYERS,.25f,1.6f);return;
        }WORK.remove(p);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void punch(PlayerInteractEvent.LeftClickBlock event){
        if(event.getAction()!=PlayerInteractEvent.LeftClickBlock.Action.START||!(event.getEntity() instanceof ServerPlayer p)||event.getFace()==null||event.getUseBlock()==net.neoforged.neoforge.common.util.TriState.FALSE)return;
        ignite(p,event.getPos(),event.getFace());
    }
    public static boolean ignite(ServerPlayer p,BlockPos block,Direction face){
        if(!FeruchemyConfig.BRASS_IGNITE.get()||!reaches(heldLevel(p),FeruchemyConfig.BRASS_IGNITE_LEVEL.get())||!p.mayBuild())return false;
        var level=p.serverLevel();var pos=block.relative(face);if(!level.hasChunkAt(block)||!level.hasChunkAt(pos)||!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        double range=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE)+1;
        if(p.getEyePosition().distanceToSqr(Vec3.atCenterOf(block))>range*range||!level.mayInteract(p,block)||!level.mayInteract(p,pos)||!p.mayUseItemAt(pos,face,p.getMainHandItem())||level.getBlockState(block).isAir()||!level.isEmptyBlock(pos)||!BaseFireBlock.canBePlacedAt(level,pos,face))return false;
        long now=level.getGameTime();var last=FIRE.get(p);if(last!=null&&now-last<FeruchemyConfig.BRASS_FIRE_COOLDOWN.get())return false;
        var snapshot=BlockSnapshot.create(level.dimension(),level,pos);if(!level.setBlock(pos,BaseFireBlock.getState(level,pos),3))return false;
        if(EventHooks.onBlockPlace(p,snapshot,face)){snapshot.restore();return false;}
        FIRE.put(p,now);level.playSound(null,pos,SoundEvents.FLINTANDSTEEL_USE,SoundSource.PLAYERS,1,1);level.gameEvent(p,GameEvent.BLOCK_PLACE,pos);return true;
    }
}
