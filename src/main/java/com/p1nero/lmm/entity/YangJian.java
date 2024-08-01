package com.p1nero.lmm.entity;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

/**
 * 1.平a有刺击和横扫两种，横扫攻击判定是BOSS面前的一个半圆，刺击就是正前方较长的一段距离，刺击的时候播放：attack1,横扫播放attack2,分别有各自的音效
 * 2.走路是飘着的，是一个身体前倾的动画，播放walk
 * 3.技能一是从额头处射出一道激光，锁定一名玩家，每秒造成3点伤害，持续5秒，不需要额外的动画，但播放一个音效
 * 4.技能二是对所有玩家脚下生成一个3X3的判定圈，两秒后在判定圈落下光球这种东西砸向玩家，造成10点伤害，并给予5秒缓慢Ⅰ.播放动画skill1，并伴随音效
 * 5.boss霸体，2000血，每秒持续恢复1点血
 * 6.平a几次后接技能
 */
public class YangJian extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    public YangJian(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier setAttributes() {//生物属性
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2000)//最大血量
                .add(Attributes.ATTACK_SPEED, 0.5f)//攻速
                .add(Attributes.MOVEMENT_SPEED, 0.30f)//移速
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));

        this.goalSelector.addGoal(0, new YangJianAttackGoal(this));
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        bossInfo.removePlayer(player);
    }

    /**
     * 每秒回1滴血
     */
    @Override
    public void tick() {
        super.tick();
        if(tickCount % 20 == 0){
            setHealth(getHealth() + 1);
        }
    }

    /**
     * 戳
     */
    public void doPoke(LivingEntity target){

    }

    /**
     * 横扫
     */
    public void doSweep(LivingEntity target){

    }

    /**
     * 技能1发射激光
     */
    public void racer(){

    }

    /**
     * 技能2下落石头
     */
    public void fallBall(){

    }

    private static class YangJianAttackGoal extends MeleeAttackGoal{
        private YangJian boss;
        private int ticksUntilNextAttack;
        private final int attackRange = 5;
        public Random random = new Random();
        public YangJianAttackGoal(YangJian boss) {
            super(boss, 0.3, true);
            this.boss = boss;
        }


        @Override
        public boolean canUse() {
            return super.canUse();
        }

        @Override
        public void start() {
            super.start();
            this.ticksUntilNextAttack = 0;
        }

        @Override
        public void tick() {
            super.tick();
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        }

        @Override
        protected void resetAttackCooldown() {
            super.resetAttackCooldown();
            this.ticksUntilNextAttack = this.adjustedTickDelay(20);
        }

        @Override
        protected boolean isTimeToAttack() {
            super.isTimeToAttack();
            return this.ticksUntilNextAttack <= 0;
        }

        @Override
        protected int getTicksUntilNextAttack() {
            super.getTicksUntilNextAttack();
            return this.ticksUntilNextAttack;
        }

        @Override
        protected void checkAndPerformAttack(@NotNull LivingEntity entity, double p_25558_) {
            LivingEntity target = boss.getTarget();
            if (target != null && target.distanceTo(boss) < attackRange && ticksUntilNextAttack <= 0) {
                this.resetAttackCooldown();
                if (random.nextBoolean()) {
                    boss.doPoke(target);
                } else {
                    boss.doSweep(target);
                }
            }

        }

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
