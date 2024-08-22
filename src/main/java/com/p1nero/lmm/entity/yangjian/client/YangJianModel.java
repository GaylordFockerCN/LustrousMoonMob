package com.p1nero.lmm.entity.yangjian.client;

import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.entity.yangjian.YangJian;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class YangJianModel extends DefaultedEntityGeoModel<YangJian> {
    public YangJianModel() {
        super(new ResourceLocation(LustrousMoonMobMod.MOD_ID, "yang_jian"), true);
    }

    /**
     * 模型用的Head，但是geckolib默认head...
     */
    @Override
    public void setCustomAnimations(YangJian animatable, long instanceId, AnimationState<YangJian> animationState) {
//        CoreGeoBone head = this.getAnimationProcessor().getBone("allhead");
//        if (head != null) {
//            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
//            head.setRotX(entityData.headPitch() * 0.017453292F);
//            head.setRotY(entityData.netHeadYaw() * 0.017453292F);
//        }
    }
}
