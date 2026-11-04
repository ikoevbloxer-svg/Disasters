package com.vladdisasters.registry;

import com.vladdisasters.VladDisasters;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, VladDisasters.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MAMA_SCREAM = reg("mama_scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAMA_ROAR = reg("mama_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAMA_EAT = reg("mama_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAMA_HURT = reg("mama_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAMA_DEATH = reg("mama_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREAMER = reg("screamer");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN = reg("siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM = reg("alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUKE_BOOM = reg("nuke_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUAKE_RUMBLE = reg("quake_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> TORNADO_WIND = reg("tornado_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACKHOLE_HUM = reg("blackhole_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> METEOR_WHOOSH = reg("meteor_whoosh");

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(VladDisasters.id(name)));
    }

    private ModSounds() {}
}
