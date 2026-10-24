package dev.cosmerearcanists.fortune;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.feruchemy.ChromiumFeruchemy;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.*;
import net.neoforged.neoforge.registries.*;

/** Additive, bounded extra table samples; never replaces a tapping player's existing loot or recursively reruns GLMs. */
public final class FortuneLootModifier extends LootModifier {
    public static final MapCodec<FortuneLootModifier> CODEC=RecordCodecBuilder.mapCodec(inst->codecStart(inst).apply(inst,FortuneLootModifier::new));
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER=DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,CosmereArcanists.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>,MapCodec<FortuneLootModifier>> SERIALIZER=REGISTER.register("fortune",()->CODEC);
    public static final TagKey<Item> VALUABLE=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"fortune_valuable"));
    public static final TagKey<Item> PROTECTED=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"fortune_protected"));
    private static final ThreadLocal<Boolean> SAMPLING=ThreadLocal.withInitial(()->false);
    public FortuneLootModifier(LootItemCondition[] conditions){super(conditions);}
    public static void register(IEventBus bus){REGISTER.register(bus);}
    @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
    public static int score(ItemStack item){return FortuneMath.score(item.getRarity().ordinal(),item.is(VALUABLE),item.isEnchanted(),item.getCount());}
    private static int best(List<ItemStack> items){int selected=-1;for(int i=0;i<Math.min(256,items.size());i++){var stack=items.get(i);if(stack.isEmpty()||stack.is(PROTECTED))continue;if(selected<0||score(stack)>score(items.get(selected)))selected=i;}return selected;}
    public static ServerPlayer mobPlayer(LootContext context){var credited=context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);if(credited instanceof ServerPlayer p)return p;return context.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof ServerPlayer p?p:null;}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        if(SAMPLING.get()||!ChromiumFeruchemy.configured())return loot;
        var subject=context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if(subject instanceof FishingHook hook){var owner=hook.getPlayerOwner();if(FortuneConfig.FISHING.get()&&owner instanceof ServerPlayer p&&!loot.isEmpty()&&MisfortuneService.lostCatch(p))loot.clear();return loot;}
        var id=context.getQueriedLootTableId();if(id==null)return loot;var key=ResourceKey.create(Registries.LOOT_TABLE,id);var table=context.getLevel().getServer().reloadableRegistries().getLootTable(key);
        ServerPlayer player=null;
        if(FortuneConfig.CHESTS.get()&&table.getParamSet()==LootContextParamSets.CHEST&&subject instanceof ServerPlayer p)player=p;
        else if(FortuneConfig.MOBS.get()&&table.getParamSet()==LootContextParamSets.ENTITY&&subject instanceof Mob)player=mobPlayer(context);
        if(player==null||player.level()!=context.getLevel())return loot;
        double stored=ChromiumFeruchemy.storedFraction(player);if(stored>0){if(context.getRandom().nextDouble()<FortuneMath.probability(stored,FortuneConfig.LOOT_PENALTY.get())){int index=best(loot);if(index>=0)loot.remove(index);}return loot;}
        double budget=ChromiumFeruchemy.bonusAttempts(player);if(budget<=0)return loot;int attempts=FortuneMath.attempts(budget,context.getRandom().nextDouble());
        if(attempts==0)return loot;
        SAMPLING.set(true);try{for(int i=0;i<attempts;i++){var candidates=new ObjectArrayList<ItemStack>();table.getRandomItemsRaw(context,stack->{if(candidates.size()<256)candidates.add(stack);});int index=best(candidates);if(index>=0){var candidate=candidates.get(index);loot.add(candidate.copyWithCount(Math.min(candidate.getCount(),candidate.getMaxStackSize())));}}}finally{SAMPLING.remove();}
        return loot;
    }
}
