package dev.cosmerearcanists.client;

import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.power.PowerNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CosmereArcanists.MOD_ID, dist = Dist.CLIENT)
public final class PowerClientMod {
    public PowerClientMod(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, PowerClientConfig.SPEC);
        bus.addListener(this::keys);
        bus.addListener(TinRenderTypes::registerShaders);
        bus.addListener(BronzeDangerClient::registerShaders);
        dev.cosmerearcanists.power.BronzeWarningPacket.receiver=BronzeDangerClient::receive;
        NeoForge.EVENT_BUS.register(new BronzeDangerClient());
        bus.addListener(this::renderers);
        bus.addListener(this::setup);
        dev.cosmerearcanists.power.EmotionPacket.receiver=EmotionClient::receive;
        NeoForge.EVENT_BUS.register(new EmotionClient());
        dev.cosmerearcanists.power.SeekingPacket.receiver=SeekingClient::receive;
        NeoForge.EVENT_BUS.register(new SeekingClient());
        dev.cosmerearcanists.power.ScoutPacket.receiver=ScoutClient::receive;
        NeoForge.EVENT_BUS.register(new ScoutClient());
        dev.cosmerearcanists.power.TemporalPacket.receiver=TemporalClient::receive;
        NeoForge.EVENT_BUS.register(new TemporalClient());
        dev.cosmerearcanists.power.GoldPacket.receiver=GoldClient::receive;
        NeoForge.EVENT_BUS.register(new GoldClient());
        dev.cosmerearcanists.power.BubbleService.clientEntityRate=BubbleClient::entityRate;
        dev.cosmerearcanists.power.BubbleService.clientRate=p->p==net.minecraft.client.Minecraft.getInstance().player?BubbleClient.rate():1;
        dev.cosmerearcanists.power.BubblePacket.receiver=BubbleClient::receive;
        NeoForge.EVENT_BUS.register(new BubbleClient());
        PowerNetwork.tinReceiver = TinClient::status;
        PowerNetwork.echoReceiver = TinClient::echo;
        NeoForge.EVENT_BUS.register(new TinClient());
        NeoForge.EVENT_BUS.register(new TinTouchClient());
        PowerNetwork.clientReceiver = PowerClient::receive;
        PowerNetwork.pewterReceiver = PewterClient::receive;
        NeoForge.EVENT_BUS.register(new PewterClient());
        PowerNetwork.impulseReceiver = PowerClient::receiveImpulse;
        NeoForge.EVENT_BUS.register(new PowerClient());
        NeoForge.EVENT_BUS.register(new EquipmentClient());
    }
    private void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(dev.cosmerearcanists.equipment.EquipmentContent.PROJECTILE.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(dev.cosmerearcanists.power.MetalProjectiles.STEEL.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }
    private void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
        var item=dev.cosmerearcanists.equipment.EquipmentContent.CROSSBOW.get();
        net.minecraft.client.renderer.item.ItemProperties.register(item,net.minecraft.resources.ResourceLocation.withDefaultNamespace("charged"),(stack,level,entity,seed)->net.minecraft.world.item.CrossbowItem.isCharged(stack)?1:0);
        net.minecraft.client.renderer.item.ItemProperties.register(item,net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"),(stack,level,entity,seed)->entity!=null && entity.isUsingItem() && entity.getUseItem()==stack?1:0);
        net.minecraft.client.renderer.item.ItemProperties.register(item,net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"),(stack,level,entity,seed)->entity!=null && entity.getUseItem()==stack?(float)(stack.getUseDuration(entity)-entity.getUseItemRemainingTicks())/net.minecraft.world.item.CrossbowItem.getChargeDuration(stack,entity):0);
    });}
    private void keys(RegisterKeyMappingsEvent event) {
        event.register(PowerClient.SELECT); event.register(PowerClient.STATE); event.register(PowerClient.APPLY); event.register(PowerClient.SECONDARY); event.register(PowerClient.ALTERNATE);
    }
}
