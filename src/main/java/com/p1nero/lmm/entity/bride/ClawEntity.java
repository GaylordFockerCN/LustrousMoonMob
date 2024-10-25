package com.p1nero.lmm.entity.bride;

import com.p1nero.lmm.entity.LMMEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtils;

/**
 * 树爪继承自Mob，和平模式无法召唤！！
 */
public class ClawEntity extends Mob implements GeoEntity{
    private Bride bride;
    private Player target;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int catchTimer;
    private final int catchTimerMax = 20;//逃离时间
    private boolean isCatching;
    public ClawEntity(EntityType<? extends ClawEntity> p_37466_, Level p_37467_) {
         super(p_37466_, p_37467_);
         SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public ClawEntity(Level world, Bride thrower, Player target){
        super(LMMEntities.CLAW.get(), world);
        bride = thrower;
        this.target = target;
        catchTimer = catchTimerMax;
        isCatching = false;
        setNoAi(true);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .build();
    }

    public Bride getBride() {
        return bride;
    }

    @Override
    public void tick() {
        super.tick();
        catchTimer--;
        if(catchTimer < 0 && !isCatching && this.target != null && !level().isClientSide && checkHit(target.getOnPos(),1)){
            target.hurt(level().damageSources().mobAttack(getBride()),10);
            isCatching = true;
        }
        if(isCatching && this.target != null && !level().isClientSide){
            target.teleportTo(target.getX(),target.getY(),target.getZ());
        }
        if(catchTimer < -catchTimerMax * 5){//禁锢5秒
            this.discard();//时间够久就自毁
        }
    }
    public boolean checkHit(BlockPos pos1, int offSet){
        BlockPos pos2 = getOnPos();
        return pos1.getX()<=pos2.getX()+offSet && pos1.getX()>=pos2.getX()-offSet
                && pos1.getZ()<=pos2.getZ()+offSet && pos1.getZ()>=pos2.getZ()-offSet;
    }

    public void catchPlayer(){
        triggerAnim("Catch","catch");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "Catch", 0, state -> PlayState.STOP)
                .triggerableAnim("catch", RawAnimation.begin().thenPlay("catch")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return  RenderUtils.getCurrentTick();
    }
}
