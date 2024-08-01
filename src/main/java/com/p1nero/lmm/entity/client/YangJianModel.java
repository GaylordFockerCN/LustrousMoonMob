package com.p1nero.lmm.entity.client;

import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.entity.YangJian;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class YangJianModel extends DefaultedEntityGeoModel<YangJian> {
    public YangJianModel() {
        super(new ResourceLocation(LustrousMoonMobMod.MOD_ID, "yang_jian"), true);
    }

}
