package com.p1nero.lmm.entity.bride.client;

import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.entity.bride.Bride;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BrideRenderer extends GeoEntityRenderer<Bride> {
    public BrideRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(new ResourceLocation(LustrousMoonMobMod.MOD_ID, "ghost_bride")));
    }
}
