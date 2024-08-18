package com.p1nero.lmm.data.sound;

import com.p1nero.lmm.client.LMMSounds;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

public class SoundGenerator extends TCRSoundProvider {

    public SoundGenerator(PackOutput output, ExistingFileHelper helper) {
        super(output, helper);
    }

    @Override
    public void registerSounds() {
        this.generateNewSoundWithSubtitle(LMMSounds.GROUND_SLAM, "sfx/ground_slam", 1);
        this.generateNewSoundWithSubtitle(LMMSounds.GROUND_SLAM_SMALL, "sfx/ground_slam_small", 1);
        this.generateNewSoundWithSubtitle(LMMSounds.BREAK, "break", 1);
        this.generateNewSoundWithSubtitle(LMMSounds.LIGHT, "light", 1);
        this.generateNewSoundWithSubtitle(LMMSounds.SKILL, "skill", 1);
        this.generateNewSoundWithSubtitle(LMMSounds.ATTACK, "attack", 4);
    }
}
