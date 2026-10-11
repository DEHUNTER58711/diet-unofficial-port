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
 * - Minecraft 26.3 / NeoForge API migration (ResourceLocation -> Identifier, Optional NBT
 *   getters, registry holders)
 */




package com.illusivesoulworks.diet.common.network.server;

import com.illusivesoulworks.diet.DietCommonMod;
import com.illusivesoulworks.diet.client.DietClientPacketReceiver;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Port: SimpleChannel message -> CustomPacketPayload (id diet:activate) with a StreamCodec.
public record SPacketActivate(boolean flag) implements CustomPacketPayload {

  public static final Type<SPacketActivate> TYPE = new Type<>(DietCommonMod.resource("activate"));
  public static final StreamCodec<FriendlyByteBuf, SPacketActivate> STREAM_CODEC =
      StreamCodec.of((buf, msg) -> encode(msg, buf), SPacketActivate::decode);

  public static void encode(SPacketActivate msg, FriendlyByteBuf buf) {
    buf.writeBoolean(msg.flag);
  }

  public static SPacketActivate decode(FriendlyByteBuf buf) {
    return new SPacketActivate(buf.readBoolean());
  }

  public static void handle(SPacketActivate msg) {
    DietClientPacketReceiver.handleActivate(msg);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
