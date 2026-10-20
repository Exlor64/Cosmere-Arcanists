package dev.cosmerearcanists.mixin;

import java.util.Optional;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.EmotionService;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import org.spongepowered.asm.mixin.Mixin;

/** Preserve the chosen aggro target while letting species-specific brain attacks execute normally. */
@Mixin(Brain.class)
public abstract class AngerBrainTargetMixin {
    @WrapMethod(method="setMemoryInternal")
    private void cosmerearcanists$angerTarget(MemoryModuleType<?> type,Optional<?> value,Operation<Void> original){
        if(type==MemoryModuleType.ATTACK_TARGET){
            var override=EmotionService.angerBrainTarget((Brain<?>)(Object)this);
            if(override!=null)value=override.target()==null?Optional.empty():Optional.of(ExpirableValue.of(override.target()));
        }
        original.call(type,value);
    }
}
