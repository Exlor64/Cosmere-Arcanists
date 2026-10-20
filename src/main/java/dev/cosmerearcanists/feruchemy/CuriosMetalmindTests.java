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
        PowerData.state(p,"f:iron",-5);IronFeruchemy.tick(p);double charge=Metalminds.accessibleCharge(p,"iron");h.assertTrue(Math.abs(charge-.015)<1e-7&&Math.abs(IronFeruchemy.massMultiplier(p)-.7)<1e-7,"Worn metalminds automatically store one shared budget without holding an item");
        var rings=inventory.getStacksHandler("ring").orElseThrow();var first=rings.getStacks().getStackInSlot(0);Metalminds.transfer(first,p.getUUID(),"iron",Metalminds.capacity(first));IronFeruchemy.tick(p);h.assertTrue(Metalminds.charge(rings.getStacks().getStackInSlot(1))>0,"Storage moves to the next appropriate worn item when the first is full");
        var cosmetic=item("ring");Metalminds.transfer(cosmetic,p.getUUID(),"iron",10);rings.getCosmeticStacks().setStackInSlot(0,cosmetic);double cosmeticBefore=Metalminds.charge(cosmetic);PowerData.state(p,"f:iron",1);IronFeruchemy.tick(p);h.assertTrue(IronFeruchemy.massMultiplier(p)>1&&Metalminds.charge(cosmetic)==cosmeticBefore,"Tapping uses functional jewelry and ignores cosmetic slots");
        var foreign=UUID.randomUUID();for(var access:CuriosMetalminds.touching(p))Metalminds.transfer(access.stack(),p.getUUID(),"iron",Metalminds.capacity(access.stack()));
        h.assertTrue(Metalminds.transfer(first,foreign,"iron",1)==0,"Worn storage retains identity ownership");IronFeruchemy.mass(p,1);
    }
    public static void verifyConfigurableSlots(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"ConfiguredCurios"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);PowerAttributes.set(p,"f:iron",1);
        int rings=FeruchemyConfig.RING_SLOTS.get(),bracelets=FeruchemyConfig.BRACELET_SLOTS.get(),necklaces=FeruchemyConfig.NECKLACE_SLOTS.get();var inventory=CuriosApi.getCuriosInventory(p).orElseThrow();
        try{
            FeruchemyConfig.RING_SLOTS.set(3);FeruchemyConfig.BRACELET_SLOTS.set(4);FeruchemyConfig.NECKLACE_SLOTS.set(2);CuriosMetalminds.configureSlots(p);CuriosMetalminds.configureSlots(p);
            var ring=inventory.getStacksHandler("ring").orElseThrow();h.assertTrue(ring.getSlots()==3&&inventory.getStacksHandler("bracelet").orElseThrow().getSlots()==4&&inventory.getStacksHandler("necklace").orElseThrow().getSlots()==2,"Configured counts update real Curios inventories without accumulating modifiers");
            var piece=item("ring");ring.getStacks().setStackInSlot(2,piece);PowerData.state(p,"f:iron",-5);IronFeruchemy.tick(p);h.assertTrue(Math.abs(Metalminds.charge(piece)-.015)<1e-7,"Configured extra slots participate in storing beyond the old two-ring limit");
            var external=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"test_other_slot_provider");inventory.addTransientSlotModifier("ring",external,1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);ring.update();CuriosMetalminds.configureSlots(p);h.assertTrue(ring.getSlots()==4,"Unrelated mod slot bonuses remain intact");inventory.removeSlotModifier("ring",external);ring.update();
            var cosmetic=item("ring");Metalminds.transfer(cosmetic,p.getUUID(),"iron",7);ring.getCosmeticStacks().setStackInSlot(2,cosmetic);
            ring.clearCachedModifiers(); // Forces Curios to defer this resize for the current tick.
            FeruchemyConfig.RING_SLOTS.set(0);FeruchemyConfig.BRACELET_SLOTS.set(0);FeruchemyConfig.NECKLACE_SLOTS.set(0);CuriosMetalminds.configureSlots(p);p.tickCount++;
            h.assertTrue(ring.getSlots()==0&&inventory.getStacksHandler("bracelet").orElseThrow().getSlots()==0&&inventory.getStacksHandler("necklace").orElseThrow().getSlots()==0,"Zero removes this mod's default slots");
            inventory.handleInvalidStacks();
            double recovered=p.getInventory().items.stream().filter(Metalminds::bound).mapToDouble(Metalminds::charge).sum();h.assertTrue(Math.abs(recovered-7.015)<1e-7,"Both functional and cosmetic jewelry displaced by shrinking keep their exact stored charge");
        }finally{FeruchemyConfig.RING_SLOTS.set(rings);FeruchemyConfig.BRACELET_SLOTS.set(bracelets);FeruchemyConfig.NECKLACE_SLOTS.set(necklaces);CuriosMetalminds.configureSlots(p);IronFeruchemy.mass(p,1);}
    }
    private CuriosMetalmindTests(){}
}
