package dev.cosmerearcanists.aondor;

import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.cosmerearcanists.power.PowerData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Named server-persistent drawing layouts; loading only restores ink, never casts or bypasses payment. */
public final class AonSavedSpells {
    private static final String KEY="aondor_saved_spells";
    private static final Map<ServerPlayer,AonDrawing> LAST=new WeakHashMap<>();
    static void checked(ServerPlayer p,AonDrawing drawing){LAST.put(p,drawing);}
    private static String name(String name){if(!name.matches("[a-z0-9_-]{1,32}"))throw new IllegalArgumentException("Spell name: 1–32lowercase letters, numbers, _ or -");return name;}
    static void save(ServerPlayer p,String name){name=name(name);var drawing=LAST.get(p);if(drawing==null)throw new IllegalArgumentException("Draw and check a complete spell first");var data=PowerData.data(p).getCompound(KEY);if(!data.contains(name)&&data.getAllKeys().size()>=8)throw new IllegalArgumentException("Saved spell limit8reached");data.put(name,AonDrawingNbt.save(drawing));PowerData.data(p).put(KEY,data);}
    static AonDrawing load(ServerPlayer p,String name){var data=PowerData.data(p).getCompound(KEY);if(!data.contains(name(name)))throw new IllegalArgumentException("No spell named "+name);var drawing=AonDrawingNbt.load(data.getCompound(name));AonProgram.compile(drawing,AonDorService.definitions(p),AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get(),AonDorConfig.FALLBACK_POTENCY.get());return drawing;}
    public static void commands(RegisterCommandsEvent event){var root=Commands.literal("aondor").requires(s->s.getEntity() instanceof ServerPlayer p&&AonDorService.allowed(p));
        root.then(Commands.literal("list").executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal("Saved Aon spells: "+String.join(", ",new TreeSet<>(PowerData.data(p).getCompound(KEY).getAllKeys()))));return 1;}));
        for(String operation:List.of("save","load","delete"))root.then(Commands.literal(operation).then(Commands.argument("name",StringArgumentType.word()).executes(c->{var p=c.getSource().getPlayerOrException();String key=StringArgumentType.getString(c,"name");try{
            switch(operation){case "save"->save(p,key);case "delete"->{name(key);var data=PowerData.data(p).getCompound(KEY);data.remove(key);PowerData.data(p).put(KEY,data);}case "load"->{var drawing=load(p,key);int slot=AonDorService.POWER.equals(PowerData.secondary(p))?1:0;PacketDistributor.sendToPlayer(p,new AonDorNetwork.Draft(new AonDorNetwork.Check(UUID.randomUUID(),p.level().dimension().location().toString(),slot,drawing)));}}
            p.sendSystemMessage(Component.literal("Aon spell "+operation+": "+key));return 1;
        }catch(IllegalArgumentException error){p.sendSystemMessage(Component.literal(error.getMessage()));return 0;}})));
        event.getDispatcher().register(root);
    }
    private AonSavedSpells(){}
}
