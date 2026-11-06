package dev.cosmerearcanists.aondor;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.connection.WorldConnectionAccess;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import static dev.cosmerearcanists.aondor.AonProgram.Type;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonAccessGameTests {
    private static FakePlayer player(GameTestHelper h){for(int x=0;x<=4;x++)for(int y=20;y<=25;y++)for(int z=0;z<=12;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonAccess"));p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(2,20,2)).getCenter());p.getAttribute(PowerAttributes.AONDOR).setBaseValue(1);PowerData.assignSlot(p,AonDorService.POWER,0);return p;}
    private static ResourceLocation id(){return ResourceLocation.fromNamespaceAndPath("cosmerearcanists","test_"+UUID.randomUUID().toString().replace("-",""));}
    private static AonProgram.Node node(String key,Type type,AonProgram.Node... args){return new AonProgram.Node("cosmerearcanists:"+key,0,type,List.of(args));}
    private static AonProgram.Node number(double n){return new AonProgram.Node("cosmerearcanists:number",n,Type.NUMBER,List.of());}
    private static AonCastPlan light(FakePlayer p){return AonEvaluator.prepare(p,new AonProgram(node("light",Type.EFFECT),1),1);}
    private static boolean rejects(Runnable action){try{action.run();return false;}catch(IllegalArgumentException expected){return true;}}
    private static AutoCloseable connection(FakePlayer p,double[] value){return WorldConnectionAccess.register(id(),(actor,world,current)->actor==p?new WorldConnectionAccess.State(value[0],value[1]):current);}
    static final class Bank implements AonInvestiture.Source {
        final ServerPlayer owner;double balance;int reservations,commits,refunds,closed;boolean fail;Runnable onReserve=()->{},onCommit=()->{};
        Bank(ServerPlayer owner,double balance){this.owner=owner;this.balance=balance;}
        public AonInvestiture.Payment reserve(ServerPlayer p,AonCost.Quote quote){if(p!=owner)return null;reservations++;if(balance<quote.total())return null;onReserve.run();return new AonInvestiture.Payment(){boolean paid;
            public boolean commit(){commits++;if(fail||balance<quote.total())return false;balance-=quote.total();paid=true;onCommit.run();return true;}
            public void refund(){if(paid){balance+=quote.total();paid=false;refunds++;}}
            public void close(){closed++;}
        };}
    }
    @GameTest(template="empty") public static void ambientDorIsUnlimitedAndDoesNotTouchExternalFunds(GameTestHelper h)throws Exception{
        var p=player(h);var previous=AonDorConfig.DOR_DIMENSIONS.get();var bank=new Bank(p,0);try(var source=AonInvestiture.register(id(),bank)){
            AonDorConfig.DOR_DIMENSIONS.set(List.of(p.level().dimension().location().toString()));var plan=light(p);h.assertTrue(plan.quote().total()>0,"Sel spells still quote dynamic work");String result=AonInvestiture.execute(p,plan);h.assertTrue(result.contains("temporary light")&&bank.balance==0&&bank.reservations==0,"Ambient Dor pays unlimited work without a player reserve or external source");
        }finally{AonDorConfig.DOR_DIMENSIONS.set(previous);}h.succeed();
    }
    @GameTest(template="empty") public static void offworldRequiresBothThreefoldConnectionAndPaidInvestiture(GameTestHelper h)throws Exception{
        var p=player(h);h.assertTrue(AonDorConfig.ACCESS.get()&&!AonInvestiture.ambient(p),"Default test world is not Sel and access enforcement is enabled");double[] connection={1,2.999};var bank=new Bank(p,1000);var plan=light(p);
        try(var boost=connection(p,connection);var source=AonInvestiture.register(id(),bank)){
            h.assertTrue(rejects(()->AonInvestiture.execute(p,plan))&&bank.balance==1000&&bank.reservations==0,"Fuel cannot replace the threefold Connection gate");connection[1]=3;bank.balance=0;h.assertTrue(rejects(()->AonInvestiture.execute(p,plan)),"Connection cannot replace fuel");bank.balance=1000;String result=AonInvestiture.execute(p,plan);h.assertTrue(result.contains("temporary light")&&Math.abs(bank.balance-(1000-plan.quote().total()))<1e-8&&bank.commits==1,"Exactly3x and an atomic source pay exactly the quote before effects");
        }h.succeed();
    }
    @GameTest(template="empty") public static void failedPaymentAndChangedAccessCannotApplyEffects(GameTestHelper h)throws Exception{
        var p=player(h);double[] connection={2,6};var bank=new Bank(p,1000);var plan=light(p);var pos=BlockPos.containing(p.getEyePosition().add(p.getLookAngle().scale(.8)));
        try(var boost=connection(p,connection);var source=AonInvestiture.register(id(),bank)){
            bank.fail=true;h.assertTrue(rejects(()->AonInvestiture.execute(p,plan))&&h.getLevel().getBlockState(pos).isAir()&&bank.balance==1000,"Failed commit applies no light and spends nothing");bank.fail=false;bank.onReserve=()->connection[1]=5;h.assertTrue(rejects(()->AonInvestiture.execute(p,plan))&&bank.balance==1000&&h.getLevel().getBlockState(pos).isAir(),"Access is rechecked after reservation");
            connection[1]=6;bank.onReserve=()->{};bank.onCommit=()->connection[1]=5;h.assertTrue(rejects(()->AonInvestiture.execute(p,plan))&&bank.balance==1000&&bank.refunds==1&&h.getLevel().getBlockState(pos).isAir(),"Access loss during payment refunds the committed source before effects");
        }h.succeed();
    }
    @GameTest(template="empty") public static void paidDuraluminOutputContributesWithoutAWhitelistShortcut(GameTestHelper h){
        var p=player(h);h.assertTrue(rejects(()->AonInvestiture.requireAccess(p)),"Unboosted Elantrian cannot cast off-world");PowerAttributes.set(p,"f:duralumin",2);var mind=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cosmerearcanists","duralumin_ingot")));p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,mind);Metalminds.transferForStorage(p,mind,"duralumin",1000);PowerData.state(p,"f:duralumin",14);
        try{DuraluminFeruchemy.tick(p);var state=AonInvestiture.connection(p);h.assertTrue(state.effective()/state.baseline()>=3&&DuraluminFeruchemy.fundedLevels(p)>0,"Real funded tapping reaches the shared ratio");AonInvestiture.requireAccess(p);PowerData.state(p,"f:duralumin",0);DuraluminFeruchemy.clear(p);h.assertTrue(rejects(()->AonInvestiture.requireAccess(p)),"Removing funded output immediately revokes Connection eligibility");}finally{PowerData.state(p,"f:duralumin",0);DuraluminFeruchemy.clear(p);}h.succeed();
    }
    @GameTest(template="empty") public static void timedAonConnectionPersistsForItsPaidDurationAndCannotBootstrap(GameTestHelper h)throws Exception{
        var p=player(h);var previous=AonDorConfig.DOR_DIMENSIONS.get();var boostProgram=new AonProgram(node("connection",Type.EFFECT,number(3),number(.1)),1);
        h.assertTrue(rejects(()->AonInvestiture.execute(p,AonEvaluator.prepare(p,boostProgram,1))),"Locked-out AonDor cannot bootstrap its own Connection");
        try{AonDorConfig.DOR_DIMENSIONS.set(List.of(p.level().dimension().location().toString()));AonInvestiture.execute(p,AonEvaluator.prepare(p,boostProgram,1));}finally{AonDorConfig.DOR_DIMENSIONS.set(previous);}
        var bank=new Bank(p,1000);try(var source=AonInvestiture.register(id(),bank)){AonInvestiture.requireAccess(p);h.assertTrue(AonInvestiture.execute(p,light(p)).contains("temporary light"),"Existing timed boost plus external fuel permits off-world casting");}
        h.runAfterDelay(5,()->{h.assertTrue(rejects(()->AonInvestiture.requireAccess(p)),"Timed Connection expires without changing baseline or art ownership");h.assertTrue(p.getAttributeValue(PowerAttributes.AONDOR)==1,"Unavailable access never revokes AonDor");h.succeed();});
    }
    @GameTest(template="empty") public static void estimatesPreflightWithoutEffectsAndScaleWithWork(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.getX(),p.getEyeY()-.4,p.getZ()+3);h.getLevel().addFreshEntity(pig);
        var defaultForce=AonEvaluator.prepare(p,new AonProgram(node("force",Type.EFFECT),1),1);var skipped=AonEvaluator.prepare(p,new AonProgram(node("force",Type.EFFECT,node("skip",Type.SKIP),node("skip",Type.SKIP)),1),1);
        h.assertTrue(defaultForce.quote().total()>0&&defaultForce.quote().total()==skipped.quote().total()&&pig.getDeltaMovement().lengthSqr()==0,"Skip is not free and cost preparation does not push targets");
        var vector=node("vector",Type.VECTOR,number(0),number(0),number(2));var strong=AonEvaluator.prepare(p,new AonProgram(node("force",Type.EFFECT,node("skip",Type.SKIP),vector),1),1);h.assertTrue(strong.quote().total()>defaultForce.quote().total(),"Magnitude changes actual work cost");
        var food=new ItemEntity(h.getLevel(),p.getX()+1,p.getY(),p.getZ(),new ItemStack(Items.BEEF,4));h.getLevel().addFreshEntity(food);AonMemory.remember(p,1,new AonEvaluator.Value(Type.ENTITY,food));var recall=node("recall",Type.VALUE,number(1));var cooking=AonEvaluator.prepare(p,new AonProgram(node("heat",Type.EFFECT,recall,number(4)),1),1);h.assertTrue(food.getItem().is(Items.BEEF)&&cooking.quote().lines().stream().anyMatch(l->l.kind().equals("cooking")&&l.units()==16),"Every processed input item is priced before any recipe transforms");pig.discard();food.discard();h.succeed();
    }
    @GameTest(template="empty") public static void comparisonsAndSavedRawSpellLayoutsRoundTrip(GameTestHelper h){
        var p=player(h);h.assertTrue(AonEvaluator.value(p,node("greater_than",Type.BOOLEAN,number(5),number(2))).equals(true)&&AonEvaluator.value(p,node("less_than",Type.BOOLEAN,number(5),number(2))).equals(false),"Numeric comparisons produce Booleans");h.assertTrue(AonEvaluator.value(p,node("equal",Type.BOOLEAN,number(-0.0),number(0))).equals(true),"Equality retains typed numeric zero semantics");
        var template=AonDorService.definitions(p).stream().filter(t->t.id().equals("cosmerearcanists:light")).findFirst().orElseThrow();var drawing=new AonDrawing(List.of(new AonDrawing.Glyph(true,.5,.5,template.strokes())));AonSavedSpells.checked(p,drawing);AonSavedSpells.save(p,"test_light");h.assertTrue(AonSavedSpells.load(p,"test_light").equals(drawing),"Named layouts persist their raw geometry");var clone=player(h);PowerData.clonePlayer(p,clone);h.assertTrue(AonSavedSpells.load(clone,"test_light").equals(drawing),"Saved layouts survive normal player data cloning");h.assertTrue(rejects(()->AonSavedSpells.load(p,"../bad")),"Invalid saved names cannot address arbitrary data");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{var packet=new AonDorNetwork.Draft(new AonDorNetwork.Check(UUID.randomUUID(),p.level().dimension().location().toString(),0,drawing));AonDorNetwork.Draft.CODEC.encode(buffer,packet);h.assertTrue(AonProgram.compile(AonDorNetwork.Draft.CODEC.decode(buffer).drawing().drawing(),AonDorService.definitions(p),AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get(),AonDorConfig.FALLBACK_POTENCY.get()).root().id().equals("cosmerearcanists:light"),"Bounded float draft restore preserves the recognized spell");}finally{buffer.release();}h.succeed();
    }
    @GameTest(template="empty") public static void noOpCastsRefundAndPostPaymentRecipeChangesCannotDuplicateItems(GameTestHelper h)throws Exception{
        var p=player(h);double[] state={1,3};var bank=new Bank(p,10000);
        try(var connection=connection(p,state);var source=AonInvestiture.register(id(),bank)){
            var none=new AonCastPlan(new AonCost.Builder(1).quote(),AonCastPlan.none("No effect"));AonInvestiture.execute(p,none);h.assertTrue(Math.abs(bank.balance-10000)<1e-8&&bank.refunds==1,"No-effect receipt is refunded exactly once");
            var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.position().add(1,0,0));pig.setTicksFrozen(200);var item=new ItemEntity(h.getLevel(),p.getX()+2,p.getY(),p.getZ(),new ItemStack(Items.BEEF,4));h.getLevel().addFreshEntity(pig);h.getLevel().addFreshEntity(item);AonMemory.remember(p,1,new AonEvaluator.Value(Type.ENTITIES,List.of(pig,item)));
            var program=new AonProgram(node("heat",Type.EFFECT,node("recall",Type.VALUE,number(1)),number(4)),1);var plan=AonEvaluator.prepare(p,program,1);bank.onCommit=()->item.getItem().shrink(1);
            h.assertTrue(rejects(()->AonInvestiture.execute(p,plan))&&pig.getTicksFrozen()==200&&!pig.isOnFire()&&item.getItem().is(Items.BEEF)&&item.getItem().getCount()==3&&Math.abs(bank.balance-10000)<1e-8,"Changed recipe input refunds Dor before any target changes and never resurrects consumed units");pig.discard();item.discard();
        }h.succeed();
    }

}
