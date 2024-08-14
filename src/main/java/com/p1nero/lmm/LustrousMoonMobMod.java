package com.p1nero.lmm;

import com.mojang.logging.LogUtils;
import com.p1nero.lmm.entity.LMMEntities;
import com.p1nero.lmm.entity.yangjian.client.YangJianRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LustrousMoonMobMod.MOD_ID)
public class LustrousMoonMobMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "lustrous_moon_mob";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public LustrousMoonMobMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        LMMEntities.REGISTRY.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(LMMEntities.YANG_JIAN.get(), YangJianRenderer::new);
            EntityRenderers.register(LMMEntities.XIAO_TIAN.get(), WolfRenderer::new);
        }
    }
}
