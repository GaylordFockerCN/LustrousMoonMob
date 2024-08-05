package com.p1nero.lmm.entity.yangjian;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

/**
 * 1.平a有刺击和横扫两种，横扫3点戳5点，横扫攻击判定是BOSS面前的一个半圆，刺击就是正前方较长的一段距离，刺击的时候播放：attack1,横扫播放attack2,分别有各自的音效
 * 2.走路是飘着的，是一个身体前倾的动画，播放walk
 * 3.技能一是从额头处射出一道激光，锁定一名玩家，每秒造成3点伤害，持续5秒，不需要额外的动画，但播放一个音效
 * 4.技能二是对所有玩家脚下生成一个3X3的判定圈，两秒后在判定圈落下光球这种东西砸向玩家，造成10点伤害，并给予5秒缓慢Ⅰ.播放动画skill1，并伴随音效
 * 5.boss霸体，单人500血，双人1000，三人1500，四人以上2000
 * 6.平A 1~5次后接技能
 */
public class YangJian extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
    private int explodeTimer;
    private int pokeAttackTimer;
    private int sweepAttackTimer;
    private int basicAttackCount = 0;
    private final Set<Vec3> explodePos = new HashSet<>();
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RACER_TIMER = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.INT);
    public YangJian(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        //根据人数调血量（生成即为确定，不实时检测）
        if(level instanceof ServerLevel serverLevel){
            int playerCnt = Math.min(serverLevel.players().size(), 4);
            Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(500 * playerCnt);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(TARGET_ID, -1);
        getEntityData().define(RACER_TIMER, 0);
    }

    public static AttributeSupplier setAttributes() {//生物属性
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2000)//最大血量
                .add(Attributes.ATTACK_SPEED, 0.5f)//攻速
                .add(Attributes.MOVEMENT_SPEED, 2f)//移速
                .add(Attributes.KNOCKBACK_RESISTANCE, 114514)//抗性
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));

        this.goalSelector.addGoal(0, new RecoverIfNoPlayerGoal(this));
        this.goalSelector.addGoal(1, new YangJianAttackGoal(this));
        this.goalSelector.addGoal(2, new YangJianSkillGoal(this));
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

    /**
     * 因为攻击要延迟，所以几乎都集中在tick判断
     */
    @Override
    public void tick() {
        super.tick();

        if(level() instanceof ServerLevel serverLevel){

            //爆炸判断
            if(!explodePos.isEmpty()){
                if(explodeTimer > 0){
                    explodeTimer--;
                    //生成一圈粒子特效
                    for(Vec3 pos : explodePos){
                        double radius = 3;
                        int particlesCount = 200;
                        double angleIncrement = 2 * Math.PI / particlesCount;
                        for (int i = 0; i < particlesCount; i++) {
                            double angle = i * angleIncrement;
                            double offsetX = radius * Math.cos(angle);
                            double offsetZ = radius * Math.sin(angle);
                            double posX = pos.x + offsetX;
                            double posZ = pos.z + offsetZ;
                            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, posX, pos.y, posZ, 1, 0, 0.1, 0, 0.01);
                        }
                    }
                }else {
                    explodeTimer = 40;
                    for(Vec3 pos : explodePos){
                        level().explode(this, this.damageSources().mobAttack(this), null, pos, 3F, false, Level.ExplosionInteraction.NONE);
                    }
                    explodePos.clear();
                }
            }

        }

        int racerTimer = getEntityData().get(RACER_TIMER);
        if(racerTimer > 0){
            getEntityData().set(RACER_TIMER, racerTimer - 1);
        }

        //戳判断
        if(pokeAttackTimer > 0 && pokeAttackTimer < 114514){
            pokeAttackTimer--;
        }else if(pokeAttackTimer < 114514){
            List<Player> players = getNearByPlayers(6);
            for(Player player : players){
                if(Math.abs(player.getY() - this.getY()) >= 4){
                    continue;
                }
                Vec3 targetToBoss = player.position().subtract(this.position());
                Vec2 targetToBossV2 = new Vec2(((float) targetToBoss.x), ((float) targetToBoss.z));
                Vec3 view = this.getViewVector(1.0F);
                Vec2 viewV2 = new Vec2(((float) view.x), ((float) view.z));
                double angleRadians = Math.acos(targetToBossV2.dot(viewV2)/(targetToBossV2.length() * viewV2.length()));
                double degree = Math.toDegrees(angleRadians);
                if(Math.abs(degree) <= 7 && player.distanceTo(this) <= 4.2F){
                    player.hurt(this.damageSources().mobAttack(this), 10.0F);
                }
            }
            pokeAttackTimer = 114514;
        }
        //横扫攻击判断
        if(sweepAttackTimer > 0 && sweepAttackTimer < 114514){
            sweepAttackTimer--;
        }else if(sweepAttackTimer < 114514){
            List<Player> players = getNearByPlayers(6);
            for(Player player : players){
                if(Math.abs(player.getY() - this.getY()) >= 4){
                    continue;
                }
                Vec3 targetToBoss = player.position().subtract(this.position());
                Vec2 targetToBossV2 = new Vec2(((float) targetToBoss.x), ((float) targetToBoss.z));
                Vec3 view = this.getViewVector(1.0F);
                Vec2 viewV2 = new Vec2(((float) view.x), ((float) view.z));
                double angleRadians = Math.acos(targetToBossV2.dot(viewV2)/(targetToBossV2.length() * viewV2.length()));
                double degree = Math.toDegrees(angleRadians);
                if(Math.abs(degree) <= 90 && player.distanceTo(this) <= 3.2F){
                    player.hurt(this.damageSources().mobAttack(this), 6.0F);
                }
            }
            sweepAttackTimer = 114514;
        }


    }

    /**
     * 戳
     */
    public void doPoke(LivingEntity target){
        this.lookControl.setLookAt(target);
        triggerAnim("BasicAttack", "poke");
        pokeAttackTimer = 10;
    }

    /**
     * 横扫
     */
    public void doSweep(LivingEntity target){
        this.lookControl.setLookAt(target);
        triggerAnim("BasicAttack", "sweep");
        sweepAttackTimer = 10;
    }

    /**
     * 技能1发射激光
     */
    public void racer(){
        List<Player> players = getNearByPlayers(64);
        if(players.isEmpty()){
            return;
        }
        Player target;
        if(getTarget() instanceof Player player && players.contains(player)){
            target = player;
        } else {
            target = players.iterator().next();
        }
        this.lookControl.setLookAt(target);
        getEntityData().set(TARGET_ID, target.getId());
        getEntityData().set(RACER_TIMER, 100);
    }

    public int getRacerTimer() {
        return getEntityData().get(RACER_TIMER);
    }

    @Nullable
    public Entity getRacerTarget(){
        return level().getEntity(getEntityData().get(TARGET_ID));
    }

    /**
     * 技能2范围爆炸，判断在tick里
     */
    public void preExplode(int size){
//        triggerAnim("Skill", "explode");
        List<Player> players = getNearByPlayers(size);
        for(Player player : players){
            explodePos.add(player.getPosition(1.0F));
        }
    }

    public List<Player> getNearByPlayers(int dis){
        BlockPos myPos = this.getOnPos();
        return level().getNearbyPlayers(TargetingConditions.DEFAULT, this, new AABB(myPos.offset(-dis, -dis, -dis), myPos.offset(dis, dis, dis)));
    }

    private static class RecoverIfNoPlayerGoal extends Goal {
        private final YangJian boss;
        public RecoverIfNoPlayerGoal(YangJian boss){
            this.boss = boss;
        }
        @Override
        public boolean canUse() {
            return boss.getNearByPlayers(64).isEmpty();
        }

        @Override
        public void start() {
            boss.setHealth(boss.getMaxHealth());
        }
    }

    private static class YangJianSkillGoal extends Goal{

        private final YangJian boss;

        private int count;

        private YangJianSkillGoal(YangJian boss){
            this.boss = boss;
            count = boss.random.nextInt(2,5);
        }

        @Override
        public boolean canUse() {
            return boss.basicAttackCount >= count;
        }

        @Override
        public void start() {
            count = boss.random.nextInt(1,5);
            boss.basicAttackCount = 0;
            if(boss.random.nextBoolean()){
                boss.preExplode(64);
            }else {
                boss.racer();
            }
        }

    }

    private static class YangJianAttackGoal extends MeleeAttackGoal{
        private final YangJian boss;
        private int ticksUntilNextAttack;
        private final int attackRange = 5;
        private YangJianAttackGoal(YangJian boss) {
            super(boss, 0.3, true);
            this.boss = boss;
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
                boss.lookControl.setLookAt(target);
                boss.basicAttackCount++;
                if (boss.random.nextBoolean()) {
                    boss.doPoke(target);
                } else {
                    boss.doSweep(target);
                }
            }

        }

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller",
                10,tAnimationState -> {
            if(tAnimationState.isMoving()) {
                tAnimationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            }
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
            return PlayState.STOP;
        }));
        controllers.add(new AnimationController<>(this, "BasicAttack", 10, state -> PlayState.STOP)
                .triggerableAnim("poke", RawAnimation.begin().then("poke", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("sweep", RawAnimation.begin().then("sweep", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "Skill", 10, state -> PlayState.STOP)
                .triggerableAnim("racer", RawAnimation.begin().then("racer", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("explode", RawAnimation.begin().then("explode", Animation.LoopType.PLAY_ONCE)));

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
        return super.getHurtSound(source);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return super.getDeathSound();
    }

}
