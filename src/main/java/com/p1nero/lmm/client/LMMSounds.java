package com.p1nero.lmm.client;

import com.p1nero.lmm.LustrousMoonMobMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class LMMSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LustrousMoonMobMod.MOD_ID);
    public static final RegistryObject<SoundEvent> GROUND_SLAM_SMALL = registerSound("sfx.ground_slam_small");
    public static final RegistryObject<SoundEvent> GROUND_SLAM = registerSound("sfx.ground_slam");
    private static RegistryObject<SoundEvent> registerSound(String name) {
        ResourceLocation res = new ResourceLocation(LustrousMoonMobMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(res));
    }
}
