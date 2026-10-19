package dev.cosmerearcanists.filing;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FilingConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue PER_NUGGET, VIAL_CAPACITY, DRINK_TICKS, FILE_DAMAGE;
    public static final ModConfigSpec.DoubleValue RESERVE;
    static {
        var b=new ModConfigSpec.Builder();
        ENABLED=b.define("enabled",true);
        PER_NUGGET=b.comment("New nuggets/ingots/blocks provide this many filings per nugget equivalent. Existing filed pieces retain their original capacity.").defineInRange("filingsPerNugget",4,1,64);
        VIAL_CAPACITY=b.comment("Total filings, across every metal. Lowering this never deletes existing contents.").defineInRange("vialCapacity",16,1,256);
        RESERVE=b.comment("Reserve restored by each consumed filing. Filings that cannot fit fully remain in the vial.").defineInRange("reservePerFiling",50.0,.01,10000.0);
        DRINK_TICKS=b.defineInRange("drinkTicks",32,1,200);
        FILE_DAMAGE=b.defineInRange("fileWearPerFiling",1,0,16);
        SPEC=b.build();
    }
    private FilingConfig() { }
}
