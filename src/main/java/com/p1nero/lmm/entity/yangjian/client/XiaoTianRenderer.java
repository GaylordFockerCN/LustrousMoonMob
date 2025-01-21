package com.p1nero.lmm.entity.yangjian.client;

import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.entity.yangjian.XiaoTian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class XiaoTianRenderer extends GeoEntityRenderer<XiaoTian> {
    public XiaoTianRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(new ResourceLocation(LustrousMoonMobMod.MOD_ID, "xiaotianquan")));
    }
}
