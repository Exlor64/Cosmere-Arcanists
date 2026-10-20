package dev.cosmerearcanists.power;

import java.util.*;
import java.util.function.IntConsumer;
import it.unimi.dsi.fastutil.longs.*;
import dev.cosmerearcanists.mixin.BubbleChunkTicksAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.*;

/** Local world clocks preserve pending vanilla queues, priorities and positions; no chunks are requested. */
public final class BubbleWorldService {
    private static final ThreadLocal<ServerLevel> LEVEL=new ThreadLocal<>();
    private static final ThreadLocal<Long> SCHEDULER_TIME=new ThreadLocal<>();
    private static final ThreadLocal<Integer> PHASE=ThreadLocal.withInitial(()->-1);
    private static final ThreadLocal<Boolean> RANDOM_STEP=ThreadLocal.withInitial(()->false);
    private static final Map<ScheduledTick<?>,Budget> BUDGETS=new WeakHashMap<>();
    private static final Map<ServerLevel,Work> WORK=new WeakHashMap<>();
    private static final class Budget{double remaining;long frame;int phase;Budget(double remaining,long frame,int phase){this.remaining=remaining;this.frame=frame;this.phase=phase;}}
    private static final class Work{long frame=Long.MIN_VALUE;int count;}
    public static void levelTick(ServerLevel level,Runnable original){var before=LEVEL.get();LEVEL.set(level);try{original.run();}finally{if(before==null)LEVEL.remove();else LEVEL.set(before);}}
    private static boolean enabled(Object type){return type instanceof Block?BubbleConfig.SCHEDULED_BLOCKS.get():type instanceof Fluid&&BubbleConfig.SCHEDULED_FLUIDS.get();}
    public static void scheduled(ScheduledTick<?> tick){var level=LEVEL.get();if(level==null||!enabled(tick.type())||BubbleService.worldRate(level,tick.pos())==1)return;long time=SCHEDULER_TIME.get()==null?level.getGameTime():SCHEDULER_TIME.get();BUDGETS.put(tick,new Budget(Math.max(0,tick.triggerTick()-time),time,PHASE.get()));}
    public static <T> void scheduledFrame(Long2ObjectMap<LevelChunkTicks<T>> containers,Long2LongMap next,long time,int maximum,IntConsumer original){
        var level=LEVEL.get();if(level==null||(!BubbleConfig.SCHEDULED_BLOCKS.get()&&!BubbleConfig.SCHEDULED_FLUIDS.get())||BubbleService.bubbles(level).isEmpty()){original.accept(maximum);return;}
        int steps=BubbleConfig.MAX_STEPS.get(),previous=PHASE.get();var previousTime=SCHEDULER_TIME.get();SCHEDULER_TIME.set(time);try{for(int phase=0;phase<steps;phase++){PHASE.set(phase);retime(level,containers,next,time,phase,steps);original.accept(Math.max(1,maximum/steps));}}finally{PHASE.set(previous);if(previousTime==null)SCHEDULER_TIME.remove();else SCHEDULER_TIME.set(previousTime);}
    }
    /** Public for deterministic engine tests: executes exactly the same path used by the server-level mixin. */
    public static <T> void tickQueue(ServerLevel level,LevelTicks<T> ticks,long time,int maximum,java.util.function.BiConsumer<BlockPos,T> consumer){levelTick(level,()->ticks.tick(time,maximum,consumer));}
    private static <T> void retime(ServerLevel level,Long2ObjectMap<LevelChunkTicks<T>> containers,Long2LongMap next,long time,int phase,int steps){
        var work=WORK.computeIfAbsent(level,l->new Work());if(work.frame!=time){work.frame=time;work.count=0;}if(work.count>=BubbleConfig.WORLD_WORK.get())return;
        var chunks=new LongOpenHashSet();for(var b:BubbleService.bubbles(level)){int x1=net.minecraft.util.Mth.floor(b.center().x-b.radius())>>4,x2=net.minecraft.util.Mth.floor(b.center().x+b.radius())>>4,z1=net.minecraft.util.Mth.floor(b.center().z-b.radius())>>4,z2=net.minecraft.util.Mth.floor(b.center().z+b.radius())>>4;for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)chunks.add(ChunkPos.asLong(x,z));}
        for(long chunk:chunks){var container=containers.get(chunk);if(container==null)continue;var access=(BubbleChunkTicksAccess<T>)(Object)container;var queue=access.cosmerearcanists$queue();if(queue.isEmpty())continue;var replacements=new HashMap<ScheduledTick<T>,ScheduledTick<T>>();
            for(var tick:queue){if(work.count++>=BubbleConfig.WORLD_WORK.get())break;if(!enabled(tick.type()))continue;double rate=BubbleService.worldRate(level,tick.pos());if(Math.abs(rate-1)<1e-6){BUDGETS.remove(tick);continue;}
                var budget=BUDGETS.get(tick);if(budget==null||budget.frame<time-1||budget.frame>time){budget=new Budget(Math.max(0,tick.triggerTick()-time+1),time,-1);BUDGETS.put(tick,budget);}if(budget.frame==time&&budget.phase==phase)continue;
                budget.remaining-=rate/steps;budget.frame=time;budget.phase=phase;long due=budget.remaining<=1e-8?time:time+Math.max(1,(long)Math.ceil(budget.remaining));if(due==tick.triggerTick())continue;
                var replacement=new ScheduledTick<>(tick.type(),tick.pos(),due,tick.priority(),tick.subTickOrder());replacements.put(tick,replacement);
            }
            if(!replacements.isEmpty()){queue.removeAll(replacements.keySet());for(var entry:replacements.entrySet()){access.cosmerearcanists$positions().remove(entry.getKey());access.cosmerearcanists$positions().add(entry.getValue());queue.add(entry.getValue());var budget=BUDGETS.remove(entry.getKey());BUDGETS.put(entry.getValue(),budget);}var first=queue.peek();if(first==null)next.remove(chunk);else next.put(chunk,first.triggerTick());}
            if(work.count>=BubbleConfig.WORLD_WORK.get())return;
        }
    }
    public static int randomCount(double rate,RandomSource random){int full=(int)Math.floor(rate);double fraction=rate-full;return full+(fraction>1e-8&&random.nextDouble()<fraction?1:0);}
    public static void randomBlock(ServerLevel level,BlockPos pos,RandomSource random,Runnable original){if(RANDOM_STEP.get()||!BubbleConfig.RANDOM_BLOCKS.get()){original.run();return;}double rate=BubbleService.worldRate(level,pos);if(Math.abs(rate-1)<1e-6){original.run();return;}int count=randomCount(rate,random);RANDOM_STEP.set(true);try{for(int i=0;i<count;i++){var state=level.getBlockState(pos);if(!state.isRandomlyTicking())break;state.randomTick(level,pos,random);}}finally{RANDOM_STEP.set(false);}}
    public static void randomFluid(ServerLevel level,BlockPos pos,RandomSource random,Runnable original){if(RANDOM_STEP.get()||!BubbleConfig.RANDOM_FLUIDS.get()){original.run();return;}double rate=BubbleService.worldRate(level,pos);if(Math.abs(rate-1)<1e-6){original.run();return;}int count=randomCount(rate,random);RANDOM_STEP.set(true);try{for(int i=0;i<count;i++){var state=level.getFluidState(pos);if(!state.isRandomlyTicking())break;state.randomTick(level,pos,random);}}finally{RANDOM_STEP.set(false);}}
    public static void clear(){BUDGETS.clear();WORK.clear();LEVEL.remove();PHASE.remove();SCHEDULER_TIME.remove();}
    private BubbleWorldService(){}
}
