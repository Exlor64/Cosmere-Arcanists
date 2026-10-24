package dev.cosmerearcanists.power;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.equipment.*;
import dev.cosmerearcanists.hemalurgy.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AluminumSpikeGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AluminumTest"));p.setGameMode(GameType.SURVIVAL);PowerAttributes.migrate(p);return p;}
    private static ItemStack equip(FakePlayer p){var stack=new ItemStack(EquipmentContent.SPIKES.get("aluminum").get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);return stack;}
    private static LivingDeathEvent queue(FakePlayer attacker,net.minecraft.world.entity.LivingEntity target){target.setHealth(0);var event=new LivingDeathEvent(target,attacker.damageSources().playerAttack(attacker));HemalurgyService.capture(event);return event;}
    @GameTest(template="empty") public static void lethalAluminumSuppressesAllPowersWithoutStealingOrErasing(GameTestHelper h){var p=player(h);var donor=player(h);PowerAttributes.set(donor,"a:steel",2);PowerAttributes.set(donor,"f:gold",1);PowerData.reserve(donor,"steel",40);PowerData.state(donor,"a:steel",2);var stack=equip(p);queue(p,donor);h.assertTrue(!AluminumSuppression.active(donor),"Wait for final death cancellation");HemalurgyService.flush();h.assertTrue(AluminumSuppression.remaining(donor)==20L*HemalurgyConfig.ALUMINUM_SECONDS.get(),"Full configured five-minute default timer starts at settlement");h.assertTrue(PowerData.available(donor).isEmpty()&&PowerAttributes.strength(donor,"a:steel")==0&&PowerAttributes.strength(donor,"f:gold")==0,"Both arts are inaccessible");h.assertTrue(PowerAttributes.baseStrength(donor,"a:steel")==2&&PowerAttributes.baseStrength(donor,"f:gold")==1&&PowerData.reserve(donor,"steel")==40,"Learned powers and reserves survive");h.assertTrue(!HemalurgicSpikes.marked(stack)&&stack.getCount()==1&&PowerAttributes.baseStrength(p,"a:steel")==0,"Aluminum is not charged, consumed or granted to killer");h.succeed();}
    @GameTest(template="empty") public static void suppressionSurvivesCloneAndExpiresWithoutRegranting(GameTestHelper h){var p=player(h);PowerAttributes.set(p,"a:steel",1.5);PowerAttributes.set(p,"f:gold",0);AluminumSuppression.apply(p);var replacement=player(h);PowerData.clonePlayer(p,replacement);h.assertTrue(AluminumSuppression.remaining(replacement)==AluminumSuppression.remaining(p)&&PowerData.available(replacement).isEmpty(),"Respawn preserves deadline");PowerData.state(replacement,"a:steel",3);h.assertTrue(PowerData.state(replacement,"a:steel")==0,"Cannot reactivate during suppression");PowerData.data(replacement).putLong("hemalurgic_aluminum_until",replacement.server.overworld().getGameTime());h.assertTrue(!AluminumSuppression.active(replacement)&&PowerAttributes.strength(replacement,"a:steel")==1.5&&PowerAttributes.baseStrength(replacement,"f:gold")==0,"Exact deadline restores existing access without recreating absent powers");h.assertTrue(PowerData.state(replacement,"a:steel")==0,"Expiry requires deliberate reactivation");h.succeed();}
    @GameTest(template="empty") public static void canceledMobAndChangedWeaponAluminumKillsHaveNoEffect(GameTestHelper h){var p=player(h);var donor=player(h);var stack=equip(p);var event=queue(p,donor);event.setCanceled(true);HemalurgyService.flush();h.assertTrue(!AluminumSuppression.active(donor),"Canceled death does not suppress");var zombie=EntityType.ZOMBIE.create(h.getLevel());PowerAttributes.migrate(p);queue(p,zombie);HemalurgyService.flush();h.assertTrue(!HemalurgicSpikes.marked(stack)&&!zombie.getPersistentData().hasUUID("cosmerearcanists_hemalurgic_claim"),"Aluminum does nothing to mobs");queue(p,donor);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);HemalurgyService.flush();h.assertTrue(!AluminumSuppression.active(donor),"Changing weapon before settlement aborts");h.succeed();}
}
