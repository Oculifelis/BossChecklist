package com.yori3o.boss_checklist.neoforge;


import com.yori3o.boss_checklist.common.BossChecklist;
import com.yori3o.boss_checklist.common.client.data.BossRegistry;
import com.yori3o.boss_checklist.common.event.EventHandler;
import com.yori3o.boss_checklist.impl.PlatformKeyMappingRegistry;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;



/**
 * Client entry point, the NeoForge side of BossChecklistFabricClient.
 */
@Mod(value = BossChecklist.MOD_ID, dist = Dist.CLIENT)
public class BossChecklistNeoForgeClient {


    public BossChecklistNeoForgeClient(IEventBus modBus) {
        modBus.addListener(BossChecklistNeoForgeClient::onRegisterKeyMappings);
        modBus.addListener(BossChecklistNeoForgeClient::onAddClientReloadListeners);

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> EventHandler.whenJoinClient());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> EventHandler.whenDisconnectClient());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> EventHandler.whenClientTickStart());
    }


    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping keyMapping : PlatformKeyMappingRegistry.keyMappings) {
            event.register(keyMapping);
        }
    }

    // reads assets/boss_checklist/bosses.json from every resource pack and mod
    private static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(
            Identifier.fromNamespaceAndPath(BossChecklist.MOD_ID, "client_resources_reloader"),
            (ResourceManagerReloadListener) resourceManager -> BossRegistry.reload()
        );
    }
}
