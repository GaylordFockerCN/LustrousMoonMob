package com.p1nero.lmm.block;

import com.p1nero.lmm.LustrousMoonMobMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Blocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LustrousMoonMobMod.MOD_ID);

    public static final RegistryObject<FractureBlock> FRACTURE = BLOCKS.register("fracture_block", () -> new FractureBlock(BlockBehaviour.Properties.of()));
}
