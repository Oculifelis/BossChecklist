package com.yori3o.boss_checklist.neoforge;


import com.yori3o.boss_checklist.common.BossChecklist;
import com.yori3o.boss_checklist.common.event.EventHandler;
import com.yori3o.boss_checklist.common.server.data.ServerBossIdsLoader;
import com.yori3o.boss_checklist.impl.PlatformNetworkHelper;
import com.yori3o.boss_checklist.impl.PlatformSoundRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;



/**
 * NeoForge entry point. It wires the same events as the Fabric entry point of this version
 * (BossChecklistFabric): after-death, after-damage, player join, server started and the server data reload.
 */
@Mod(BossChecklist.MOD_ID)
public class BossChecklistNeoForge {


    public BossChecklistNeoForge(IEventBus modBus) {
        (new BossChecklist()).init();

        modBus.addListener(PlatformSoundRegistry::onRegister);
        modBus.addListener(BossChecklistNeoForge::onRegisterPayloads);

        // LOWEST and skipping cancelled deaths: like Fabric's AFTER_DEATH, only deaths that really happen count
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BossChecklistNeoForge::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(BossChecklistNeoForge::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(BossChecklistNeoForge::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(BossChecklistNeoForge::onServerStarted);
        NeoForge.EVENT_BUS.addListener(BossChecklistNeoForge::onAddReloadListeners);
    }


    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PlatformNetworkHelper.init(event);
        EventHandler.whenRegisterPayloads();
    }

    // Fabric: ServerLivingEntityEvents.AFTER_DAMAGE passes baseDamageTaken (before armour); getOriginalDamage is the match
    private static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();

        if (!entity.level().isClientSide()) {
            EventHandler.whenEntityAllowDamage(entity, event.getSource(), event.getOriginalDamage());
        }
    }

    // Fabric: ServerLivingEntityEvents.AFTER_DEATH
    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!entity.level().isClientSide() && !event.isCanceled()) {
            EventHandler.whenEntityDeath(entity, event.getSource());
        }
    }

    private static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EventHandler.whenPlayerJoinToServer(player);
        }
    }

    private static void onServerStarted(ServerStartedEvent event) {
        EventHandler.whenServerStarted(event.getServer());
    }

    // reads data/boss_checklist/server_bosses_ids.json from every data pack and mod
    private static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
            Identifier.fromNamespaceAndPath(BossChecklist.MOD_ID, "server_resources_reloader"),
            (ResourceManagerReloadListener) ServerBossIdsLoader::load
        );
    }
}
