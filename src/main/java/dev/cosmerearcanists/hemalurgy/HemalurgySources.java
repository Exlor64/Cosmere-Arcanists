package dev.cosmerearcanists.hemalurgy;
import dev.cosmerearcanists.compounding.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.registries.BuiltInRegistries;
/** Personal powers are removed; natural traits are recorded from the dead donor without inventing additional permanent respawn penalties. */
public final class HemalurgySources {
    public record Source(String power,double strength,List<IdentityMath.Portion> identities,HemalurgicTraits.Senses senses,double attackBase,HemalurgicTraits.Cognition cognition){
        public Source(String power,double strength,List<IdentityMath.Portion> identities){this(power,strength,identities,HemalurgicTraits.NONE,0,HemalurgicTraits.NO_COGNITION);}
        public Source{identities=List.copyOf(identities);}
        public boolean trait(){return power.startsWith("trait:");}
    }
    private static boolean eligible(LivingEntity entity,String metal){return entity.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cosmerearcanists",metal.equals("zinc")?"hemalurgy_living_humanoids":"hemalurgy_intelligent_humanoids")));}
    private static double healthBase(LivingEntity entity){var attr=entity.getAttribute(Attributes.MAX_HEALTH);return attr==null?0:attr.getBaseValue();}
    private static double attackBase(LivingEntity entity){var attr=entity.getAttribute(Attributes.ATTACK_DAMAGE);return attr==null?0:attr.getBaseValue();}
    private static HemalurgicTraits.Senses senses(LivingEntity entity){return HemalurgyConfig.senses(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),entity.getType().getCategory()==MobCategory.MONSTER);}
    private static double strength(LivingEntity entity,String key){if(entity instanceof ServerPlayer p)return PowerAttributes.baseStrength(p,key);var holder=PowerAttributes.holder(key);var attr=holder==null?null:entity.getAttribute(holder);return attr==null?0:attr.getBaseValue();}
    private static List<IdentityMath.Portion> identities(LivingEntity e,String key,double strength){return e instanceof ServerPlayer p?PowerSources.personal(p,key):List.of(new IdentityMath.Portion(dev.cosmerearcanists.mobs.NaturalMobPowers.identity(e),strength));}
    public static boolean enabled(String key){if(key.equals(HemalurgicTraits.STRENGTH)||key.equals(HemalurgicTraits.SENSES)||key.equals(HemalurgicTraits.FORTITUDE)||key.equals(HemalurgicTraits.MEMORY))return true;if(!PowerData.powerEnabled(key))return false;String metal=key.substring(2);if(key.startsWith("a:"))return EnhancementService.featureEnabled(metal);return dev.cosmerearcanists.feruchemy.FeruchemyConfig.ENABLED.get()&&(metal.equals("copper")?dev.cosmerearcanists.copper.CopperService.configured():metal.equals("nicrosil")?dev.cosmerearcanists.nicrosil.NicrosilService.configured():dev.cosmerearcanists.feruchemy.FeruchemicalEnhancement.enabled(metal));}
    public static Source select(LivingEntity victim,String metal){
        if(metal.equals("iron")){double base=attackBase(victim),gain=HemalurgicTraits.iron(base);return gain<=0?null:new Source(HemalurgicTraits.STRENGTH,gain,List.of(new IdentityMath.Portion(victim.getUUID(),gain)),HemalurgicTraits.NONE,base,HemalurgicTraits.NO_COGNITION);}
        if(metal.equals("tin")){var values=senses(victim);double gain=values.maximum();return gain<=0?null:new Source(HemalurgicTraits.SENSES,gain,List.of(new IdentityMath.Portion(victim.getUUID(),gain)),values,0,HemalurgicTraits.NO_COGNITION);}
        if(metal.equals("zinc")||metal.equals("copper")){if(!eligible(victim,metal))return null;var cognition=metal.equals("zinc")?HemalurgicTraits.zinc(healthBase(victim),attackBase(victim)):HemalurgicTraits.copper(HemalurgicTraits.intelligence(BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString()),victim.getRandom().nextDouble());double gain=cognition.resistance();return gain<=0?null:new Source(metal.equals("zinc")?HemalurgicTraits.FORTITUDE:HemalurgicTraits.MEMORY,gain,List.of(new IdentityMath.Portion(victim.getUUID(),gain)),HemalurgicTraits.NONE,attackBase(victim),cognition);}
        var values=new HashMap<String,Double>();for(String key:HemalurgyRules.POWERS.getOrDefault(metal,Set.of()))if(enabled(key))values.put(key,strength(victim,key));String key=HemalurgyRules.choose(metal,values);if(key.isEmpty())return null;double strength=values.get(key);return new Source(key,strength,identities(victim,key,strength));}
    public static boolean unchanged(LivingEntity e,Source s){if(s.power().equals(HemalurgicTraits.STRENGTH))return attackBase(e)==s.attackBase();if(s.power().equals(HemalurgicTraits.SENSES))return senses(e).equals(s.senses());if(s.power().equals(HemalurgicTraits.FORTITUDE))return eligible(e,"zinc")&&HemalurgicTraits.zinc(healthBase(e),attackBase(e)).equals(s.cognition());if(s.power().equals(HemalurgicTraits.MEMORY))return eligible(e,"copper");double now=strength(e,s.power());return now==s.strength()&&now>0&&identities(e,s.power(),now).equals(s.identities());}
    public static void remove(LivingEntity e,Source s){if(!unchanged(e,s))throw new IllegalStateException("Power source changed before theft");if(s.trait())return;if(e instanceof ServerPlayer p){PowerAttributes.set(p,s.power(),0);PowerService.powerTransferred(p,s.power());}else e.getAttribute(PowerAttributes.holder(s.power())).setBaseValue(0);}
    private HemalurgySources(){}
}
