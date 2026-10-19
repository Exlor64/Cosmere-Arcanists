package dev.cosmerearcanists.client;

import java.util.*;
import dev.cosmerearcanists.power.SeekingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Bronze is private directional rhythm, not entity outlines or a public sound broadcast. */
public final class SeekingClient {
    private static SeekingPacket status=new SeekingPacket("",1,false,false,0,List.of());
    private static long received;
    private static final Map<String,Long> BEATS=new HashMap<>();
    public static void receive(SeekingPacket packet){var mc=Minecraft.getInstance();if(mc.level==null || !packet.dimension().equals(mc.level.dimension().location().toString()))return;status=packet;received=mc.level.getGameTime();if(!packet.bronze())BEATS.clear();}
    private static boolean active(){var mc=Minecraft.getInstance();return mc.player!=null && mc.level!=null && mc.player.isAlive() && !mc.player.isSpectator() && status.dimension().equals(mc.level.dimension().location().toString()) && mc.level.getGameTime()-received<status.lifetime();}
    private static String key(SeekingPacket.Pulse p){return p.art()+"/"+p.effect()+"/"+Math.round(p.position().x/2)+"/"+Math.round(p.position().z/2);}
    private static double beat(SeekingPacket.Pulse p,long tick){return 1-Math.floorMod(tick+key(p).hashCode(),p.period())/(double)p.period();}
    private static net.minecraft.sounds.SoundEvent instrument(){return switch(PowerClientConfig.SEEKING_INSTRUMENT.get()) {
        case BASS_DRUM -> SoundEvents.NOTE_BLOCK_BASEDRUM.value();case BASS -> SoundEvents.NOTE_BLOCK_BASS.value();
        case HARP -> SoundEvents.NOTE_BLOCK_HARP.value();case SNARE -> SoundEvents.NOTE_BLOCK_SNARE.value();
        case HAT -> SoundEvents.NOTE_BLOCK_HAT.value();case BELL -> SoundEvents.NOTE_BLOCK_BELL.value();
        case CHIME -> SoundEvents.NOTE_BLOCK_CHIME.value();case FLUTE -> SoundEvents.NOTE_BLOCK_FLUTE.value();
    };}
    @SubscribeEvent public void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(!active()){status=new SeekingPacket("",1,false,false,0,List.of());BEATS.clear();return;}
        long tick=mc.level.getGameTime();BEATS.entrySet().removeIf(e->tick-e.getValue()>240);
        if(!status.bronze() || !PowerClientConfig.SEEKING_SOUNDS.get() || mc.isPaused())return;
        int played=0;
        for(var pulse:status.pulses().stream().limit(PowerClientConfig.SEEKING_LIMIT.get()).toList()) {
            String key=key(pulse);if(beat(pulse,tick)<.999 || BEATS.getOrDefault(key,Long.MIN_VALUE)==tick)continue;
            BEATS.put(key,tick);if(played++>=4)continue;
            var direction=pulse.position().subtract(mc.player.getEyePosition()).normalize();var position=mc.player.getEyePosition().add(direction.scale(2));
            float pitch=.65f+Math.floorMod((pulse.art()+pulse.effect()).hashCode(),9)*.09f;
            mc.level.playLocalSound(position.x,position.y,position.z,instrument(),SoundSource.PLAYERS,(float)(PowerClientConfig.SEEKING_VOLUME.get()*pulse.clarity()),pitch,false);
        }
    }
    private static String effect(SeekingPacket.Pulse p){
        if(p.effect().isEmpty())return "Unfamiliar Investiture";
        String name=p.effect().substring(p.effect().indexOf(':')+1).replace('_',' ');name=Character.toUpperCase(name.charAt(0))+name.substring(1);
        if(!p.art().equals("cosmerearcanists:allomancy")){String art=p.art().substring(p.art().indexOf(':')+1).replace('_',' ');name=art+": "+name;}
        return name;
    }
    @SubscribeEvent public void hud(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();if(!active() || mc.options.hideGui || mc.screen!=null || !PowerClientConfig.SEEKING_HUD.get())return;
        var g=event.getGuiGraphics();int center=g.guiWidth()/2,y=25;
        if(status.ownCloud()>0)g.drawCenteredString(mc.font,String.format(Locale.ROOT,"Coppercloud active · %.1f m",status.ownCloud()),center,y,0xC5A181);
        if(!status.bronze())return;if(status.ownCloud()>0)y+=12;
        var pulses=status.pulses().stream().limit(PowerClientConfig.SEEKING_LIMIT.get()).toList();
        g.fill(center-93,y,center+94,y+14,0xA010151C);
        g.drawCenteredString(mc.font,"|",center,y+2,0x777777);
        for(var pulse:pulses) {
            var offset=pulse.position().subtract(mc.player.position());double bearing=Mth.wrapDegrees((float)(Math.toDegrees(Math.atan2(-offset.x,offset.z))-mc.player.getYRot()));
            int x=center+(int)(bearing/180*88);float beat=(float)beat(pulse,mc.level.getGameTime());int alpha=(int)(80+175*pulse.clarity()*(.3+.7*beat));
            int color=alpha<<24|0xD7A15A;g.fill(x-1,y+3,x+2,y+11,color);
        }
        g.drawCenteredString(mc.font,"Bronze · "+status.pulses().size()+" pulse"+(status.pulses().size()==1?"":"s"),center,y+17,0xD7A15A);
        if(!status.focused())return;
        int row=0;
        for(var pulse:pulses) {
            double distance=pulse.position().distanceTo(mc.player.position()),height=pulse.position().y-mc.player.getY();
            String state=pulse.burnState()==0?"":pulse.burnState()==1?" · low burn":pulse.burnState()==2?" · burning":" · flaring";
            String direction=height>3?" · above":height< -3?" · below":"";
            g.drawCenteredString(mc.font,effect(pulse)+" · ~"+(int)(Math.round(distance/2)*2)+" m"+direction+state,center,y+31+row*11,0xE4C59E);
            if(++row>=3)break;
        }
    }
}
