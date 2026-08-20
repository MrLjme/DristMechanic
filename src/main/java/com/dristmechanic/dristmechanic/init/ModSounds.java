package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Dristmechanic.MODID);

    public static final Supplier<SoundEvent> FARMBO_AMBIENT = register("farmbot_ambient");
    public static final Supplier<SoundEvent> FARMBO_HURT = register("farmbot_hurt");
    public static final Supplier<SoundEvent> FARMBO_DEATH = register("farmbot_death");

    public static final Supplier<SoundEvent> HAYBOT_AMBIENT = register("haybot_ambient");
    public static final Supplier<SoundEvent> HAYBOT_HURT = register("haybot_hurt");
    public static final Supplier<SoundEvent> HAYBOT_DEATH = register("haybot_death");

    public static final Supplier<SoundEvent> TAPEBOT_AMBIENT = register("tapebot_ambient");
    public static final Supplier<SoundEvent> TAPEBOT_HURT = register("tapebot_hurt");
    public static final Supplier<SoundEvent> TAPEBOT_DEATH = register("tapebot_death");
    public static final Supplier<SoundEvent> TAPEBOT_SHOOT = register("tapebot_shoot");

    public static final Supplier<SoundEvent> TOTEBOT_AMBIENT = register("totebot_ambient");
    public static final Supplier<SoundEvent> TOTEBOT_HURT = register("totebot_hurt");
    public static final Supplier<SoundEvent> TOTEBOT_DEATH = register("totebot_death");

    public static final Supplier<SoundEvent> TAPE_HIT = register("tape_hit");

    private static Supplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () ->
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, name)));
    }
}