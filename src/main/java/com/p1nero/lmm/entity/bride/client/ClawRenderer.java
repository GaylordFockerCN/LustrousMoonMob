package com.p1nero.lmm.entity.bride.client;

import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.entity.bride.ClawEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ClawRenderer extends GeoEntityRenderer<ClawEntity> {
    public ClawRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(new ResourceLocation(LustrousMoonMobMod.MOD_ID, "claw")));
    }

}
