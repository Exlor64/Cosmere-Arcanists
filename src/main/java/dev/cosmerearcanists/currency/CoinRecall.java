package dev.cosmerearcanists.currency;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
public final class CoinRecall {
    private static final String TARGET="cosmerearcanists_coin_pull",SPEED="cosmerearcanists_coin_pull_speed";
    public static void start(ItemEntity item,ServerPlayer player,double speed){item.getPersistentData().putUUID(TARGET,player.getUUID());item.getPersistentData().putDouble(SPEED,speed);item.setNoGravity(true);item.setPickUpDelay(0);var offset=player.position().add(0,.5,0).subtract(item.position());item.setDeltaMovement(offset.normalize().scale(Math.min(Math.max(.05,speed),offset.length())));item.hurtMarked=true;}
    private static void stop(ItemEntity item){item.getPersistentData().remove(TARGET);item.getPersistentData().remove(SPEED);item.setNoGravity(false);}
    public static void move(ItemEntity item){var data=item.getPersistentData();if(item.level().isClientSide||!data.hasUUID(TARGET))return;var player=item.level().getPlayerByUUID(data.getUUID(TARGET));
        if(!CurrencyConfig.RECALL.get()||!Coins.coin(item.getItem())||player==null||!player.isAlive()||player.isSpectator()||player.distanceToSqr(item)>Math.pow(CurrencyConfig.RETURN_RANGE.get(),2)){stop(item);return;}
        var offset=player.position().add(0,.5,0).subtract(item.position());double saved=data.getDouble(SPEED);double speed=Math.min(CurrencyConfig.RETURN_SPEED.get(),Double.isFinite(saved)?Math.max(.05,saved):.05);item.setDeltaMovement(offset.normalize().scale(Math.min(speed,offset.length())));item.hurtMarked=true;item.hasImpulse=true;
    }
    @SubscribeEvent public void tick(EntityTickEvent.Post e){if(e.getEntity() instanceof ItemEntity item)move(item);}
}
