package dev.cosmerearcanists.power;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class GoldConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,RECORD,EQUIPMENT,DETAILS,REQUIRE_HOLD;
    public static final ModConfigSpec.IntValue LIMIT,INTERVAL,COOLDOWN,UPDATE,BASE_ACCESS;
    public static final ModConfigSpec.DoubleValue DRAIN,DISTANCE;
    static{var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);RECORD=b.comment("Record all players so later Augurs can see earlier versions. No inventories or locations are saved.").define("recordHistory",true);
        EQUIPMENT=b.define("showHistoricalEquipment",true);DETAILS=b.define("showHistoricalDetails",true);REQUIRE_HOLD=b.define("requireActivationHold",true);
        LIMIT=b.defineInRange("maxSavedVersions",16,2,64);INTERVAL=b.defineInRange("periodicSnapshotTicks",6000,200,72000);COOLDOWN=b.defineInRange("changedSnapshotCooldownTicks",1200,20,72000);UPDATE=b.defineInRange("visionUpdateTicks",5,1,10);BASE_ACCESS=b.defineInRange("baseAccessibleVersions",4,1,64);
        DRAIN=b.defineInRange("reservePerSecond",1.0,0.0,100.0);DISTANCE=b.defineInRange("shadowDistance",2.5,1.0,4.0);SPEC=b.build();}
    private GoldConfig(){}
}
