package com.p1nero.lmm.entity.yangjian;

import com.p1nero.lmm.client.sound.BossMusicPlayer;
import com.p1nero.lmm.client.sound.LMMSounds;
import com.p1nero.lmm.entity.LMMEntities;
import com.p1nero.lmm.utils.LevelUtil;
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
import net.minecraft.world.entity.ai.goal.*;
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
    private int lineTimer = 0;
    private boolean canSayLine;
    private int explodeTimer;
    private int attackTimer;
    //记录上个技能，使不会连续放同一个技能（避免bug）
    private int lastSkill = 0;
    private final int explodeDelay = 50;
    private final Set<Vec3> explodePos = new HashSet<>();
    private final Queue<Vec3> playerPos = new ArrayDeque<>();
    private static final EntityDataAccessor<Boolean> CAN_USE_RACER = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> XIAO_TIAN_ID = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RACER_TIMER = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> TARGET_DIR = SynchedEntityData.defineId(YangJian.class, EntityDataSerializers.VECTOR3);
    public YangJian(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        //根据人数调血量（生成即为确定，不实时检测）
        if(level instanceof ServerLevel serverLevel){
            int playerCnt = Math.min(serverLevel.players().size(), 4);
            playerCnt = Math.max(1, playerCnt-1);
            Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).addPermanentModifier(new AttributeModifier("player_cnt", (500 * playerCnt), AttributeModifier.Operation.ADDITION));
            setHealth(getMaxHealth());
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(CAN_USE_RACER, true);
        getEntityData().define(XIAO_TIAN_ID, -1);
        getEntityData().define(TARGET_ID, -1);
        getEntityData().define(RACER_TIMER, 0);
        getEntityData().define(TARGET_DIR, new Vector3f());
    }

    public static AttributeSupplier setAttributes() {//生物属性
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500)//最大血量
                .add(Attributes.MOVEMENT_SPEED, 0.3f)//移速
                .add(Attributes.KNOCKBACK_RESISTANCE, 114514)//抗性
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));

        this.goalSelector.addGoal(0, new RecoverIfNoPlayerGoal(this));
        this.goalSelector.addGoal(1, new YangJianSkillGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        bossInfo.addPlayer(player);
        canSayLine = true;
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

    private void sayToNear(Component component){
        for(Player player : getNearByPlayers(32)){
            player.displayClientMessage(component, true);
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
        if(canSayLine){
//            lineTimer++;
            if(lineTimer == 5){
                level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.LINE1.get(), SoundSource.BLOCKS, 1, 1);
            }
            if(lineTimer >=4 && lineTimer <=100){
                sayToNear(Component.translatable("line1.lustrous_moon_mob.yang_jian"));
            }
            if(lineTimer == 155){
                level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.LINE2.get(), SoundSource.BLOCKS, 1, 1);
            }
            if(lineTimer >=164 && lineTimer <=230){
                sayToNear(Component.translatable("line2.lustrous_moon_mob.yang_jian"));
            }
            if(lineTimer == 285){
                level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.LINE3.get(), SoundSource.BLOCKS, 1, 1);

            }
            if(lineTimer >= 284 && lineTimer <=355){
                sayToNear(Component.translatable("line3.lustrous_moon_mob.yang_jian"));
            }
            if(lineTimer == 385){
                canSayLine = false;
            }
        }

        if(level() instanceof ServerLevel serverLevel){

            //开场白判断

            //普攻判断
            if(attackTimer > 0){
//                Vector3f vector3f = getEntityData().get(TARGET_DIR);
//                this.setYRot((float) Math.toDegrees(Math.atan2(vector3f.x, vector3f.z)));
//                this.setYHeadRot(getYRot());
                //延迟播动画，先把朝向调整对了再播
                if(attackTimer == 94){
                    triggerAnim("Skill", "attack");
                }
                //戳
                if(attackTimer == 80 || attackTimer == 60){
                    level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.ATTACK.get(), SoundSource.BLOCKS, 1, 1);
                    List<Player> players = getNearByPlayers(6);
                    for(Player player : players){
                        if(Math.abs(player.getY() - this.getY()) >= 4){
                            continue;
                        }
                        if(Math.abs(getDegree(player)) <= 35 && player.distanceTo(this) <= 5.2F){
                            player.hurt(this.damageSources().mobAttack(this), 18.0F);
                        }
                    }
                }
                //横扫
                if(attackTimer == 49 || attackTimer == 42){
                    level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.ATTACK.get(), SoundSource.BLOCKS, 1, 1);
                    List<Player> players = getNearByPlayers(6);
                    for(Player player : players){
                        if(Math.abs(player.getY() - this.getY()) >= 4){
                            continue;
                        }
                        if(player.distanceTo(this) <= 4.0F){
                            player.hurt(this.damageSources().mobAttack(this), 15.0F);
                        }
                    }
                }

                if(attackTimer == 18){
                    LevelUtil.circleSlamFracture(this, level(), this.getOnPos().getCenter(), 5);
                    level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.BREAK.get(), SoundSource.BLOCKS, 2, 1);
                }

            }

            //爆炸判断
            if(!explodePos.isEmpty()){
                if(explodeTimer > 0){
                    explodeTimer--;
                    this.setPos(getPosition(1.0f));
                    //延迟播放音效，动作先做一会儿
                    if(explodeTimer == 40){
                        level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.SKILL.get(), SoundSource.BLOCKS, 2, 1);
                    }
                    if(explodeTimer <= 40){
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

        //手动位移，原版的navigation搞不明白
        if(attackTimer > 0){
            attackTimer--;
            Vector3f vector3f = getEntityData().get(TARGET_DIR);
            Vec3 vec3 = new Vec3(vector3f.x, 0, vector3f.z);
            if(level().isClientSide){
//                this.setYRot((float) Math.toDegrees(Math.atan2(vector3f.x, vector3f.z)));
//                this.setYHeadRot(getYRot());
            }
            this.getNavigation().stop();
//            this.setDeltaMovement(vec3.normalize().scale(0.13));
//            Vec3 dir = new Vec3(Math.cos(getYRot()), 0, Math.sin(getYRot()));
//            this.setDeltaMovement(dir.normalize().scale(0.13));
            this.setYRot(getViewYRot(1.0F));
            this.setDeltaMovement(getViewVector(1.0F).normalize().scale(0.13));
        } else {
            //追击目标
            if(getTarget() != null && explodePos.isEmpty()){
                this.getNavigation().moveTo(getTarget(), 1.0F);
            }

        }

        int racerTimer = getEntityData().get(RACER_TIMER);
        if(racerTimer > 0){
            Entity entity = level().getEntity(getEntityData().get(TARGET_ID));
            if(entity instanceof Player player){
                playerPos.add(player.position());

                if(isRacerTargetInFront() && canRenderRacer()){
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
        int delay = 3;
        if(playerPos.size() > delay){
            playerPos.poll();
        }

    }

    /**
     * 戳
     */
    public void doAttack(){
        LivingEntity target = getTarget();
        if(target == null || target.distanceTo(this) > 5){
            return;
        }
        if(attackTimer > 0){
            return;
        }
        Vec3 dir = target.position().subtract(this.position());
        Vec3 targetPos = this.position().add(dir.normalize().scale(9));
        getEntityData().set(TARGET_DIR, dir.toVector3f());
        attackTimer = 95;
    }

    /**
     * 技能1发射激光
     */
    public void racer(){
        level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.LIGHT.get(), SoundSource.BLOCKS, 1, 1);
        Player target = level().getNearestPlayer(this, 64);
        if(target == null){
            return;
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
            return Math.abs(getDegree(entity)) <= 25;
        }
        return false;
    }

    public boolean canRenderRacer(){
        if(!level().isClientSide){
            getEntityData().set(CAN_USE_RACER, attackTimer <= 0 && explodePos.isEmpty());
        }
        return getEntityData().get(CAN_USE_RACER);
    }

    /**
     * 判断目标是否在boss前方
     * 攻击的时候也不能放激光不然会跑后边
     */
    public boolean isRacerTargetInFront(Vec3 target){
        return Math.abs(getDegree(target)) <= 25 && attackTimer <= 0;
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
//        level().playSound(null, this.getX(), this.getY(), this.getZ(), LMMSounds.SKILL.get(), SoundSource.BLOCKS, 1, 1);//延迟点播放比较合理
        List<Player> players = getNearByPlayers(size);
        for(Player player : players){
            explodePos.add(player.getPosition(1.0F));
        }
        this.getNavigation().stop();
        explodeTimer = explodeDelay;
    }

    /**
     * 召唤哮天犬
     * 血+30
     * 改名
     */
    public void summonXiaoTian(){
        if(level() instanceof ServerLevel serverLevel && !hasXiaoTian(serverLevel)){
            XiaoTian wolf = LMMEntities.XIAO_TIAN.get().spawn(serverLevel, this.getOnPos().offset(0,2,0), MobSpawnType.MOB_SUMMONED);
            if(wolf != null){
                wolf.setCustomName(Component.translatable("entity.lustrous_moon_mob.xiao_tian"));
                wolf.setCustomNameVisible(true);
                wolf.getPersistentData().putBoolean("fromYangJian", true);
                wolf.setTarget(this.getTarget());
                wolf.setOwner(this.getId());
                getEntityData().set(XIAO_TIAN_ID, wolf.getId());
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
        //技能释放间隔的tick
        private final int maxCount = 30;

        private YangJianSkillGoal(YangJian boss){
            this.boss = boss;
            count = maxCount;
        }

        @Override
        public boolean canUse() {
            return count-- < 0 && boss.attackTimer <= 0 && boss.explodePos.isEmpty();
        }

        @Override
        public void start() {
            count = maxCount;
            int i;
            if(boss.getTarget() != null && boss.distanceTo(boss.getTarget()) < 3){
                i = 2;
            }else {
                i =  boss.random.nextInt(boss.hasXiaoTian(((ServerLevel) boss.level())) ? 3 : 4);
            }
            if(i == boss.lastSkill){
                i+=1;
                if(i > 3){
                    i = 0;
                }
            }
            boss.lastSkill = i;
            switch (i){
                case 0 -> boss.preExplode(64);
                case 1 -> boss.racer();
                case 2 -> boss.doAttack();
                default -> boss.summonXiaoTian();
            }
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
        controllers.add(new AnimationController<>(this, "Skill", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("explode", RawAnimation.begin().then("explode", Animation.LoopType.PLAY_ONCE)));

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
        return super.getHurtSound(source);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return super.getDeathSound();
    }

}
