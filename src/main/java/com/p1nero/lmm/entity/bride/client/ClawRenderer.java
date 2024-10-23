package com.p1nero.lmm.entity.bride.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.entity.bride.ClawEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ClawRenderer extends GeoEntityRenderer<ClawEntity> {
    public ClawRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(new ResourceLocation(LustrousMoonMobMod.MOD_ID, "claw")));
    }

    @Override
    public void render(@NotNull ClawEntity entity, float entityYaw, float partialTick, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.scale(4f, 4f, 4f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

}
