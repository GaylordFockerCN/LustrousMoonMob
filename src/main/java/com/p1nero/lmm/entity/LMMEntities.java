package com.p1nero.lmm.entity;

import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.entity.bride.Bride;
import com.p1nero.lmm.entity.bride.ClawEntity;
import com.p1nero.lmm.entity.yangjian.XiaoTian;
import com.p1nero.lmm.entity.yangjian.YangJian;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
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
            EntityType.Builder.of(YangJian::new, MobCategory.MONSTER), 1, 2.6F);
    public static final RegistryObject<EntityType<XiaoTian>> XIAO_TIAN = register("xiao_tian",
            EntityType.Builder.of(XiaoTian::new, MobCategory.MONSTER), 0.6F, 0.8F);
    public static final RegistryObject<EntityType<Bride>> BRIDE = register("bride",
            EntityType.Builder.of(Bride::new, MobCategory.MONSTER), 0.8F, 3.0F);
    public static final RegistryObject<EntityType<ClawEntity>> CLAW = register("claw",
            EntityType.Builder.of(ClawEntity::new, MobCategory.MONSTER), 1, 2.6F);

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> entityTypeBuilder, float sizeXZ, float sizeY) {
        return REGISTRY.register(name, () -> entityTypeBuilder.sized(sizeXZ, sizeY).build(new ResourceLocation(LustrousMoonMobMod.MOD_ID, name).toString()));
    }

    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(YANG_JIAN.get(), YangJian.setAttributes());
        event.put(XIAO_TIAN.get(), XiaoTian.createAttributes().build());
        event.put(BRIDE.get(), Bride.setAttributes());
        event.put(CLAW.get(), ClawEntity.setAttributes());
    }
    @SubscribeEvent
    public static void entitySpawnRestriction(SpawnPlacementRegisterEvent event) {
        event.register(YANG_JIAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                YangJian::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(XIAO_TIAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                XiaoTian::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(BRIDE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Bride::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(CLAW.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ClawEntity::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
