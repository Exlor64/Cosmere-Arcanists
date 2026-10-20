package dev.cosmerearcanists.feruchemy;
import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
/** Kept outside the GameTest holder so the no-Curios matrix never loads optional API classes. */
public final class CuriosMetalmindTests {
    private static ItemStack item(String slot){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"iron_metalmind_"+slot)));}
    public static void verify(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"CuriosFerring"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);PowerAttributes.set(p,"f:iron",1);
        var inventory=CuriosApi.getCuriosInventory(p).orElseThrow();
        for(String slot:List.of("ring","bracelet","necklace")){var holder=inventory.getStacksHandler(slot).orElseThrow();int count=slot.equals("necklace")?1:2;h.assertTrue(holder.getStacks().getSlots()==count,"Player has2 rings,2 bracelets and1 necklace by default");for(int i=0;i<count;i++)holder.getStacks().setStackInSlot(i,item(slot));}
        h.assertTrue(CuriosMetalminds.touching(p).size()==5,"All five functional worn slots are available to the storage service");
        PowerData.state(p,"f:iron",-5);IronFeruchemy.tick(p);double charge=Metalminds.accessibleCharge(p,"iron");h.assertTrue(Math.abs(charge-.025)<1e-7&&Math.abs(IronFeruchemy.massMultiplier(p)-.5)<1e-7,"Worn metalminds automatically store one shared budget without holding an item");
        var rings=inventory.getStacksHandler("ring").orElseThrow();var first=rings.getStacks().getStackInSlot(0);Metalminds.transfer(first,p.getUUID(),"iron",Metalminds.capacity(first));IronFeruchemy.tick(p);h.assertTrue(Metalminds.charge(rings.getStacks().getStackInSlot(1))>0,"Storage moves to the next appropriate worn item when the first is full");
        var cosmetic=item("ring");Metalminds.transfer(cosmetic,p.getUUID(),"iron",10);rings.getCosmeticStacks().setStackInSlot(0,cosmetic);double cosmeticBefore=Metalminds.charge(cosmetic);PowerData.state(p,"f:iron",1);IronFeruchemy.tick(p);h.assertTrue(IronFeruchemy.massMultiplier(p)>1&&Metalminds.charge(cosmetic)==cosmeticBefore,"Tapping uses functional jewelry and ignores cosmetic slots");
        var foreign=UUID.randomUUID();for(var access:CuriosMetalminds.touching(p))Metalminds.transfer(access.stack(),p.getUUID(),"iron",Metalminds.capacity(access.stack()));
        h.assertTrue(Metalminds.transfer(first,foreign,"iron",1)==0,"Worn storage retains identity ownership");IronFeruchemy.mass(p,1);
    }
    private CuriosMetalmindTests(){}
}
