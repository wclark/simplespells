package com.github.wclark.simplespells;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SimpleSpellsNetwork {
    private SimpleSpellsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CastStaffPayload.TYPE, CastStaffPayload.STREAM_CODEC, CastStaffPayload::handle);
    }
}
