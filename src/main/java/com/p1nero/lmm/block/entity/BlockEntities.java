package com.p1nero.lmm.block.entity;

import com.google.common.collect.ImmutableSet;
import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.block.Blocks;
import net.minecraft.Util;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LustrousMoonMobMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<FractureBlockEntity>> FRACTURE = BLOCK_ENTITIES.register("fracture_block", () ->
            new UniversalBlockEntityType<FractureBlockEntity>(FractureBlockEntity::new, ImmutableSet.of(Blocks.FRACTURE.get()), Util.fetchChoiceType(References.BLOCK_ENTITY, "fracture_block")));
}
