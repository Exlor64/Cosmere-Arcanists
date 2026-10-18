package dev.cosmerearcanists.client;

import java.util.*;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.power.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PowerClient {
    public static final KeyMapping SELECT = new KeyMapping("key.cosmerearcanists.select", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.cosmerearcanists");
    public static final KeyMapping STATE = new KeyMapping("key.cosmerearcanists.state", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.cosmerearcanists");
    public static final KeyMapping APPLY = new KeyMapping("key.cosmerearcanists.apply", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.cosmerearcanists");
    public static final KeyMapping SECONDARY = new KeyMapping("key.cosmerearcanists.secondary", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.cosmerearcanists");
    private static boolean secondaryApplied,secondaryAlternate,assignmentChanged;
    private static int lastSecondaryActivation;
    public static final KeyMapping ALTERNATE = new KeyMapping("key.cosmerearcanists.alternate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.cosmerearcanists");
    private static int lastActivation;
    private static boolean alternateLatched, alternatePreviouslyHeld;
    private static PowerNetwork.Snapshot snapshot = new PowerNetwork.Snapshot("", "", "", 5, List.of(), List.of());
    private static boolean selecting, choosingState, applied, connected;
    private static int selectTicks, stateTicks, clock, previewIndex, previewState;
    private static boolean scrolledSelect, scrolledState;
    public static void receive(PowerNetwork.Snapshot packet) { snapshot = packet; }
    private static Vec3 walkingForce = Vec3.ZERO;
    private static int impulseClock = -100;
    public static void receiveImpulse(PowerNetwork.Impulse packet) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !packet.dimension().equals(mc.level.dimension().location().toString())) return;
        Vec3 previous = mc.player.getDeltaMovement();
        double[] result = ImpulsePhysics.add(new double[]{previous.x, previous.y, previous.z},
            new double[]{packet.x(),packet.y(),packet.z()}, packet.maxSpeed());
        mc.player.setDeltaMovement(new Vec3(result[0],result[1],result[2]));
        if (packet.resistWalking()) {
            Vec3 force = new Vec3(packet.x(),0,packet.z());
            walkingForce = impulseClock == clock ? walkingForce.add(force) : force;
            impulseClock = clock;
        }
    }
    @SubscribeEvent public void movementInput(MovementInputUpdateEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || mc.level == null || clock - impulseClock > 2) return;
        var input = event.getInput(); double yaw = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double x = -sin*input.forwardImpulse + cos*input.leftImpulse;
        double z = cos*input.forwardImpulse + sin*input.leftImpulse;
        double[] filtered = ImpulsePhysics.walkingInput(x,z,walkingForce.x,walkingForce.z);
        input.forwardImpulse = (float)(-sin*filtered[0] + cos*filtered[1]);
        input.leftImpulse = (float)(cos*filtered[0] + sin*filtered[1]);
    }
    private static String selected() { return snapshot.selected(); }
    private static String preview() { return snapshot.powers().isEmpty() ? "" : snapshot.powers().get(Math.floorMod(previewIndex, snapshot.powers().size())).power(); }
    private static int state(String power) { return snapshot.powers().stream().filter(e -> e.power().equals(power)).mapToInt(PowerNetwork.Entry::state).findFirst().orElse(0); }
    private static void send(int action, String power, int value) { PacketDistributor.sendToServer(new PowerNetwork.Control(action, power, value)); }
    private static boolean playing() { var mc = Minecraft.getInstance(); return mc.player != null && mc.level != null && mc.screen == null && mc.isWindowActive(); }
    @SubscribeEvent public void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance(); clock++;
        boolean selectClick = false, stateClick = false, activateClick = false;
        while (SELECT.consumeClick()) selectClick = true;
        while (STATE.consumeClick()) stateClick = true;
        while (APPLY.consumeClick()) activateClick = true;
        boolean secondaryClick=false; while(SECONDARY.consumeClick())secondaryClick=true;
        while (ALTERNATE.consumeClick()) { }
        if (mc.player == null || mc.level == null) {
            snapshot = new PowerNetwork.Snapshot("", "", "", 5, List.of(), List.of()); connected = false; walkingForce = Vec3.ZERO; impulseClock = -100; selecting = choosingState = applied = false; alternateLatched = alternatePreviouslyHeld = false; lastActivation = lastSecondaryActivation = 0; secondaryApplied=secondaryAlternate=false; return;
        }
        if (!connected) { connected = true; send(4, "", 0); }
        if (!playing()) {
            if (applied) send(2, "", 0);
            if(secondaryApplied)send(7,"",0);
            selecting = choosingState = applied = false; alternateLatched = alternatePreviouslyHeld = false; lastActivation = lastSecondaryActivation = 0; secondaryApplied=secondaryAlternate=false; return;
        }
        if (selectClick && !SELECT.isDown() && !selecting && !selected().isEmpty()) send(3, selected(), 0);
        if (stateClick && !STATE.isDown() && !choosingState && !SELECT.isDown() && !selected().isEmpty()) send(3, selected(), 0);
        boolean down = SELECT.isDown();
        if (down && !selecting) {
            selecting = true; selectTicks = 0; assignmentChanged=false; scrolledSelect = false; previewIndex = 0;
            for (int i = 0; i < snapshot.powers().size(); i++) if (snapshot.powers().get(i).power().equals(selected())) previewIndex = i;
        }
        if (down) selectTicks++;
        if (!down && selecting) {
            if(assignmentChanged) { /* A menu click already applied its change; release must not overwrite slot assignments. */ }
            else if (selectTicks >= PowerClientConfig.HOLD_TICKS.get() || scrolledSelect) { if (!preview().isEmpty()) send(0, preview(), 0); }
            else if (!selected().isEmpty()) send(3, selected(), 0);
            selecting = false;
        }
        down = STATE.isDown() && !selecting;
        if (down && !choosingState) { choosingState = true; stateTicks = 0; scrolledState = false; previewState = state(selected()); }
        if (down) stateTicks++;
        if (!down && choosingState) {
            if (!selected().isEmpty()) {
                if (stateTicks >= PowerClientConfig.HOLD_TICKS.get() || scrolledState) send(1, selected(), previewState);
                else send(3, selected(), 0);
            }
            choosingState = false;
        }
        down = APPLY.isDown() && !selecting && !choosingState;
        boolean alternateReady = ALTERNATE.isDown() && alternatePreviouslyHeld;
        if (down && !applied) alternateLatched = alternateReady;
        int inputs = movementInputs(mc);
        if (activateClick && !APPLY.isDown() && !selecting && !choosingState) send(5, "", (alternateReady ? 2 : 0) | inputs);
        int activation = down ? 1 | (alternateLatched ? 2 : 0) | inputs : 0;
        if (activation != lastActivation || down && clock % 2 == 0) send(2, "", activation);
        lastActivation = activation; applied = down;
        if (!down) alternateLatched = false;
        boolean secondaryDown=SECONDARY.isDown() && !selecting && !choosingState;
        if(secondaryDown && !secondaryApplied)secondaryAlternate=alternateReady;
        if(secondaryClick && !SECONDARY.isDown() && !selecting && !choosingState)send(8,"",(alternateReady?2:0)|inputs);
        int secondaryActivation=secondaryDown?1|(secondaryAlternate?2:0)|inputs:0;
        if(secondaryActivation!=lastSecondaryActivation || secondaryDown && clock%2==0)send(7,"",secondaryActivation);
        lastSecondaryActivation=secondaryActivation; secondaryApplied=secondaryDown;
        if(!secondaryDown)secondaryAlternate=false;
        alternatePreviouslyHeld = ALTERNATE.isDown();
    }
    private static int movementInputs(Minecraft mc) {
        return (mc.options.keyUp.isDown() ? 4 : 0) | (mc.options.keyDown.isDown() ? 8 : 0)
            | (mc.options.keyLeft.isDown() ? 16 : 0) | (mc.options.keyRight.isDown() ? 32 : 0)
            | (mc.options.keyJump.isDown() ? 64 : 0) | (mc.options.keyShift.isDown() ? 128 : 0);

    }
    @SubscribeEvent public void scroll(InputEvent.MouseScrollingEvent event) {
        if (!playing() || event.getScrollDeltaY() == 0) return;
        int direction = event.getScrollDeltaY() > 0 ? -1 : 1;
        if (SELECT.isDown()) {
            if (!selecting) {
                selecting = true; selectTicks = 0; assignmentChanged=false; previewIndex = 0;
                for (int i = 0; i < snapshot.powers().size(); i++) if (snapshot.powers().get(i).power().equals(selected())) previewIndex = i;
            }
            if (!snapshot.powers().isEmpty()) previewIndex = Math.floorMod(previewIndex + direction, snapshot.powers().size());
            scrolledSelect = true; event.setCanceled(true);
        } else if (STATE.isDown()) {
            if (!choosingState) { choosingState = true; stateTicks = 0; previewState = state(selected()); }
            previewState = PowerRules.clamp(selected(), previewState + direction, snapshot.levels());
            scrolledState = true; event.setCanceled(true);
        }
    }
    @SubscribeEvent public void click(InputEvent.MouseButton.Pre event) {
        boolean left=event.getButton()==GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean right=event.getButton()==GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        if(playing() && SELECT.isDown() && selecting && (left||right)) {
            if(event.getAction()==GLFW.GLFW_PRESS && !preview().isEmpty()) {
                if(right){send(6,preview(),0);assignmentChanged=true;}
                else {send(3,preview(),0);assignmentChanged=true;}
            }
            scrolledSelect=true;
            if(left)Minecraft.getInstance().options.keyAttack.setDown(false);
            else Minecraft.getInstance().options.keyUse.setDown(false);
            event.setCanceled(true);
        }
    }
    private static String slotLabel(String power) {
        return power.equals(selected())?APPLY.getTranslatedKeyMessage().getString():power.equals(snapshot.secondary())?SECONDARY.getTranslatedKeyMessage().getString():"—";
    }
    private static String name(String key) {
        if (key.isEmpty()) return "No power";
        String metal = key.substring(2); return Character.toUpperCase(metal.charAt(0)) + metal.substring(1) + (key.startsWith("a:") ? " · Allomancy" : " · Feruchemy");
    }
    private static ItemStack icon(String power) {
        if (power.isEmpty()) return ItemStack.EMPTY;
        String metal = power.substring(2); String namespace = Set.of("iron", "gold", "copper").contains(metal) ? "minecraft" : CosmereArcanists.MOD_ID;
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, metal + "_ingot")));
    }
    @SubscribeEvent public void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        var g = event.getGuiGraphics(); int width = g.guiWidth(), height = g.guiHeight();
        if (PowerClientConfig.HUD.get() && !selected().isEmpty()) {
            int x = PowerClientConfig.HUD_X.get() < 0 ? width - 200 : PowerClientConfig.HUD_X.get();
            int y = PowerClientConfig.HUD_Y.get() < 0 ? height - 65 : PowerClientConfig.HUD_Y.get();
            g.fill(x - 3, y - 3, x + 194, y + 34, 0xB0101B29); g.renderItem(icon(selected()), x, y);
            g.drawString(mc.font, mc.font.plainSubstrByWidth("["+slotLabel(selected())+"] "+name(selected()),170), x + 21, y, 0x91CFFF);
            String label = PowerRules.label(selected(), state(selected()));
            if (!PowerRules.implemented(selected())) label += " · effects pending";
            g.drawString(mc.font, label, x + 21, y + 12, 0xFFFFFF);
            if (selected().startsWith("a:")) {
                double reserve = snapshot.powers().stream().filter(e -> e.power().equals(selected())).mapToDouble(PowerNetwork.Entry::reserve).findFirst().orElse(0);
                g.drawString(mc.font, String.format(Locale.ROOT, "Reserve: %.1f", reserve), x + 21, y + 24, 0xACB9C8);
            }
        }
        if(PowerClientConfig.HUD.get() && !snapshot.secondary().isEmpty()) {
            int x=PowerClientConfig.HUD_X.get()<0?width-200:PowerClientConfig.HUD_X.get();
            int y=(PowerClientConfig.HUD_Y.get()<0?height-65:PowerClientConfig.HUD_Y.get())-44;
            String power=snapshot.secondary();
            g.fill(x-3,y-3,x+194,y+34,0xB0101B29); g.renderItem(icon(power),x,y);
            g.drawString(mc.font,mc.font.plainSubstrByWidth("["+slotLabel(power)+"] "+name(power),170),x+21,y,0x91CFFF);
            String label=PowerRules.label(power,state(power)); if(!PowerRules.implemented(power))label+=" · effects pending";
            g.drawString(mc.font,label,x+21,y+12,0xFFFFFF);
            if(power.startsWith("a:")) {
                double reserve=snapshot.powers().stream().filter(e->e.power().equals(power)).mapToDouble(PowerNetwork.Entry::reserve).findFirst().orElse(0);
                g.drawString(mc.font,String.format(Locale.ROOT,"Reserve: %.1f",reserve),x+21,y+24,0xACB9C8);
            }
        }
        if (selecting && (selectTicks >= PowerClientConfig.HOLD_TICKS.get() || scrolledSelect)) {
            int x = width / 2 - 130, y = Math.max(20, height / 2 - 70);
            g.fill(x - 8, y - 25, x + 260, y + 105, 0xE0101B29);
            g.drawString(mc.font, "Choose power · scroll · release to select", x, y - 17, 0x91CFFF);
            if (snapshot.powers().isEmpty()) g.drawString(mc.font, "You have no available powers.", x, y + 20, 0xFFFFFF);
            else for (int delta = -Math.min(2, (snapshot.powers().size() - 1) / 2); delta < Math.min(5, snapshot.powers().size()) - Math.min(2, (snapshot.powers().size() - 1) / 2); delta++) {
                int index = Math.floorMod(previewIndex + delta, snapshot.powers().size()); var power = snapshot.powers().get(index).power(); int row = y + (delta + 2) * 18;
                if (delta == 0) g.fill(x - 3, row - 2, x + 253, row + 16, 0xA0356491);
                g.renderItem(icon(power), x, row); g.drawString(mc.font, "["+slotLabel(power)+"] "+name(power), x + 23, row + 4, delta == 0 ? 0xFFFFFF : 0x8797A8);
            }
            g.drawString(mc.font, "Left: state · Right: switch activation key", x, y + 94, 0xACB9C8);
        } else if (choosingState && (stateTicks >= PowerClientConfig.HOLD_TICKS.get() || scrolledState)) {
            int x = width / 2 - 110, y = height / 2 - 25;
            g.fill(x - 8, y - 12, x + 220, y + 45, 0xE0101B29);
            g.drawString(mc.font, name(selected()), x, y, 0x91CFFF);
            g.drawString(mc.font, PowerRules.label(selected(), previewState), x, y + 14, 0xFFFFFF);
            g.drawString(mc.font, "Scroll · release to set state", x, y + 28, 0xACB9C8);
        }
    }
    @SubscribeEvent public void lines(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || mc.player == null || mc.level == null || !PowerClientConfig.LINES.get()
            || !snapshot.dimension().equals(mc.level.dimension().location().toString()) || snapshot.targets().isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 chest = mc.player.getPosition(partial).add(0, mc.player.getBbHeight() * .65, 0), camera = event.getCamera().getPosition();
        // Reuse the world pose: the event matrix is not a substitute for this stack.
        // Subtract the camera in doubles before converting vertices to floats.
        Vec3 relativeChest = chest.subtract(camera);
        var pose = event.getPoseStack().last(); var buffers = mc.renderBuffers().bufferSource(); var type = MetalLineRenderType.get(); var vertices = buffers.getBuffer(type);
        for (var target : snapshot.targets()) {
            Vec3 point = target.position(mc.level, partial); if (point == null) continue;
            Vec3 normal = point.subtract(chest).normalize();
            Vec3 relativePoint = point.subtract(camera);
            boolean aimed = point.subtract(mc.player.getEyePosition(partial)).normalize().dot(mc.player.getLookAngle()) > Math.cos(Math.toRadians(12));
            int red = aimed ? 115 : 35, green = aimed ? 205 : 125, alpha = aimed ? 240 : 160;
            vertices.addVertex(pose.pose(), (float)relativeChest.x, (float)relativeChest.y, (float)relativeChest.z).setColor(red, green, 255, alpha).setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
            vertices.addVertex(pose.pose(), (float)relativePoint.x, (float)relativePoint.y, (float)relativePoint.z).setColor(red, green, 255, alpha).setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
        }
        buffers.endBatch(type);
    }
}
