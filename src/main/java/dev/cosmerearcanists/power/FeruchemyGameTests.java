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
        for(int i=0;i<20;i++)IronFeruchemy.tick(p);h.assertTrue(Math.abs(Metalminds.charge(item)-.3)<1e-7&&Math.abs(IronFeruchemy.massMultiplier(p)-.7)<1e-7,"One second at70percent normal mass stores0.3weight-seconds");
        h.assertTrue(SeekingService.signalsFor(p).stream().anyMatch(s->s.art().equals(dev.cosmerearcanists.investiture.InvestitureSignals.FERUCHEMY)),"Active storing emits a real Feruchemy signal");
        PowerData.state(p,"f:iron",1);IronFeruchemy.tick(p);h.assertTrue(Math.abs(IronFeruchemy.massMultiplier(p)-1.15)<1e-7,"Tapping level1 adds15percent of normal mass");for(int i=0;i<40;i++)IronFeruchemy.tick(p);
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
        var mind=new ItemStack(Items.IRON_INGOT);Metalminds.transfer(mind,p.getUUID(),"iron",10);p.setItemInHand(InteractionHand.OFF_HAND,mind);PowerData.state(p,"f:iron",10);IronFeruchemy.tick(p);p.setDeltaMovement(Vec3.ZERO);p.knockback(1,1,0);double heavyKnockback=p.getDeltaMovement().horizontalDistance();IronFeruchemy.mass(p,1);p.setDeltaMovement(Vec3.ZERO);p.knockback(1,1,0);h.assertTrue(Math.abs(p.getDeltaMovement().horizontalDistance()*.8-heavyKnockback)<1e-7,"Tap10 reduces vanilla knockback by20percent");
        double gravity=p.getGravity();IronFeruchemy.mass(p,.5);p.setDeltaMovement(0,-4,0);p.travel(Vec3.ZERO);h.assertTrue(p.getDeltaMovement().y>=-FeruchemyConfig.TERMINAL.get()*.5-1e-7&&p.getGravity()==gravity,"Lightweight air resistance caps falling velocity without changing gravity");
        var fall=new net.neoforged.neoforge.event.entity.living.LivingFallEvent(p,20,1);new IronFeruchemy().fall(fall);h.assertTrue(Math.abs(fall.getDamageMultiplier()-.5)<1e-7,"Lightweight impact mitigation scales with actual mass");IronFeruchemy.mass(p,1);h.succeed();
    }
    @GameTest(template="empty") public static void featureGatesAndMissingContactPreserveItemCharge(GameTestHelper h){
        var p=player(h);var item=new ItemStack(Items.IRON_INGOT);Metalminds.transfer(item,p.getUUID(),"iron",10);p.setItemInHand(InteractionHand.OFF_HAND,item);PowerData.state(p,"f:iron",1);IronFeruchemy.tick(p);double saved=Metalminds.charge(item);boolean enabled=FeruchemyConfig.ENABLED.get();
        try{FeruchemyConfig.ENABLED.set(false);IronFeruchemy.tick(p);h.assertTrue(IronFeruchemy.massMultiplier(p)==1&&Metalminds.charge(item)==saved&&PowerAttributes.strength(p,"f:iron")==1,"Disabling removes transient effects and preserves charge and power attributes");}finally{FeruchemyConfig.ENABLED.set(enabled);}
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);IronFeruchemy.tick(p);h.assertTrue(PowerData.state(p,"f:iron")==0&&Metalminds.charge(item)==saved,"Losing contact stops the power without destroying the item-owned reserve");h.succeed();
    }
    @GameTest(template="empty") public static void ironFractionalAttributesBoundStatesAndSyncAsymmetricSelectorLimits(GameTestHelper h){
        var p=player(h);var item=new ItemStack(Items.IRON_INGOT);p.setItemInHand(InteractionHand.OFF_HAND,item);
        PowerAttributes.set(p,"f:iron",1.5);h.assertTrue(PowerData.storeLevels(p,"f:iron")==5&&PowerData.tapLevels(p,"f:iron")==15,"Attribute1.5 grants five storing steps with partial45percent maximum and15tap levels");
        PowerData.state(p,"f:iron",-100);IronFeruchemy.tick(p);h.assertTrue(PowerData.state(p,"f:iron")==-5&&Math.abs(IronFeruchemy.massMultiplier(p)-.55)<1e-7,"Server bounds storing to45percent at attribute1.5");
        Metalminds.transfer(item,p.getUUID(),"iron",100);PowerData.state(p,"f:iron",100);IronFeruchemy.tick(p);h.assertTrue(PowerData.state(p,"f:iron")==15&&Math.abs(IronFeruchemy.massMultiplier(p)-3.25)<1e-7,"Tap15 is225percent additional mass, without double-multiplying strength");
        PowerAttributes.set(p,"f:iron",1);h.assertTrue(PowerData.state(p,"f:iron")==10,"Reducing the attribute clamps an existing state immediately");PowerAttributes.set(p,"f:iron",1.5);h.assertTrue(PowerData.state(p,"f:iron")==10,"An out-of-range raw state does not silently reactivate when strength returns");
        var entry=new PowerNetwork.Entry("f:iron",15,Metalminds.charge(item),PowerData.storeLevels(p,"f:iron"),PowerData.tapLevels(p,"f:iron"));var packet=new PowerNetwork.Snapshot(p.level().dimension().location().toString(),"f:iron","",5,List.of(entry),List.of());
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{PowerNetwork.Snapshot.CODEC.encode(b,packet);h.assertTrue(PowerNetwork.Snapshot.CODEC.decode(b).equals(packet),"Per-power asymmetric selector limits round-trip to the client");}finally{b.release();}
        PowerAttributes.set(p,"f:iron",4);PowerData.state(p,"f:iron",-100);IronFeruchemy.tick(p);h.assertTrue(Math.abs(IronFeruchemy.massMultiplier(p)-.05)<1e-7&&PowerData.state(p,"f:iron")==-10,"High-strength final storage level remains capped at95percent");
        PowerData.state(p,"f:iron",40);IronFeruchemy.tick(p);h.assertTrue(Math.abs(IronFeruchemy.massMultiplier(p)-7)<1e-7,"Attribute4 unlocks40tap levels at fixed15percent per level");IronFeruchemy.mass(p,1);h.succeed();
    }
    @GameTest(template="empty") public static void inventoryStorageAndTappingAreOptInAndNeverDoubleCountHeldItems(GameTestHelper h){
        var p=player(h);var single=new ItemStack(Items.IRON_NUGGET);var stack=new ItemStack(Items.IRON_INGOT,2);p.getInventory().items.set(2,single);p.getInventory().items.set(3,stack);boolean previous=FeruchemyConfig.INVENTORY.get();
        try{
            FeruchemyConfig.INVENTORY.set(false);PowerData.state(p,"f:iron",-5);IronFeruchemy.tick(p);h.assertTrue(!Metalminds.bound(single)&&IronFeruchemy.massMultiplier(p)==1,"Inventory-only items do not store by default");
            FeruchemyConfig.INVENTORY.set(true);PowerData.state(p,"f:iron",-5);IronFeruchemy.tick(p);h.assertTrue(Math.abs(Metalminds.charge(single)-.015)<1e-7&&stack.getCount()==2&&!Metalminds.bound(stack),"Enabled inventory storage finds singletons without charging or splitting stacked denominations");
            p.getInventory().selected=2;h.assertTrue(Math.abs(Metalminds.accessibleCharge(p,"iron")-.015)<1e-7,"Selected hotbar item is counted only once in held plus inventory access");IronFeruchemy.tick(p);h.assertTrue(Math.abs(Metalminds.charge(single)-.03)<1e-7,"One transfer budget applies even when the same item is reachable through two paths");
            p.getInventory().selected=0;PowerData.state(p,"f:iron",1);IronFeruchemy.tick(p);h.assertTrue(Math.abs(Metalminds.charge(single)-.0225)<1e-7&&Math.abs(IronFeruchemy.massMultiplier(p)-1.15)<1e-7,"Enabled inventory access also taps without holding the piece");
            FeruchemyConfig.INVENTORY.set(false);IronFeruchemy.tick(p);h.assertTrue(PowerData.state(p,"f:iron")==0&&Metalminds.charge(single)>.02249&&IronFeruchemy.massMultiplier(p)==1,"Turning inventory access off stops effects and preserves stored charge");
        }finally{FeruchemyConfig.INVENTORY.set(previous);IronFeruchemy.mass(p,1);}h.succeed();
    }
    @GameTest(template="empty") public static void configurableCuriosCountsUpdateSlotsAndPreserveDisplacedCharge(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("curios"))dev.cosmerearcanists.feruchemy.CuriosMetalmindTests.verifyConfigurableSlots(h);h.succeed();
    }
    @GameTest(template="empty") public static void curiosSlotsAndWornMetalmindsWorkWhenInstalled(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("curios"))dev.cosmerearcanists.feruchemy.CuriosMetalmindTests.verify(h);h.succeed();
    }
    @GameTest(template="empty") public static void ironCombatOnlyRewardsFundedTappingAndFallingCriticals(GameTestHelper h){
        var p=player(h);var mind=new ItemStack(Items.IRON_INGOT);p.setItemInHand(InteractionHand.OFF_HAND,mind);Metalminds.transfer(mind,p.getUUID(),"iron",20);PowerData.state(p,"f:iron",10);IronFeruchemy.tick(p);
        var critical=new net.neoforged.neoforge.event.entity.player.CriticalHitEvent(p,p,1.5f,true);new IronFeruchemy().critical(critical);h.assertTrue(Math.abs(critical.getDamageMultiplier()-2.25)<1e-6,"Tap10 adds50percent to falling critical damage");
        var normal=new net.neoforged.neoforge.event.entity.player.CriticalHitEvent(p,p,1,false);normal.setCriticalHit(true);new IronFeruchemy().critical(normal);h.assertTrue(normal.getDamageMultiplier()==1,"Grounded or mod-forced criticals gain no bonus");
        PowerData.state(p,"f:iron",0);h.assertTrue(IronFeruchemy.combatLevels(p)==0&&IronFeruchemy.knockbackMultiplier(p)==1,"Stopping immediately removes combat bonuses");
        PowerData.state(p,"f:iron",-3);IronFeruchemy.tick(p);h.assertTrue(IronFeruchemy.combatLevels(p)==0,"Storing never grants critical damage");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);PowerData.state(p,"f:iron",10);IronFeruchemy.tick(p);h.assertTrue(IronFeruchemy.combatLevels(p)==0,"Missing contact cannot grant free combat power");h.succeed();
    }
    @GameTest(template="empty") public static void maximumStoringStrengthScalesMetalmindFillRate(GameTestHelper h){
        double[] attributes={1,2,4};double[] expected={.30,.60,.95};
        for(int j=0;j<attributes.length;j++){var p=player(h);PowerAttributes.set(p,"f:iron",attributes[j]);var mind=new ItemStack(Items.IRON_INGOT);p.setItemInHand(InteractionHand.OFF_HAND,mind);PowerData.state(p,"f:iron",-100);for(int i=0;i<20;i++)IronFeruchemy.tick(p);h.assertTrue(Math.abs(Metalminds.charge(mind)-expected[j])<1e-7,"Maximum storing fills at the actual stored fraction per second, capped95percent");}h.succeed();
    }
    private FeruchemyGameTests(){}
}
