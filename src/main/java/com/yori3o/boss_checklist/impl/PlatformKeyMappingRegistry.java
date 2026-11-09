package com.yori3o.boss_checklist.impl;


import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;



public class PlatformKeyMappingRegistry {


    // NeoForge registers key mappings in RegisterKeyMappingsEvent (see BossChecklistNeoForgeClient)
    public static final List<KeyMapping> keyMappings = new ArrayList<>();


    public static void registerKeyMapping(KeyMapping keyMapping) {
        keyMappings.add(keyMapping);
    }

}
