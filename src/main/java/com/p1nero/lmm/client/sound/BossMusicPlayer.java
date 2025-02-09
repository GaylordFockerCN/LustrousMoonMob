package com.p1nero.lmm.client.sound;

import com.p1nero.lmm.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class BossMusicPlayer {

    private static BossMusic music;

    public static void playBossMusic(LivingEntity entity, SoundEvent bgm, float dis) {
        if (!Config.PLAY_BGM.get()) return;

        if (bgm != null && entity.isAlive()) {
            Player player = Minecraft.getInstance().player;
            if (music != null) {
//                if (Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC) <= 0) {
//                    music = null;
//                } else
                    if (music.boss == entity && entity.distanceTo(player) > dis) {
                    music.boss = null;
                } else if (music.boss == null && music.soundEvent == bgm) {
                    music.boss = entity;
                }
            } else {
                if (entity.distanceTo(player) <= dis) {
                    music = new BossMusic(bgm, entity, entity.getRandom());
                } else {
                    music = null;
                }
            }

            if (music != null && !Minecraft.getInstance().getSoundManager().isActive(music)) {
                Minecraft.getInstance().getSoundManager().play(music);
            }
        }
    }

    public static void stopBossMusic(LivingEntity entity) {
        if (music != null && music.boss == entity) {
            music.boss = null;
        }
    }
    private static class BossMusic extends AbstractTickableSoundInstance {
        public LivingEntity boss;
        private int ticksExisted = 0;
        public final SoundEvent soundEvent;

        public BossMusic(SoundEvent bgm, LivingEntity boss, RandomSource random) {
            super(bgm, SoundSource.RECORDS, random);
            this.boss = boss;
            this.soundEvent = bgm;
            this.attenuation = Attenuation.NONE;
            this.looping = true;
            this.delay = 0;
            this.volume = 4.5F;
            this.x = boss.getX();
            this.y = boss.getY();
            this.z = boss.getZ();
        }

        public boolean canPlaySound() {
            return BossMusicPlayer.music == this;
        }

        public void tick() {
            if (boss == null || !boss.isAlive() || boss.isSilent()) {
                boss = null;
                if (volume >= 0) {
                    volume -= 0.03F;
                } else {
                    BossMusicPlayer.music = null;
                }
            }

            if (ticksExisted % 100 == 0) {
                Minecraft.getInstance().getMusicManager().stopPlaying();
            }
            ticksExisted++;
        }
    }
}
