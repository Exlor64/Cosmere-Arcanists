package dev.cosmerearcanists.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cosmerearcanists.power.PowerNetwork;
import dev.cosmerearcanists.power.TinMath;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.opengl.GL11;

public final class TinClient {
    private static PowerNetwork.TinStatus status=new PowerNetwork.TinStatus("",0,1,false);
    private static long statusReceived;
    private static float vision,previousVision;
    private record Echo(PowerNetwork.TinEcho packet,long started,Vec3 from) { }
    private static final List<Echo> ECHOES=new ArrayList<>();
    public static void status(PowerNetwork.TinStatus packet){var mc=Minecraft.getInstance();if(mc.level==null || !packet.dimension().equals(mc.level.dimension().location().toString()))return;statusReceived=mc.level.getGameTime();status=packet;if(!packet.wireframes())ECHOES.clear();}
    public static void echo(PowerNetwork.TinEcho packet){
        var mc=Minecraft.getInstance();if(mc.level==null || !PowerClientConfig.TIN_ECHOES.get() || !packet.dimension().equals(mc.level.dimension().location().toString()))return;
        // Keep independent waves, but render each mob once using its strongest active echo.
        Vec3 from=feet(packet.sourceBox());
        for(var old:ECHOES.reversed())if(packet.entityId()>=0 && old.packet.entityId()==packet.entityId()){from=anchor(old,1);break;}
        ECHOES.add(new Echo(packet,mc.level.getGameTime(),from));
        while(ECHOES.size()>PowerClientConfig.TIN_ECHO_LIMIT.get())ECHOES.removeFirst();
    }
    private static boolean active(){var mc=Minecraft.getInstance();return mc.level!=null && mc.player!=null && mc.player.isAlive() && !mc.player.isSpectator() && mc.level.getGameTime()-statusReceived>=0 && mc.level.getGameTime()-statusReceived<40 && status.strength()>0 && status.dimension().equals(mc.level.dimension().location().toString());}
    /** Fraction sampled by Minecraft's lightmap shader; potion night vision is left untouched. */
    public static double nightVision(float partial){return Mth.lerp(Math.clamp(partial,0,1),previousVision,vision);}
    private static boolean wireActive(){return active() && status.wireframes();}
    @SubscribeEvent public void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        previousVision=vision;
        if(mc.level==null || mc.player==null || !mc.player.isAlive() || mc.player.isSpectator() || !status.dimension().equals(mc.level.dimension().location().toString()) || !PowerClientConfig.TIN_LIGHTMAP.get())vision=previousVision=0;
        else{
            double target=active()?Math.clamp(status.vision()*PowerClientConfig.TIN_VISION_SCALE.get(),0,1):0;
            vision+=(float)((target-vision)*-Math.expm1(-1/PowerClientConfig.TIN_VISION_FADE.get()));if(Math.abs(vision-target)<.0001)vision=(float)target;
        }
        if(!active()){ECHOES.clear();if(mc.level==null)status=new PowerNetwork.TinStatus("",0,1,false);return;}
        if(!wireActive())ECHOES.clear();
        ECHOES.removeIf(e->!e.packet.dimension().equals(status.dimension()) || mc.level.getGameTime()-e.started>=e.packet.ticks());
    }
    @SubscribeEvent public void fov(ViewportEvent.ComputeFov event){if(active() && PowerClientConfig.TIN_ZOOM.get() && event.usedConfiguredFov())event.setFOV(Math.max(5,event.getFOV()/Math.max(1,status.zoom())));}
    private static final VertexConsumer DISCARD=new VertexConsumer() {
        public VertexConsumer addVertex(float x,float y,float z){return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    };
    /** NO_DEPTH_TEST does not guarantee that a previously enabled world depth test is disabled.
     * Flush this pass explicitly without depth testing, then restore the caller's state. */
    private static void flushThroughWalls(MultiBufferSource.BufferSource buffers,RenderType type){
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try{RenderSystem.disableDepthTest();buffers.endBatch(type);}
        finally{if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();}
    }
    private static Vec3 feet(AABB box){return new Vec3((box.minX+box.maxX)*.5,box.minY,(box.minZ+box.maxZ)*.5);}
    private static Vec3 anchor(Echo echo,float partial){
        var mc=Minecraft.getInstance();var entity=echo.packet.entityId()<0?null:mc.level.getEntity(echo.packet.entityId());
        if(entity!=null && !entity.isRemoved())return entity.getPosition(partial);
        return echo.from.lerp(feet(echo.packet.sourceBox()),TinMath.smoothProgress(mc.level.getGameTime()-echo.started+partial,4));
    }
    private static float alpha(Echo echo,double age){return (float)Math.clamp(TinMath.echoFade(age,echo.packet.ticks(),PowerClientConfig.TIN_HOLD.get())*echo.packet.audible()*PowerClientConfig.TIN_OPACITY.get()*PowerClientConfig.TIN_BRIGHTNESS.get()*PowerClientConfig.TIN_PULSE_STRENGTH.get(),0,1);}
    private static float mobAlpha(Echo echo,double age){return (float)(alpha(echo,age)*TinMath.waveReveal(age,echo.packet.sourceBox().getCenter().distanceTo(echo.packet.position()),PowerClientConfig.TIN_WAVE_SPEED.get()));}
    private static void wave(com.mojang.blaze3d.vertex.PoseStack pose,VertexConsumer vertices,Vec3 origin,double radius,float alpha){
        if(radius<.05 || alpha<.01)return;
        for(int plane=0;plane<3;plane++)for(int i=0;i<48;i++){
            double a=i*Math.PI*2/48,b=(i+1)*Math.PI*2/48;
            Vec3 start=plane==0?new Vec3(Math.cos(a)*radius,0,Math.sin(a)*radius):plane==1?new Vec3(Math.cos(a)*radius,Math.sin(a)*radius,0):new Vec3(0,Math.cos(a)*radius,Math.sin(a)*radius);
            Vec3 end=plane==0?new Vec3(Math.cos(b)*radius,0,Math.sin(b)*radius):plane==1?new Vec3(Math.cos(b)*radius,Math.sin(b)*radius,0):new Vec3(0,Math.cos(b)*radius,Math.sin(b)*radius);
            var normal=end.subtract(start).normalize();start=start.add(origin);end=end.add(origin);
            vertices.addVertex(pose.last().pose(),(float)start.x,(float)start.y,(float)start.z).setColor(.78f,.93f,1f,alpha).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
            vertices.addVertex(pose.last().pose(),(float)end.x,(float)end.y,(float)end.z).setColor(.78f,.93f,1f,alpha).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
        }
    }
    private record Puff(Vec3 position,float width,float height,float opacity) { }
    private static void puff(java.util.List<Puff> puffs,Vec3 position,double width,double height,float opacity){
        if(opacity>.003)puffs.add(new Puff(position,(float)width,(float)height,opacity));
    }
    /** Gaussian billboards provide vague sound impressions without a full-screen blur or hard mesh edges. */
    private static void softEchoes(RenderLevelStageEvent event){
        var mc=Minecraft.getInstance();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);double now=mc.level.getGameTime()+partial;
        var camera=event.getCamera().getPosition();var strongest=new HashMap<Integer,Echo>();
        for(var echo:ECHOES)if(echo.packet.entityId()>=0){var old=strongest.get(echo.packet.entityId());if(old==null || mobAlpha(echo,now-echo.started)>mobAlpha(old,now-old.started))strongest.put(echo.packet.entityId(),echo);}
        var puffs=new ArrayList<Puff>();double size=PowerClientConfig.TIN_BLUR_SIZE.get();
        float opacity=(float)(PowerClientConfig.TIN_BLUR_OPACITY.get()/Math.sqrt(Math.max(1,ECHOES.size())));
        // Keep one softly connected body impression per actual sound source, not one per sound packet.
        for(var echo:strongest.values()){
            var entity=mc.level.getEntity(echo.packet.entityId());if(entity==mc.player)continue;
            var pos=anchor(echo,partial);var box=echo.packet.sourceBox();double width=Math.max(.45,box.getXsize()),height=Math.max(.4,box.getYsize());
            float a=mobAlpha(echo,now-echo.started)*opacity;
            if(PowerClientConfig.TIN_MODELS.get()){
                puff(puffs,pos.add(0,height*.28,0),width*1.5*size,height*.65*size,a*.65f);
                puff(puffs,pos.add(0,height*.58,0),width*1.7*size,height*.7*size,a);
                puff(puffs,pos.add(0,height*.86,0),width*1.3*size,height*.4*size,a*.8f);
            }else puff(puffs,pos.add(0,height*.5,0),width*1.7*size,height*1.2*size,a);
        }
        // Merge repeated surface impressions before drawing to prevent overlapping sounds whitening the view.
        var surfaces=new HashMap<net.minecraft.core.BlockPos,Puff>();
        for(var echo:ECHOES){double age=now-echo.started;float a=alpha(echo,age)*opacity*.55f;
            for(var box:echo.packet.surfaces()){
                var center=box.getCenter();float reveal=(float)TinMath.waveReveal(age,center.distanceTo(echo.packet.position()),PowerClientConfig.TIN_WAVE_SPEED.get());
                if(reveal<=0)continue;
                var key=net.minecraft.core.BlockPos.containing(center);var old=surfaces.get(key);float strength=a*reveal;
                if(old==null || strength>old.opacity())surfaces.put(key,new Puff(center,(float)(Math.clamp(Math.max(box.getXsize(),box.getZsize())*1.5,.5,2.5)*size),(float)(Math.clamp(box.getYsize()*1.5,.5,2.5)*size),strength));
            }
        }
        // Prefer nearby surfaces and preserve the source impressions when reaching the shared draw budget.
        var terrain=new ArrayList<>(surfaces.values());terrain.sort(Comparator.comparingDouble(p->p.position().distanceToSqr(camera)));puffs.addAll(terrain);
        var type=TinRenderTypes.blur();var buffers=mc.renderBuffers().bufferSource();var vertices=buffers.getBuffer(type);var pose=event.getPoseStack();int drawn=0;
        for(var puff:puffs){
            if(drawn>=PowerClientConfig.TIN_BLUR_LIMIT.get())break;
            if(puff.opacity()<.003 || puff.position().distanceToSqr(camera)<.75*.75)continue; // Avoid a cloud filling the camera.
            drawn++;pose.pushPose();
            try{pose.translate(puff.position().x-camera.x,puff.position().y-camera.y,puff.position().z-camera.z);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                float x=puff.width()*.5f,y=puff.height()*.5f;
                vertices.addVertex(pose.last().pose(),-x,-y,0).setUv(0,1).setColor(1f,1f,1f,puff.opacity());
                vertices.addVertex(pose.last().pose(),x,-y,0).setUv(1,1).setColor(1f,1f,1f,puff.opacity());
                vertices.addVertex(pose.last().pose(),x,y,0).setUv(1,0).setColor(1f,1f,1f,puff.opacity());
                vertices.addVertex(pose.last().pose(),-x,y,0).setUv(0,0).setColor(1f,1f,1f,puff.opacity());
            }finally{pose.popPose();}
        }
        flushThroughWalls(buffers,type);
    }
    @SubscribeEvent public void render(RenderLevelStageEvent event) {
        var mc=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || !wireActive() || !PowerClientConfig.TIN_ECHOES.get() || ECHOES.isEmpty())return;
        if(PowerClientConfig.TIN_ECHO_STYLE.get()==PowerClientConfig.TinEchoStyle.SOFT_BLUR){softEchoes(event);return;}
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);double now=mc.level.getGameTime()+partial;
        var strongest=new HashMap<Integer,Echo>();
        for(var echo:ECHOES)if(echo.packet.entityId()>=0){var previous=strongest.get(echo.packet.entityId());if(previous==null || mobAlpha(echo,now-echo.started)>mobAlpha(previous,now-previous.started))strongest.put(echo.packet.entityId(),echo);}
        for(var echo:ECHOES){
            double age=now-echo.started,speed=PowerClientConfig.TIN_WAVE_SPEED.get();float alpha=alpha(echo,age);if(alpha<.01)continue;
            var lines=TinRenderTypes.lines();var vertices=buffers.getBuffer(lines);double radius=3;
            for(var box:echo.packet.surfaces()){
                double distance=box.getCenter().distanceTo(echo.packet.position());radius=Math.max(radius,distance+1);
                float reveal=(float)TinMath.waveReveal(age,distance,speed);
                if(reveal>0)LevelRenderer.renderLineBox(pose,vertices,box.move(camera.scale(-1)),.78f,.93f,1f,alpha*reveal);
            }
            double front=age*speed,maxRadius=Math.min(34,radius);
            if(front<maxRadius)wave(pose,vertices,echo.packet.position().subtract(camera),front,(float)(alpha*.65*(1-front/maxRadius)));
            flushThroughWalls(buffers,lines);
            if(echo.packet.entityId()<0 || strongest.get(echo.packet.entityId())!=echo)continue;
            float modelAlpha=mobAlpha(echo,age);if(modelAlpha<.01)continue;
            var entity=mc.level.getEntity(echo.packet.entityId());if(entity==mc.player)continue;
            var position=anchor(echo,partial);var box=echo.packet.sourceBox().move(position.subtract(feet(echo.packet.sourceBox())));
            boolean[] modelBuffer={false};
            if(entity!=null && PowerClientConfig.TIN_MODELS.get()){
                var type=TinRenderTypes.models();MultiBufferSource wire=original->{if(original.format().equals(DefaultVertexFormat.NEW_ENTITY)){modelBuffer[0]=true;return buffers.getBuffer(type);}return DISCARD;};
                pose.pushPose();pose.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);
                try{
                    RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK,GL11.GL_LINE);RenderSystem.setShaderColor(.78f,.93f,1f,modelAlpha);
                    mc.getEntityRenderDispatcher().getRenderer(entity).render(entity,Mth.rotLerp(partial,entity.yRotO,entity.getYRot()),partial,pose,wire,LightTexture.FULL_BRIGHT);
                    flushThroughWalls(buffers,type);
                }finally{RenderSystem.setShaderColor(1,1,1,1);RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK,GL11.GL_FILL);pose.popPose();}
            }
            if(!modelBuffer[0]){LevelRenderer.renderLineBox(pose,buffers.getBuffer(lines),box.move(camera.scale(-1)),.85f,.97f,1f,modelAlpha);flushThroughWalls(buffers,lines);}
        }
    }
}
