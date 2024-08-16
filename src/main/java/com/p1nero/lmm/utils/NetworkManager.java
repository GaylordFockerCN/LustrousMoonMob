package com.p1nero.lmm.utils;

import com.p1nero.lmm.LustrousMoonMobMod;
import com.p1nero.lmm.network.SPFracture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(LustrousMoonMobMod.MOD_ID, "network_manager"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    public static <MSG> void sendToAllPlayerTrackingThisChunkWithSelf(MSG message, LevelChunk chunk) {
        sendToClient(message, PacketDistributor.TRACKING_CHUNK.with(() -> chunk));
    }

    public static <MSG> void sendToClient(MSG message, PacketDistributor.PacketTarget packetTarget) {
        INSTANCE.send(packetTarget, message);
    }

    public static void registerPacket(){
        int id = 0;
        INSTANCE.registerMessage(id++, SPFracture.class, SPFracture::toBytes, SPFracture::fromBytes, SPFracture::handle);
    }
}
