package dev.cosmerearcanists.power;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.currency.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScadrialWorldGameTests {
    @GameTest(template="empty") public static void dimensionUsesOverworldStyleAndIndependentSeed(GameTestHelper h)throws Exception{var server=h.getLevel().getServer();try(var reader=server.getResourceManager().getResourceOrThrow(ResourceLocation.parse("cosmerearcanists:dimension/scadrial.json")).openAsReader()){var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,server.registryAccess());var stem=net.minecraft.world.level.dimension.LevelStem.CODEC.parse(ops,com.google.gson.JsonParser.parseReader(reader)).getOrThrow();h.assertTrue(stem.type().value().natural()&&stem.generator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,"Actual Scadrial resource decodes to Overworld-style natural noise generation");}var live=server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(ScadrialWorld.ID)));if(live!=null)h.assertTrue(live.getSeed()==ScadrialWorld.seed(server.overworld().getSeed()),"Live Scadrial uses independent seed");h.assertTrue(ScadrialWorld.seed(server.overworld().getSeed())!=server.overworld().getSeed(),"Distinct seed policy");h.succeed();}
    @GameTest(template="empty") public static void scadrialVillagersUseTenClipCurrency(GameTestHelper h){var villager=new Villager(EntityType.VILLAGER,h.getLevel());var originalDimensions=CurrencyConfig.DIMENSIONS.get();int old=CurrencyConfig.CLIPS_PER_BOXING.get();try{CurrencyConfig.DIMENSIONS.set(java.util.List.of(h.getLevel().dimension().location().toString()));CurrencyConfig.CLIPS_PER_BOXING.set(10);var offers=new MerchantOffers();offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,2),new ItemStack(Items.BREAD),16,1,.05f));CurrencyTrades.prepare(villager,offers);h.assertTrue(offers.stream().noneMatch(o->o.getBaseCostA().is(Items.EMERALD)||o.getCostB().is(Items.EMERALD)||o.getResult().is(Items.EMERALD)),"Dimension-scoped villagers convert emerald legs");var exchange=offers.stream().filter(CurrencyTrades::exchange).findFirst().orElseThrow();h.assertTrue(exchange.getBaseCostA().getCount()==10&&exchange.getCostB().isEmpty()&&exchange.getResult().is(ModMetals.BOXING.get()),"Exactly10clips per boxing");CurrencyConfig.CLIPS_PER_BOXING.set(100);CurrencyTrades.prepare(villager,offers);CurrencyConfig.CLIPS_PER_BOXING.set(10);CurrencyTrades.prepare(villager,offers);h.assertTrue(offers.stream().filter(CurrencyTrades::exchange).count()==1&&offers.stream().filter(CurrencyTrades::exchange).findFirst().orElseThrow().getBaseCostA().getCount()==10,"Existing exchange updates to configured10:1 without duplicate offers");}finally{CurrencyConfig.CLIPS_PER_BOXING.set(old);CurrencyConfig.DIMENSIONS.set(originalDimensions);}h.succeed();}
}
