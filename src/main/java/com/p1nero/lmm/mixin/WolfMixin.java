package com.p1nero.lmm.mixin;

import com.p1nero.lmm.entity.yangjian.YangJian;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Wolf.class)
public abstract class WolfMixin extends TamableAnimal implements NeutralMob {

    protected WolfMixin(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void inject(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir){
        if(pSource.getEntity() instanceof YangJian){
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void inject(DamageSource pCause, CallbackInfo ci){
        if(pCause.getEntity() instanceof YangJian){
            ci.cancel();
        }
    }
}
