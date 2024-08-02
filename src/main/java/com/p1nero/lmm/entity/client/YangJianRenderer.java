package com.p1nero.lmm.entity.client;

import com.p1nero.lmm.entity.yangjian.YangJian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class YangJianRenderer extends GeoEntityRenderer<YangJian> {
    public YangJianRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new YangJianModel());
    }
}
