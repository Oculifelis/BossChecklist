package com.yori3o.boss_checklist.impl;


import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;



public class PlatformSoundRegistry {


    // NeoForge freezes registries before mods run, so sounds are queued here and registered in RegisterEvent
    public static final List<SoundEvent> SOUNDS = new ArrayList<>();


    public static void registerSound(SoundEvent soundEvent) {
        SOUNDS.add(soundEvent);
    }

    public static void onRegister(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            for (SoundEvent soundEvent : SOUNDS) {
                helper.register(soundEvent.location(), soundEvent);
            }
        });
    }

}
