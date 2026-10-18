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
        bus.addListener(this::renderers);
        PowerNetwork.clientReceiver = PowerClient::receive;
        PowerNetwork.impulseReceiver = PowerClient::receiveImpulse;
        NeoForge.EVENT_BUS.register(new PowerClient());
    }
    private void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(dev.cosmerearcanists.power.MetalProjectiles.STEEL.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }
    private void keys(RegisterKeyMappingsEvent event) {
        event.register(PowerClient.SELECT); event.register(PowerClient.STATE); event.register(PowerClient.APPLY); event.register(PowerClient.SECONDARY); event.register(PowerClient.ALTERNATE);
    }
}
