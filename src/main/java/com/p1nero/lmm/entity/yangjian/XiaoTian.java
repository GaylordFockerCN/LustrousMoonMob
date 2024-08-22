package com.p1nero.lmm.entity.yangjian;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class XiaoTian extends Wolf {

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(XiaoTian.class, EntityDataSerializers.INT);
    public XiaoTian(EntityType<? extends Wolf> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(OWNER_ID, -1);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if(pSource.getEntity() instanceof Player){
            return super.hurt(pSource, pAmount);
        }
        return false;
    }

    @Override
    protected int calculateFallDamage(float pFallDistance, float pDamageMultiplier) {
        return 0;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("owner", getEntityData().get(OWNER_ID));

    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        getEntityData().set(OWNER_ID, pCompound.getInt("owner"));
    }

    public void setOwner(int id){
        getEntityData().set(OWNER_ID, id);
    }

    /**
     * 用setPersistentTarget没用
     * 直接搜最近的玩家咬
     */
    @Override
    public void tick() {
        super.tick();
        setTarget(level().getNearestPlayer(this, 64));
        if(!(level().getEntity(getEntityData().get(OWNER_ID)) instanceof YangJian)){
//            this.discard();
            this.setHealth(0);
        }
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public void setTame(boolean pTamed) {
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player pPlayer, @NotNull InteractionHand pHand) {
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return Component.translatable("entity.lustrous_moon_mob.xiao_tian");
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }
}
