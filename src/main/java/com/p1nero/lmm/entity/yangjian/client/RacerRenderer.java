package com.p1nero.lmm.entity.yangjian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.p1nero.lmm.entity.yangjian.Racer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RacerRenderer extends EntityRenderer<Racer> {

    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation("textures/entity/beacon_beam.png");
    public RacerRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(@NotNull Racer pEntity, float pEntityYaw, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {
        LivingEntity target = pEntity.getTarget();
        LivingEntity owner = pEntity.getOwner();
        if(target == null || owner == null){
            return;
        }
        Vec3 eye = owner.getEyePosition();
        Vec3 targetVec = target.position();
        double dis = targetVec.distanceTo(eye);
        Vec3 targetToEye = targetVec.subtract(eye);

        double YRot =(float) Math.toDegrees(Math.atan2(targetToEye.x, targetToEye.z));
        double pitchRad = Math.toRadians(YRot);
        double yawRad = Math.toRadians((float) Math.toDegrees(Math.atan2(targetToEye.y, targetToEye.horizontalDistance())));
        float xRot = (float) -Math.toDegrees(yawRad * Math.cos(pitchRad));
        float zRot = (float) Math.toDegrees(yawRad * Math.sin(pitchRad));
        if(0 < -YRot && -YRot <180){
            zRot = -zRot;
        }
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        pPoseStack.mulPose(Axis.YP.rotationDegrees(((float) YRot) - 90));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
        renderBeaconBeam(pPoseStack, pBuffer, pPartialTick, owner.level().getGameTime(), - dis / 2, dis, new float[]{5,5,5});
        pPoseStack.popPose();
    }


    private static void renderBeaconBeam(PoseStack pPoseStack, MultiBufferSource pBufferSource, float pPartialTick, long pGameTime, double pYOffset, double pHeight, float[] pColors) {
        renderBeaconBeam(pPoseStack, pBufferSource, BEAM_LOCATION, pPartialTick, 1.0F, pGameTime, pYOffset, pHeight, pColors, 0.2F, 0.25F);
    }

    public static void renderBeaconBeam(PoseStack pPoseStack, MultiBufferSource pBufferSource, ResourceLocation pBeamLocation, float pPartialTick, float pTextureScale, long pGameTime, double pYOffset, double pHeight, float[] pColors, float pBeamRadius, float pGlowRadius) {
        double $$11 = pYOffset + pHeight;
        pPoseStack.pushPose();
        pPoseStack.translate(0.5, 0.0, 0.5);
        float $$12 = (float)Math.floorMod(pGameTime, 40) + pPartialTick;
        float $$13 = pHeight < 0 ? $$12 : -$$12;
        float $$14 = Mth.frac($$13 * 0.2F - (float)Mth.floor($$13 * 0.1F));
        float $$15 = pColors[0];
        float $$16 = pColors[1];
        float $$17 = pColors[2];
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.YP.rotationDegrees($$12 * 2.25F - 45.0F));//自转
        float $$30 = 0.0F;
        float $$33 = 0.0F;
        float $$34 = -pBeamRadius;
        float $$23 = 0.0F;
        float $$24 = 0.0F;
        float $$25 = -pBeamRadius;
        float $$38 = 0.0F;
        float $$39 = 1.0F;
        float $$40 = -1.0F + $$14;
        float $$41 = (float)pHeight * pTextureScale * (0.5F / pBeamRadius) + $$40;
        renderPart(pPoseStack, pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, false)), $$15, $$16, $$17, 1.0F, pYOffset, $$11, 0.0F, pBeamRadius, pBeamRadius, 0.0F, $$34, 0.0F, 0.0F, $$25, 0.0F, 1.0F, $$41, $$40);
        pPoseStack.popPose();
        $$30 = -pGlowRadius;
        float $$31 = -pGlowRadius;
        $$33 = -pGlowRadius;
        $$34 = -pGlowRadius;
        $$38 = 0.0F;
        $$39 = 1.0F;
        $$40 = -1.0F + $$14;
        $$41 = (float)pHeight * pTextureScale + $$40;
        renderPart(pPoseStack, pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, true)), $$15, $$16, $$17, 0.125F, pYOffset, $$11, $$30, $$31, pGlowRadius, $$33, $$34, pGlowRadius, pGlowRadius, pGlowRadius, 0.0F, 1.0F, $$41, $$40);
        pPoseStack.popPose();
    }

    private static void renderPart(PoseStack pPoseStack, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha, double pMinY, double pMaxY, float pX0, float pZ0, float pX1, float pZ1, float pX2, float pZ2, float pX3, float pZ3, float pMinU, float pMaxU, float pMinV, float pMaxV) {
        PoseStack.Pose $$20 = pPoseStack.last();
        Matrix4f $$21 = $$20.pose();
        Matrix3f $$22 = $$20.normal();
        renderQuad($$21, $$22, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxY, pX0, pZ0, pX1, pZ1, pMinU, pMaxU, pMinV, pMaxV);
        renderQuad($$21, $$22, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxY, pX3, pZ3, pX2, pZ2, pMinU, pMaxU, pMinV, pMaxV);
        renderQuad($$21, $$22, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxY, pX1, pZ1, pX3, pZ3, pMinU, pMaxU, pMinV, pMaxV);
        renderQuad($$21, $$22, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxY, pX2, pZ2, pX0, pZ0, pMinU, pMaxU, pMinV, pMaxV);
    }

    private static void renderQuad(Matrix4f pPose, Matrix3f pNormal, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha, double pMinY, double pMaxY, float pMinX, float pMinZ, float pMaxX, float pMaxZ, float pMinU, float pMaxU, float pMinV, float pMaxV) {
        addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, pMaxY, pMinX, pMinZ, pMaxU, pMinV);
        addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMinX, pMinZ, pMaxU, pMaxV);
        addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxX, pMaxZ, pMinU, pMaxV);
        addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, pMaxY, pMaxX, pMaxZ, pMinU, pMinV);
    }

    private static void addVertex(Matrix4f pPose, Matrix3f pNormal, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha, double pY, float pX, float pZ, float pU, float pV) {
        pConsumer.vertex(pPose, pX, (float)pY, pZ).color(pRed, pGreen, pBlue, pAlpha).uv(pU, pV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pNormal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Racer racer) {
        return BEAM_LOCATION;
    }
}
