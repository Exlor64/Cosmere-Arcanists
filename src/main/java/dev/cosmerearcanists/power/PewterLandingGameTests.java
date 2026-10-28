package dev.cosmerearcanists.power;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PewterLandingGameTests {
    private static DamageableTestPlayer player(GameTestHelper h){
        var p=new DamageableTestPlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"PewterLeaper"));p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(2,20,2)).getCenter());PowerAttributes.set(p,"f:pewter",2);PowerData.assignSlot(p,"f:pewter",0);PowerData.state(p,"f:pewter",10);
        var mind=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cosmerearcanists:pewter_metalmind_ring")));Metalminds.transfer(mind,p.getUUID(),"pewter",100);p.setItemInHand(InteractionHand.OFF_HAND,mind);return p;
    }
    private static void launch(DamageableTestPlayer p,boolean directional){PowerService.control(p,new PowerNetwork.Control(2,"",1|(directional?4:0)));PewterFeruchemy.tick(p);p.setOnGround(true);p.jumpFromGround();p.setOnGround(false);PewterFeruchemy.tick(p);}
    private static void clean(DamageableTestPlayer p){new PewterFeruchemy().logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));}
    @GameTest(template="empty") public static void activatedLeapProtectsOneLandingEvenAfterTappingStops(GameTestHelper h){var p=player(h);try{launch(p,true);PowerService.control(p,new PowerNetwork.Control(2,"",0));PowerData.state(p,"f:pewter",0);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);PewterFeruchemy.tick(p);p.causeFallDamage(20,1,p.damageSources().fall());h.assertTrue(p.getHealth()==20,"Already funded leap lands safely after release/depletion/contact loss");p.causeFallDamage(7,1,p.damageSources().fall());h.assertTrue(Math.abs(p.getHealth()-16)<.001,"Protection is consumed; next ordinary fall deals damage");}finally{clean(p);}h.succeed();}
    @GameTest(template="empty") public static void verticalEnhancedJumpProtectedButPassiveTappingAndHoldingAreNot(GameTestHelper h){var p=player(h);try{PewterFeruchemy.tick(p);p.causeFallDamage(7,1,p.damageSources().fall());h.assertTrue(Math.abs(p.getHealth()-16)<.001,"Passive strength tapping grants no fall immunity");p.setHealth(20);PowerService.control(p,new PowerNetwork.Control(2,"",1));PewterFeruchemy.tick(p);p.causeFallDamage(7,1,p.damageSources().fall());h.assertTrue(Math.abs(p.getHealth()-16)<.001,"Holding activation without jumping grants no immunity");p.setHealth(20);launch(p,false);p.causeFallDamage(20,1,p.damageSources().fall());h.assertTrue(p.getHealth()==20,"Activated vertical boosted jump also lands safely");}finally{clean(p);}h.succeed();}
    @GameTest(template="empty") public static void safeLandingAndDimensionCleanupCannotBankProtection(GameTestHelper h){var p=player(h);boolean old=FeruchemyConfig.PEWTER_SAFE_LANDING.get();try{launch(p,true);p.setOnGround(true);PewterFeruchemy.tick(p);p.causeFallDamage(7,1,p.damageSources().fall());h.assertTrue(Math.abs(p.getHealth()-16)<.001,"Safe landing without a damage event clears protection");p.setHealth(20);launch(p,true);new PewterFeruchemy().dimension(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,p.level().dimension(),p.level().dimension()));p.causeFallDamage(7,1,p.damageSources().fall());h.assertTrue(Math.abs(p.getHealth()-16)<.001,"Dimension cleanup clears pending flight");p.setHealth(20);PowerData.state(p,"f:pewter",10);launch(p,true);FeruchemyConfig.PEWTER_SAFE_LANDING.set(false);p.causeFallDamage(7,1,p.damageSources().fall());h.assertTrue(Math.abs(p.getHealth()-16)<.001,"Feature toggle restores ordinary fall behavior");}finally{FeruchemyConfig.PEWTER_SAFE_LANDING.set(old);clean(p);}h.succeed();}
}
