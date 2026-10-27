package dev.cosmerearcanists.koloss;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
/** Covers eggs/spawners as well as natural checks. Explicit raw-NBT commands remain an admin tool. */
public final class KolossSpawning {
    @SubscribeEvent public void finalizeSpawn(FinalizeSpawnEvent event){
        if(event.getEntity() instanceof KolossEntity&&(!KolossConfig.SPAWNING.get()||!KolossEntity.spawnDimension(event.getLevel())))event.setSpawnCancelled(true);
    }
}
