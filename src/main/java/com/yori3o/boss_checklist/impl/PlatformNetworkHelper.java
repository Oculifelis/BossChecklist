package com.yori3o.boss_checklist.impl;


import com.yori3o.boss_checklist.common.BossChecklist;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.BiConsumer;



public class PlatformNetworkHelper {

    // the event is kept while EventHandler.whenRegisterPayloads() runs inside it
    private static RegisterPayloadHandlersEvent registrationEvent;

    public static void init(RegisterPayloadHandlersEvent event) {
        registrationEvent = event;
    }

    private static RegisterPayloadHandlersEvent event() {
        if (registrationEvent == null) {
            throw new IllegalStateException("PlatformNetworkHelper.init() must be called before registration!");
        }
        return registrationEvent;
    }

    private static PayloadContext wrap(IPayloadContext context) {
        return new PayloadContext() {
            @Override
            public void enqueue(Runnable runnable) {
                context.enqueueWork(runnable);
            }

            @Override
            public Player getPlayer() {
                return context.player();
            }
        };
    }

    // optional(): a client or server without the mod can still connect, as on Fabric
    public static <T extends CustomPacketPayload> void registerS2C(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, PayloadContext> handler
    ) {
        event().registrar(BossChecklist.MOD_ID)
            .optional()
            .playToClient(type, codec, (payload, context) -> handler.accept(payload, wrap(context)));
    }

    public static <T extends CustomPacketPayload> void registerC2S(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, PayloadContext> handler
    ) {
        event().registrar(BossChecklist.MOD_ID)
            .optional()
            .playToServer(type, codec, (payload, context) -> handler.accept(payload, wrap(context)));
    }

    public static <T extends CustomPacketPayload> void sendToPlayer(ServerPlayer player, T payload) {
        // skip players whose client lacks the mod, which the optional channel lets join
        if (player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static <T extends CustomPacketPayload> void sendToServer(T payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    public interface PayloadContext {
        void enqueue(Runnable runnable);
        Player getPlayer();
    }
}
