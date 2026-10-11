/*
 * Copyright (C) 2021-2023 Illusive Soulworks
 *
 * Diet is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Diet is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Diet.  If not, see <https://www.gnu.org/licenses/>.
 */
/*
 * Modified 2026 by DEHunter58711 for the unofficial NeoForge 26.3 port of Diet
 * (original: Copyright (C) 2021-2023 Illusive Soulworks, LGPL-3.0-or-later).
 * - SimpleChannel -> NeoForge CustomPacketPayload registration
 */




package com.illusivesoulworks.diet.common.network;

import com.illusivesoulworks.diet.common.network.server.SPacketActivate;
import com.illusivesoulworks.diet.common.network.server.SPacketDiet;
import com.illusivesoulworks.diet.common.network.server.SPacketEaten;
import com.illusivesoulworks.diet.common.network.server.SPacketEffectsInfo;
import com.illusivesoulworks.diet.common.network.server.SPacketGroups;
import com.illusivesoulworks.diet.common.network.server.SPacketSuites;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Port: Forge SimpleChannel "diet:main" was replaced by six NeoForge play payloads
 * (server -> client only, protocol version "1"). Payloads are required on both sides, like the
 * strict version check of the original channel. Client handlers are registered separately via
 * RegisterClientPayloadHandlersEvent so that the dedicated server never touches client classes.
 */
public class DietForgeNetwork {

  private static final String PTC_VERSION = "1";

  public static void register(final RegisterPayloadHandlersEvent evt) {
    PayloadRegistrar registrar = evt.registrar(PTC_VERSION);
    registrar.playToClient(SPacketDiet.TYPE, SPacketDiet.STREAM_CODEC);
    registrar.playToClient(SPacketEffectsInfo.TYPE, SPacketEffectsInfo.STREAM_CODEC);
    registrar.playToClient(SPacketActivate.TYPE, SPacketActivate.STREAM_CODEC);
    registrar.playToClient(SPacketEaten.TYPE, SPacketEaten.STREAM_CODEC);
    registrar.playToClient(SPacketGroups.TYPE, SPacketGroups.STREAM_CODEC);
    registrar.playToClient(SPacketSuites.TYPE, SPacketSuites.STREAM_CODEC);
  }

  public static void registerClient(final RegisterClientPayloadHandlersEvent evt) {
    // Default handler thread is the main (client) thread, like enqueueWork in the original.
    evt.register(SPacketDiet.TYPE, (msg, ctx) -> SPacketDiet.handle(msg));
    evt.register(SPacketEffectsInfo.TYPE, (msg, ctx) -> SPacketEffectsInfo.handle(msg));
    evt.register(SPacketActivate.TYPE, (msg, ctx) -> SPacketActivate.handle(msg));
    evt.register(SPacketEaten.TYPE, (msg, ctx) -> SPacketEaten.handle(msg));
    evt.register(SPacketGroups.TYPE, (msg, ctx) -> SPacketGroups.handle(msg));
    evt.register(SPacketSuites.TYPE, (msg, ctx) -> SPacketSuites.handle(msg));
  }
}
