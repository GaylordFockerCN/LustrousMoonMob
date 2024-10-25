package com.p1nero.lmm.entity.bride;

import com.p1nero.lmm.client.sound.BossMusicPlayer;
import com.p1nero.lmm.client.sound.LMMSounds;
import com.p1nero.lmm.entity.LMMMob;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

/**
 * 近战，召唤控制爪
 */
public class Bride extends LMMMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private int clawTimer = 0;
    private int attackTimer;
    public Bride(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        //根据人数调血量（生成即为确定，不实时检测）
        if(level instanceof ServerLevel serverLevel){
            int playerCnt = Math.min(serverLevel.players().size(), 4);
            playerCnt = Math.max(0, playerCnt-1);
            Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).addPermanentModifier(new AttributeModifier("player_cnt", (100 * playerCnt), AttributeModifier.Operation.ADDITION));
            setHealth(getMaxHealth());
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }


    /**
     * 生物属性
     */
    public static AttributeSupplier setAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200)//最大血量
                .add(Attributes.MOVEMENT_SPEED, 0.3f)//移速
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));

        this.goalSelector.addGoal(0, new RecoverIfNoPlayerGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new SummonClawGoal(this, 100));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        bossInfo.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        if (!this.level().isClientSide()) {
            this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    /**
     * 因为攻击要延迟，所以几乎都集中在tick判断
     */
    @Override
    public void tick() {
        //控制bgm播放
        if (!level().isClientSide) {
            if (!isSilent()) {
                this.level().broadcastEntityEvent(this, MUSIC_PLAY_ID);
            }
            else {
                this.level().broadcastEntityEvent(this, MUSIC_STOP_ID);
            }
        }
        super.tick();
        if(level() instanceof ServerLevel && isAlive()){
            if(attackTimer > 0){
                attackTimer--;
                if(attackTimer == 1){
                    List<Player> players = getNearByPlayers(2);
                    for(Player player : players){
                        if(getDegree(player) < 30){
                            player.hurt(this.damageSources().mobAttack(this), 12);
                        }
                    }
                }
            }

            if(clawTimer > 0){
                clawTimer--;
                if(clawTimer == 1){
                    List<Player> players = getNearByPlayers(64);
                    for (Player target : players) {
                        ClawEntity claw = new ClawEntity(this.level(), this, target);
                        claw.setPos(target.getX(), target.getY(), target.getZ());
                        level().addFreshEntity(claw);//树爪继承自Mob，和平模式无法召唤！！
                        claw.catchPlayer();
                    }
                }
            }

        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity entity) {
        if(attackTimer <= 0 && clawTimer <= 0){
            attackTimer = 30;
            triggerAnim("Attack", "attack");
        }
        return false;
    }

    private static class SummonClawGoal extends Goal{

        private final Bride boss;

        private int count;
        //技能释放间隔的tick
        private final int maxCount;

        private SummonClawGoal(Bride boss, int maxCount){
            this.boss = boss;
            this.maxCount = count = maxCount;
        }

        @Override
        public boolean canUse() {
            return count-- < 0 && boss.attackTimer <= 0 && boss.clawTimer <= 0;
        }

        @Override
        public void start() {
            count = maxCount;
            boss.clawTimer = 40;
            boss.triggerAnim("Attack", "summon");
        }

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller",
                0,tAnimationState -> {
            if(tAnimationState.isMoving()) {
                tAnimationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            }
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "Attack", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("summon", RawAnimation.begin().then("skill", Animation.LoopType.PLAY_ONCE)));

    }


    private static final byte MUSIC_PLAY_ID = 67;
    private static final byte MUSIC_STOP_ID = 68;

    @Override
    public void handleEntityEvent(byte id) {
        if (id == MUSIC_PLAY_ID ) BossMusicPlayer.playBossMusic(this, LMMSounds.BGM.get(), 32);
        else if (id == MUSIC_STOP_ID) BossMusicPlayer.stopBossMusic(this);
        else super.handleEntityEvent(id);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return super.getAmbientSound();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return LMMSounds.BRIDE_HURT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return super.getDeathSound();
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }
}
