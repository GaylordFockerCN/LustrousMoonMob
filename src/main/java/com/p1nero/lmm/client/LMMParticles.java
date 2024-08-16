package com.p1nero.lmm.client;

import com.p1nero.lmm.LustrousMoonMobMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class LMMParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LustrousMoonMobMod.MOD_ID);
    public static final RegistryObject<SimpleParticleType> GROUND_SLAM = PARTICLES.register("ground_slam", () -> new SimpleParticleType(true));

}
