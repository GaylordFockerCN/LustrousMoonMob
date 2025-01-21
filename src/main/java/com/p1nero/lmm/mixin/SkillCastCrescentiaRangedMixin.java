//package com.p1nero.lmm.mixin;
//
//import com.aqutheseal.celestisynth.common.entity.skill.SkillCastCrescentiaRanged;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.level.Level;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;
//
//@Mixin(value = SkillCastCrescentiaRanged.class, remap = false)
//public class SkillCastCrescentiaRangedMixin {
//
//    /**
//     * 取消破坏方块
//     */
//    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
//    private boolean inject(Level instance, BlockPos pos, boolean b, Entity entity){
//        return false;
//    }
//
//}
