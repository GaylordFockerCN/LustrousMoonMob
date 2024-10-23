package com.p1nero.lmm.entity;

import com.p1nero.lmm.entity.bride.Bride;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LMMMob extends PathfinderMob {
    protected LMMMob(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
        super(p_21683_, p_21684_);
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

    public List<Player> getNearByPlayers(int dis){
        BlockPos myPos = this.getOnPos();
        return level().getNearbyPlayers(TargetingConditions.DEFAULT, this, new AABB(myPos.offset(-dis, -dis, -dis), myPos.offset(dis, dis, dis)));
    }

    protected static class RecoverIfNoPlayerGoal extends Goal {
        private final Bride boss;
        public RecoverIfNoPlayerGoal(Bride boss){
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


}
