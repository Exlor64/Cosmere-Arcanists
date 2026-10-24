package dev.cosmerearcanists.hemalurgy;
import java.util.*;
/** Natural trait yields, expressed separately from stolen Metallic Arts powers. */
public final class HemalurgicTraits {
    public static final String STRENGTH="trait:strength",SENSES="trait:senses",FORTITUDE="trait:fortitude",MEMORY="trait:memory";
    public record Senses(double sight,double hearing,double touch){
        public Senses{for(double value:new double[]{sight,hearing,touch})if(!Double.isFinite(value)||value<0||value>10)throw new IllegalArgumentException("Invalid senses");}
        public double maximum(){return Math.max(sight,Math.max(hearing,touch));}
        public Senses scale(double factor){if(!Double.isFinite(factor)||factor<0)throw new IllegalArgumentException("Invalid sense scale");return new Senses(sight*factor,hearing*factor,touch*factor);}
    }
    public static final Senses NONE=new Senses(0,0,0);
    /** Both branches meet at9 when the donor has16 base attack damage. No ordinary power-retention penalty applies. */
    public static double iron(double damage){return !Double.isFinite(damage)||damage<0?0:damage<=16?.5*damage+1:.25*damage+5;}
    public record Profile(String entity,Senses ratios){}
    public static Profile parse(String line){try{var fields=line.split(",");if(fields.length!=4||!fields[0].trim().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))return null;var values=new Senses(Double.parseDouble(fields[1].trim()),Double.parseDouble(fields[2].trim()),Double.parseDouble(fields[3].trim()));if(values.maximum()>=1||values.maximum()<=0)return null;return new Profile(fields[0].trim(),values);}catch(IllegalArgumentException e){return null;}}
    public static Senses select(String entity,Senses fallback,List<? extends String> entries){var result=fallback;for(var line:entries){var profile=parse(line);if(profile!=null&&profile.entity().equals(entity))result=profile.ratios();}return result;}
    /** Fractions of ONE configured Feruchemical tin tap level; profiles are game balance, not biological measurements. */
    public static List<String> defaults(){return List.of(
        "minecraft:bat,0.2,0.8,0.2","minecraft:warden,0,0.95,0.8",
        "minecraft:wolf,0.55,0.75,0.3","minecraft:fox,0.55,0.75,0.3",
        "minecraft:cat,0.7,0.7,0.2","minecraft:ocelot,0.7,0.7,0.2",
        "minecraft:spider,0.5,0.35,0.7","minecraft:cave_spider,0.5,0.35,0.7",
        "minecraft:rabbit,0.5,0.65,0.25","minecraft:horse,0.5,0.55,0.25",
        "minecraft:donkey,0.5,0.55,0.25","minecraft:mule,0.5,0.55,0.25",
        "minecraft:parrot,0.65,0.6,0.15","minecraft:dolphin,0.5,0.85,0.35",
        "minecraft:axolotl,0.25,0.35,0.4","minecraft:phantom,0.7,0.6,0.3",
        "minecraft:enderman,0.6,0.5,0.4","minecraft:silverfish,0.15,0.3,0.6",
        "minecraft:endermite,0.15,0.3,0.6","minecraft:skeleton,0.6,0.35,0.2",
        "minecraft:stray,0.6,0.35,0.2","minecraft:bogged,0.6,0.35,0.2",
        "minecraft:wither_skeleton,0.6,0.35,0.2","minecraft:guardian,0.65,0.5,0.45",
        "minecraft:elder_guardian,0.65,0.5,0.45","minecraft:cod,0.3,0.3,0.45",
        "minecraft:salmon,0.3,0.3,0.45","minecraft:pufferfish,0.3,0.3,0.45",
        "minecraft:tropical_fish,0.3,0.3,0.45","minecraft:squid,0.35,0.25,0.4",
        "minecraft:glow_squid,0.35,0.25,0.4");}
    public record Cognition(double resistance,double memoryCapacity){
        public Cognition{if(!Double.isFinite(resistance)||!Double.isFinite(memoryCapacity)||resistance<0||resistance>.4||memoryCapacity<0||memoryCapacity>.75)throw new IllegalArgumentException("Invalid cognition");}
    }
    public static final Cognition NO_COGNITION=new Cognition(0,0);
    /** Balance curve uses natural health and attack, excluding equipment and temporary modifiers. */
    public static Cognition zinc(double health,double attack){if(!Double.isFinite(health)||!Double.isFinite(attack)||health<0||attack<0)return NO_COGNITION;return new Cognition(Math.min(.4,.05+.005*health+.005*attack),0);}
    /** Higher intelligence biases the roll upward without removing individual variation. */
    public static Cognition copper(double intelligence,double roll){if(!Double.isFinite(intelligence)||intelligence<0||intelligence>1||!Double.isFinite(roll)||roll<0||roll>1)throw new IllegalArgumentException("Invalid memory roll");double resistance=.05+.20*Math.pow(roll,2.5-2*intelligence);return new Cognition(resistance,Math.min(.75,3*resistance));}
    public static double intelligence(String entity){return switch(entity){case "minecraft:evoker","minecraft:witch","minecraft:illusioner"->.95;case "minecraft:villager","minecraft:wandering_trader","minecraft:player"->.75;case "minecraft:enderman"->.65;case "minecraft:pillager","minecraft:vindicator"->.6;default->.45;};}
    private HemalurgicTraits(){}
}
