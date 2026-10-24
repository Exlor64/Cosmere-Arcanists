package dev.cosmerearcanists.fortune;

/** Persistent IDs; adding an entry does not change existing cooldown names. */
public enum Misfortune {
    WALL_DETECTION(.1,1200,3,false,"a hostile noticed you through cover"),
    LIGHTNING(.3,6000,1,false,"lightning found you"),
    DROPPED_ITEM_VEER(.05,200,5,true,"your dropped item veered away"),
    PROJECTILE_VEER(.1,200,4,true,"your throw went astray"),
    LOST_CATCH(.15,600,3,true,"your catch slipped free"),
    TOOL_CHIP(.05,600,5,false,"your tool chipped"),
    ARMOR_WEAR(.1,600,4,false,"your armor took a knock"),
    STUMBLE(.05,400,5,false,"you stumbled"),
    CRAMP(.05,600,5,false,"your leg cramped"),
    WEAK_GRIP(.1,600,4,false,"your grip weakened"),
    DIZZY(.15,800,3,false,"a dizzy spell caught you"),
    DUST_IN_EYES(.25,1200,2,false,"dust got in your eyes"),
    FATIGUE(.1,800,4,false,"your arms grew heavy"),
    HUNGER_PANG(.05,600,5,false,"a hunger pang struck"),
    LOST_BREATH(.15,800,3,false,"you lost your breath underwater"),
    FIRE_FLARE(.2,1000,2,false,"the flames flared up"),
    CHILL(.1,800,3,false,"the cold bit deeper"),
    SHIELD_SLIP(.15,800,3,false,"your guard slipped");
    public final double minimum;public final int cooldown,weight;public final boolean action;public final String message;
    Misfortune(double minimum,int cooldown,int weight,boolean action,String message){this.minimum=minimum;this.cooldown=cooldown;this.weight=weight;this.action=action;this.message=message;}
    public String key(){return name().toLowerCase(java.util.Locale.ROOT);}
}
