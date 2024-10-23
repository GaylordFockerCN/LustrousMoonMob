package com.p1nero.lmm;

import com.mojang.logging.LogUtils;
import com.p1nero.lmm.block.client.FractureBlockRenderer;
import com.p1nero.lmm.block.entity.BlockEntities;
import com.p1nero.lmm.block.Blocks;
import com.p1nero.lmm.client.GroundSlamParticle;
import com.p1nero.lmm.client.LMMParticles;
import com.p1nero.lmm.client.sound.LMMSounds;
import com.p1nero.lmm.entity.LMMEntities;
import com.p1nero.lmm.entity.bride.client.BrideRenderer;
import com.p1nero.lmm.entity.bride.client.ClawRenderer;
import com.p1nero.lmm.entity.yangjian.client.YangJianRenderer;
import com.p1nero.lmm.utils.NetworkManager;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LustrousMoonMobMod.MOD_ID)
public class LustrousMoonMobMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "lustrous_moon_mob";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public LustrousMoonMobMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::doCommonStuff);
        LMMEntities.REGISTRY.register(modEventBus);
        Blocks.BLOCKS.register(modEventBus);
        BlockEntities.BLOCK_ENTITIES.register(modEventBus);
        LMMParticles.PARTICLES.register(modEventBus);
        LMMSounds.SOUNDS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);;
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void doCommonStuff(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkManager::registerPacket);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(LMMEntities.YANG_JIAN.get(), YangJianRenderer::new);
            EntityRenderers.register(LMMEntities.XIAO_TIAN.get(), WolfRenderer::new);
            EntityRenderers.register(LMMEntities.BRIDE.get(), BrideRenderer::new);
            EntityRenderers.register(LMMEntities.CLAW.get(), ClawRenderer::new);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onParticleRegistry(final RegisterParticleProvidersEvent event) {
            event.registerSpecial(LMMParticles.GROUND_SLAM.get(), new GroundSlamParticle.Provider());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void registerRenderersEvent(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BlockEntities.FRACTURE.get(), FractureBlockRenderer::new);
        }
    }
}
