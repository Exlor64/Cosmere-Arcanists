package dev.cosmerearcanists.feruchemy;
import net.minecraft.world.item.Item;
/** Dedicated band: always one item per slot, even before it is first charged. */
public final class MetalmindItem extends Item {
    private final String metal,slot;private final int units;
    public MetalmindItem(String metal,String slot,int units){super(new Properties().stacksTo(1));this.metal=metal;this.slot=slot;this.units=units;}
    public String slot(){return slot;}
    public int units(){return units;}
    public String metal(){return metal;}
}
