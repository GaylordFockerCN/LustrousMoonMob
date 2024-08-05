package com.p1nero.lmm.entity.yangjian;

import com.p1nero.lmm.entity.LMMEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Racer extends Entity {
    @Nullable
    protected LivingEntity target;
    @Nullable
    protected LivingEntity owner;
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(Racer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(Racer.class, EntityDataSerializers.INT);
    public Racer(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public Racer(Level pLevel, @Nullable LivingEntity target, @Nullable LivingEntity owner) {
        super(LMMEntities.RACER.get(), pLevel);
        this.target = target;
        this.owner = owner;
        if(target != null && owner != null){
            getEntityData().set(TARGET_ID, target.getId());
            getEntityData().set(OWNER_ID, owner.getId());
        }
    }

    public @Nullable LivingEntity getTarget() {
        if(target == null){
            return ((LivingEntity) level().getEntity(getEntityData().get(TARGET_ID)));
        }
        return target;
    }

    public @Nullable LivingEntity getOwner() {
        if(owner == null){
            return ((LivingEntity) level().getEntity(getEntityData().get(OWNER_ID)));
        }
        return owner;
    }

    /**
     * 同步位置到Owner和target之间
     */
    @Override
    public void tick() {
        super.tick();
        //需要在玩家眼里才会渲染出来。。
        if(target != null && owner != null){
            setPos(target.position().add(0,1,0));
        }
        //存活5s
        if(tickCount > 100){
//            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(TARGET_ID, 0);
        getEntityData().define(OWNER_ID, 0);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {

    }
}
