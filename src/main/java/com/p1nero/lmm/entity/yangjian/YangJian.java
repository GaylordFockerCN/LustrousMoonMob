package com.p1nero.lmm.entity.yangjian;

import com.p1nero.lmm.client.LMMSounds;
import com.p1nero.lmm.entity.LMMEntities;
import com.p1nero.lmm.utils.LevelUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
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
    private int attackTimer;
    private int basicAttackCount = 0;
    private final int explodeDelay = 40;
    private final Set<Vec3> explodePos = new HashSet<>();
    private final Queue<Vec3> playerPos = new ArrayDeque<>();
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> XIAO_TIAN_ID = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RACER_TIMER = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> TARGET_DIR = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.VECTOR3);
    public YangJian(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        //根据人数调血量（生成即为确定，不实时检测）
        if(level instanceof ServerLevel serverLevel){
            int playerCnt = Math.min(serverLevel.players().size(), 4);
            Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).addPermanentModifier(new AttributeModifier("player_cnt", (500 * playerCnt - 1), AttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(XIAO_TIAN_ID, -1);
        getEntityData().define(TARGET_ID, -1);
        getEntityData().define(RACER_TIMER, 0);
        getEntityData().define(TARGET_DIR, new Vector3f());
    }

    public static AttributeSupplier setAttributes() {//生物属性
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500)//最大血量
                .add(Attributes.ATTACK_SPEED, 1.5f)//攻速
                .add(Attributes.MOVEMENT_SPEED, 0.2f)//移速
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
        super.tick();

        if(level() instanceof ServerLevel serverLevel){

            //普攻判断
            if(attackTimer > 0){
                if(attackTimer == 94){
                    triggerAnim("Skill", "attack");
                }
                if(attackTimer == 83 || attackTimer == 63){
                    level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.ATTACK.get(), SoundSource.BLOCKS, 1, 1);
                    List<Player> players = getNearByPlayers(6);
                    for(Player player : players){
                        if(Math.abs(player.getY() - this.getY()) >= 4){
                            continue;
                        }
                        if(Math.abs(getDegree(player)) <= 7 && player.distanceTo(this) <= 3.2F){
                            player.hurt(this.damageSources().mobAttack(this), 10.0F);
                        }
                    }
                }
                if(attackTimer == 49 || attackTimer == 42){
                    level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.ATTACK.get(), SoundSource.BLOCKS, 1, 1);
                    List<Player> players = getNearByPlayers(6);
                    for(Player player : players){
                        if(Math.abs(player.getY() - this.getY()) >= 4){
                            continue;
                        }
                        if(player.distanceTo(this) <= 2.5F){
                            player.hurt(this.damageSources().mobAttack(this), 8.0F);
                        }
                    }
                }

                if(attackTimer == 18){
                    LevelUtil.circleSlamFracture(this, level(), this.getOnPos().getCenter(), 3);
                    level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.BREAK.get(), SoundSource.BLOCKS, 2, 1);
                }

            }

            //爆炸判断
            if(!explodePos.isEmpty()){
                if(explodeTimer > 0){
//                    explodeTimer--;
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
                            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, posX, pos.y, posZ, 1, 0, 0.02, 0, 0.01);
                        }
                    }
                }else {
                    explodeTimer = explodeDelay;
                    for(Vec3 pos : explodePos){
                        DamageSource damageSource = this.damageSources().explosion(this, this);
                        level().explode(this, damageSource, null, pos, 3F, false, Level.ExplosionInteraction.NONE);
                    }
                    explodePos.clear();
                }
            }

        }

        //爆炸的时候也不能动
        if(explodeTimer >0){
            explodeTimer--;
//            this.getNavigation().stop();
            this.setPos(getPosition(1.0f));
        }

        //手动位移，原版的navigation搞不明白
        if(attackTimer > 0){
            attackTimer--;
            Vector3f vector3f = getEntityData().get(TARGET_DIR);
            Vec3 vec3 = new Vec3(vector3f.x, 0, vector3f.z);
//            this.getNavigation().moveTo(vector3f.x, vector3f.y, vector3f.z, 1);
            this.getNavigation().stop();
            this.getLookControl().setLookAt(vec3);
            this.setDeltaMovement(vec3.normalize().scale(0.2));
        }

        int racerTimer = getEntityData().get(RACER_TIMER);
        if(racerTimer > 0){
            Entity entity = level().getEntity(getEntityData().get(TARGET_ID));
            if(entity instanceof Player player){
                playerPos.add(player.position());

                if(isRacerTargetInFront()){
                    if(playerPos.peek() != null && playerPos.peek().distanceTo(player.position()) < 0.5){
//                    player.hurt(damageSources().mobAttack(this), 0.15f);//会受到霸体影响
                        player.setHealth(player.getHealth() - 0.3F);
                    }
                }
            }
            getEntityData().set(RACER_TIMER, racerTimer - 1);
        } else {
            playerPos.clear();
        }

        //射线的延迟时间，提供delay个tick前的玩家位置
        int delay = 5;
        if(playerPos.size() > delay){
            playerPos.poll();
        }

    }

    /**
     * 戳
     */
    public void doAttack(@NotNull LivingEntity target){
        if(attackTimer > 0){
            return;
        }
        this.lookControl.setLookAt(target);
        Vec3 dir = target.getPosition(1.0f).subtract(this.getPosition(1.0f));
        Vec3 targetPos = this.getPosition(1.0f).add(dir.normalize().scale(9));
        this.getNavigation().stop();
//        this.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1);
        getEntityData().set(TARGET_DIR, dir.toVector3f());
        attackTimer = 95;
    }

    /**
     * 技能1发射激光
     */
    public void racer(){
        level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.LIGHT.get(), SoundSource.BLOCKS, 1, 1);
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
        getEntityData().set(RACER_TIMER, 110);
    }

    public int getRacerTimer() {
        return getEntityData().get(RACER_TIMER);
    }

    @Nullable
    public Vec3 getDelayPlayerPos(){
        return playerPos.peek();
    }

    @Nullable
    public Entity getRacerTarget(){
        return level().getEntity(getEntityData().get(TARGET_ID));
    }

    /**
     * 判断目标是否在boss前方
     */
    public boolean isRacerTargetInFront(){
        Entity entity = getRacerTarget();
        if(entity != null){
            return Math.abs(getDegree(entity)) <= 70 && attackTimer <= 0;
        }
        return false;
    }

    /**
     * 判断目标是否在boss前方
     * 攻击的时候也不能放激光不然会跑后边
     */
    public boolean isRacerTargetInFront(Vec3 target){
        return Math.abs(getDegree(target)) <= 70 && attackTimer <= 0;
    }

    /**
     * 获取视线和位置连线的夹角
     */
    public double getDegree(Entity entity){
        return getDegree(entity.position());
    }

    /**
     * 获取视线和位置连线的夹角
     */
    public double getDegree(Vec3 entity){
        Vec3 targetToBoss = entity.subtract(this.position());
        Vec2 targetToBossV2 = new Vec2(((float) targetToBoss.x), ((float) targetToBoss.z));
        Vec3 view = this.getViewVector(1.0F);
        Vec2 viewV2 = new Vec2(((float) view.x), ((float) view.z));
        double angleRadians = Math.acos(targetToBossV2.dot(viewV2)/(targetToBossV2.length() * viewV2.length()));
        return Math.toDegrees(angleRadians);
    }

    /**
     * 技能2范围爆炸，判断在tick里
     */
    public void preExplode(int size){
        triggerAnim("Skill", "explode");
        level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.SKILL.get(), SoundSource.BLOCKS, 1, 1);
        List<Player> players = getNearByPlayers(size);
        for(Player player : players){
            explodePos.add(player.getPosition(1.0F));
        }
        explodeTimer = explodeDelay;
    }

    /**
     * 召唤哮天犬
     * 血+30
     * 改名
     */
    public void summonXiaoTian(){
//        triggerAnim("Summon", "summon");
        if(level() instanceof ServerLevel serverLevel && !hasXiaoTian(serverLevel)){
//            Wolf wolf = EntityType.WOLF.spawn(serverLevel, this.getOnPos().offset(0,0,0), MobSpawnType.MOB_SUMMONED);
            XiaoTian wolf = LMMEntities.XIAO_TIAN.get().spawn(serverLevel, this.getOnPos().offset(0,2,0), MobSpawnType.MOB_SUMMONED);
            if(wolf != null){
                wolf.setCustomName(Component.translatable("entity.lustrous_moon_mob.xiao_tian"));
                wolf.setCustomNameVisible(true);
                wolf.getPersistentData().putBoolean("fromYangJian", true);
                wolf.setTarget(this.getTarget());
//                wolf.setTargetId(Objects.requireNonNull(getTarget()).getUUID());
//                wolf.setPersistentAngerTarget(Objects.requireNonNull(getTarget()).getUUID());//好像没用
                getEntityData().set(XIAO_TIAN_ID, wolf.getId());
//                Objects.requireNonNull(wolf.getAttribute(Attributes.MAX_HEALTH))
//                        .addPermanentModifier(new AttributeModifier("xiao_tian", 30, AttributeModifier.Operation.ADDITION));//好像没成功
            }
        }
    }

    public boolean hasXiaoTian(ServerLevel serverLevel){
        return serverLevel.getEntity(getEntityData().get(XIAO_TIAN_ID)) instanceof XiaoTian;
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
//            count = boss.random.nextInt(2,5);
            count = 2;
        }

        @Override
        public boolean canUse() {
            return boss.basicAttackCount >= count && boss.attackTimer <= 0;
        }

        @Override
        public void start() {
//            count = boss.random.nextInt(1,5);
            count = 2;
            boss.basicAttackCount = 0;
            int i =  boss.random.nextInt(boss.hasXiaoTian(((ServerLevel) boss.level())) ? 3 : 4);
            switch (i){
                case 0 -> boss.preExplode(64);
                case 1 -> boss.racer();
                case 2 -> boss.doAttack(Objects.requireNonNull(boss.getTarget()));
                default -> boss.summonXiaoTian();
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

        /**
         * boss攻击的时候不能移动
         */
        @Override
        public boolean canUse() {
            return boss.attackTimer <= 0 && super.canUse();
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
        controllers.add(new AnimationController<>(this, "Skill", 10, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE))
//                .triggerableAnim("racer", RawAnimation.begin().then("racer", Animation.LoopType.PLAY_ONCE))
//                .triggerableAnim("summon", RawAnimation.begin().then("summon", Animation.LoopType.PLAY_ONCE))
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
