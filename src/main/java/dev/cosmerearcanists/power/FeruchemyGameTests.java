package dev.cosmerearcanists.power;
import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.filing.Filings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FeruchemyGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"IronFerring"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:iron",1);return p;}
    private static ItemStack jewelry(String metal,String slot){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_metalmind_"+slot)));}
    @GameTest(template="empty") public static void everyMetalDenominationAndJewelryHasProportionalCapacity(GameTestHelper h){
        for(var metal:Metal.values())for(String form:List.of("nugget","ingot","block")){
            String namespace=metal==Metal.IRON||metal==Metal.GOLD||metal==Metal.COPPER&&!form.equals("nugget")?"minecraft":CosmereArcanists.MOD_ID;
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace,metal.id()+"_"+form));
            if(item==Items.AIR)continue;var s=new ItemStack(item);var source=Metalminds.source(s);
            int units=form.equals("nugget")?1:form.equals("ingot")?9:81;
            h.assertTrue(source!=null&&source.metal().equals(metal.id())&&Math.abs(Metalminds.capacity(s)-units*600)<1e-7,"Registered metal forms support capacity proportional to metal amount");
        }
        for(var metal:Metal.values())for(String slot:List.of("ring","bracelet","necklace")){
            var s=jewelry(metal.id(),slot);h.assertTrue(!s.isEmpty()&&s.getMaxStackSize()==1&&Metalminds.capacity(s)==(slot.equals("ring")?600:5400),"All48 dedicated pieces are singletons with material-based capacity");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void stacksCannotStoreTapOrSplitAndBoundPiecesNeverStack(GameTestHelper h){
        var p=player(h);for(var item:List.of(Items.IRON_NUGGET,Items.IRON_INGOT,Items.IRON_BLOCK,jewelry("iron","ring").getItem())){
            var s=new ItemStack(item,2);p.setItemInHand(InteractionHand.OFF_HAND,s);PowerData.state(p,"f:iron",-5);IronFeruchemy.tick(p);
            h.assertTrue(s.getCount()==2&&!Metalminds.bound(s)&&Metalminds.charge(s)==0&&IronFeruchemy.massMultiplier(p)==1,"Storing into any stack is rejected without splitting or gaining an effect");
            var single=new ItemStack(item);h.assertTrue(Metalminds.transfer(single,p.getUUID(),"iron",1)==1&&single.getMaxStackSize()==1,"First charge makes a denomination permanently non-stackable");
            single.setCount(2);h.assertTrue(Metalminds.transfer(single,p.getUUID(),"iron",1)==0&&Metalminds.transfer(single,p.getUUID(),"iron",-1)==0&&Metalminds.charge(single)==1,"Even externally forged charged stacks cannot store or tap");
        }h.succeed();
    }
    @GameTest(template="empty") public static void ownershipAndPersistentItemComponentsSurviveSerialization(GameTestHelper h){
        var p=player(h);var s=new ItemStack(Items.IRON_INGOT);Metalminds.transfer(s,p.getUUID(),"iron",12.25);
        h.assertTrue(Metalminds.transfer(s,UUID.randomUUID(),"iron",1)==0&&Metalminds.transfer(s,UUID.randomUUID(),"iron",-1)==0,"Ordinary metalminds remain keyed to the storing player's identity");
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{ItemStack.STREAM_CODEC.encode(b,s);var copy=ItemStack.STREAM_CODEC.decode(b);h.assertTrue(Metalminds.charge(copy)==12.25&&Metalminds.capacity(copy)==5400&&Metalminds.owner(copy).equals(p.getUUID())&&copy.getMaxStackSize()==1,"Charge, capacity, identity and singleton components persist");}finally{b.release();}
        boolean ownership=FeruchemyConfig.OWNERSHIP.get();try{FeruchemyConfig.OWNERSHIP.set(false);h.assertTrue(Metalminds.transfer(s,UUID.randomUUID(),"iron",-.25)==-.25,"Ownership is independently configurable");}finally{FeruchemyConfig.OWNERSHIP.set(ownership);}h.succeed();
    }
    @GameTest(template="empty") public static void boundMetalmindsCannotBeCraftedFiledOrPlaced(GameTestHelper h){
        var p=player(h);var s=new ItemStack(Items.IRON_BLOCK);Metalminds.transfer(s,p.getUUID(),"iron",10);p.setItemInHand(InteractionHand.OFF_HAND,s);
        h.assertTrue(!net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_BLOCK).test(s)&&!Filings.file(p)&&Metalminds.charge(s)==10,"Crafting and filing cannot recycle a bound metalmind");
        var input=net.minecraft.world.item.crafting.CraftingInput.of(1,1,List.of(s));var recipe=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel());h.assertTrue(recipe.isEmpty(),"Recipe manager blocks block-to-ingot conversion of bound material");
        var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,InteractionHand.OFF_HAND,p.blockPosition(),new net.minecraft.world.phys.BlockHitResult(p.position(),net.minecraft.core.Direction.UP,p.blockPosition(),false));new MetalmindEvents().place(event);h.assertTrue(event.getUseItem()==net.neoforged.neoforge.common.util.TriState.FALSE,"Placement cannot strip stored charge or identity");
        Metalminds.transfer(s,p.getUUID(),"iron",-10);h.assertTrue(Metalminds.bound(s)&&s.getMaxStackSize()==1&&!net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_BLOCK).test(s),"Empty bound pieces retain their identity and singleton restriction");h.succeed();
    }
    @GameTest(template="empty") public static void ironStoresLostMassAndTapsOnlyPreviouslyStoredCharge(GameTestHelper h){
        var p=player(h);var item=new ItemStack(Items.IRON_NUGGET);p.setItemInHand(InteractionHand.OFF_HAND,item);PowerData.state(p,"f:iron",-5);
        for(int i=0;i<20;i++)IronFeruchemy.tick(p);h.assertTrue(Math.abs(Metalminds.charge(item)-.5)<1e-7&&Math.abs(IronFeruchemy.massMultiplier(p)-.5)<1e-7,"One second at half normal mass stores half a weight-second");
        h.assertTrue(SeekingService.signalsFor(p).stream().anyMatch(s->s.art().equals(dev.cosmerearcanists.investiture.InvestitureSignals.FERUCHEMY)),"Active storing emits a real Feruchemy signal");
        PowerData.state(p,"f:iron",1);IronFeruchemy.tick(p);h.assertTrue(Math.abs(IronFeruchemy.massMultiplier(p)-1.5)<1e-7,"Tapping level1 adds half normal mass");for(int i=0;i<20;i++)IronFeruchemy.tick(p);
        h.assertTrue(Metalminds.charge(item)<1e-7&&PowerData.state(p,"f:iron")==0&&IronFeruchemy.massMultiplier(p)==1,"Depleted charge stops tapping and restores mass");h.succeed();
    }
    @GameTest(template="empty") public static void capacityAndPartialTapsNeverCreateFreeWeight(GameTestHelper h){
        var p=player(h);var item=new ItemStack(Items.IRON_NUGGET);p.setItemInHand(InteractionHand.OFF_HAND,item);Metalminds.transfer(item,p.getUUID(),"iron",599.995);PowerData.state(p,"f:iron",-5);IronFeruchemy.tick(p);
        h.assertTrue(Math.abs(Metalminds.charge(item)-600)<1e-7&&PowerData.state(p,"f:iron")==0&&Math.abs(IronFeruchemy.massMultiplier(p)-.9)<1e-6,"A nearly full metalmind stores only available capacity and scales the penalty to it");
        Metalminds.transfer(item,p.getUUID(),"iron",-599.999);PowerData.state(p,"f:iron",5);IronFeruchemy.tick(p);h.assertTrue(Metalminds.charge(item)<1e-8&&IronFeruchemy.massMultiplier(p)>1&&IronFeruchemy.massMultiplier(p)<1.1,"A nearly empty metalmind gives only the fractional mass it can pay for");IronFeruchemy.tick(p);h.assertTrue(IronFeruchemy.massMultiplier(p)==1,"Partial final effects do not persist for free");h.succeed();
    }
    @GameTest(template="empty") public static void effectiveMassChangesAllomanticAccelerationKnockbackAndLightweightDrag(GameTestHelper h){
        var p=player(h);var target=MetalTarget.block(BlockPos.containing(PowerService.chest(p).add(2,0,0)));var direction=new Vec3(-1,0,0);
        var normal=PowerService.targetImpulse(p,target,direction,14);IronFeruchemy.mass(p,2);var heavy=PowerService.targetImpulse(p,target,direction,14);h.assertTrue(Math.abs(normal.length()-2*heavy.length())<1e-7,"Double mass halves acceleration against a fixed Allomantic anchor");
        p.setDeltaMovement(Vec3.ZERO);p.knockback(1,1,0);double heavyKnockback=p.getDeltaMovement().horizontalDistance();IronFeruchemy.mass(p,1);p.setDeltaMovement(Vec3.ZERO);p.knockback(1,1,0);h.assertTrue(Math.abs(p.getDeltaMovement().horizontalDistance()-2*heavyKnockback)<1e-7,"Heavy bodies resist vanilla knockback");
        double gravity=p.getGravity();IronFeruchemy.mass(p,.5);p.setDeltaMovement(0,-4,0);p.travel(Vec3.ZERO);h.assertTrue(p.getDeltaMovement().y>=-FeruchemyConfig.TERMINAL.get()*.5-1e-7&&p.getGravity()==gravity,"Lightweight air resistance caps falling velocity without changing gravity");
        var fall=new net.neoforged.neoforge.event.entity.living.LivingFallEvent(p,20,1);new IronFeruchemy().fall(fall);h.assertTrue(Math.abs(fall.getDamageMultiplier()-.5)<1e-7,"Lightweight impact mitigation scales with actual mass");IronFeruchemy.mass(p,1);h.succeed();
    }
    @GameTest(template="empty") public static void featureGatesAndMissingContactPreserveItemCharge(GameTestHelper h){
        var p=player(h);var item=new ItemStack(Items.IRON_INGOT);Metalminds.transfer(item,p.getUUID(),"iron",10);p.setItemInHand(InteractionHand.OFF_HAND,item);PowerData.state(p,"f:iron",1);IronFeruchemy.tick(p);double saved=Metalminds.charge(item);boolean enabled=FeruchemyConfig.ENABLED.get();
        try{FeruchemyConfig.ENABLED.set(false);IronFeruchemy.tick(p);h.assertTrue(IronFeruchemy.massMultiplier(p)==1&&Metalminds.charge(item)==saved&&PowerAttributes.strength(p,"f:iron")==1,"Disabling removes transient effects and preserves charge and power attributes");}finally{FeruchemyConfig.ENABLED.set(enabled);}
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);IronFeruchemy.tick(p);h.assertTrue(PowerData.state(p,"f:iron")==0&&Metalminds.charge(item)==saved,"Losing contact stops the power without destroying the item-owned reserve");h.succeed();
    }
    @GameTest(template="empty") public static void curiosSlotsAndWornMetalmindsWorkWhenInstalled(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("curios"))dev.cosmerearcanists.feruchemy.CuriosMetalmindTests.verify(h);h.succeed();
    }
    private FeruchemyGameTests(){}
}
