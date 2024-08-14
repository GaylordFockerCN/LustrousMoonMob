package com.p1nero.lmm.entity;

import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.entity.yangjian.XiaoTian;
import com.p1nero.lmm.entity.yangjian.YangJian;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = LustrousMoonMobMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LMMEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LustrousMoonMobMod.MOD_ID);
    public static final RegistryObject<EntityType<YangJian>> YANG_JIAN = register("yang_jian",
            EntityType.Builder.of(YangJian::new, MobCategory.MONSTER), 1, 3);
    public static final RegistryObject<EntityType<XiaoTian>> XIAO_TIAN = register("xiao_tian",
            EntityType.Builder.of(XiaoTian::new, MobCategory.MONSTER), 0.5F, 0.5F);

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> entityTypeBuilder, float sizeXZ, float sizeY) {
        return REGISTRY.register(name, () -> entityTypeBuilder.sized(sizeXZ, sizeY).build(new ResourceLocation(LustrousMoonMobMod.MOD_ID, name).toString()));
    }

    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(YANG_JIAN.get(), YangJian.setAttributes());
        event.put(XIAO_TIAN.get(), XiaoTian.createAttributes().build());
    }
    @SubscribeEvent
    public static void entitySpawnRestriction(SpawnPlacementRegisterEvent event) {
        event.register(YANG_JIAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                YangJian::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(XIAO_TIAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                XiaoTian::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
