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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Port: CustomPacketPayload (id diet:suites).
public record SPacketSuites(CompoundTag suites) implements CustomPacketPayload {

  public static final Type<SPacketSuites> TYPE = new Type<>(DietCommonMod.resource("suites"));
  public static final StreamCodec<FriendlyByteBuf, SPacketSuites> STREAM_CODEC =
      StreamCodec.of((buf, msg) -> encode(msg, buf), SPacketSuites::decode);

  public static void encode(SPacketSuites msg, FriendlyByteBuf buf) {
    buf.writeNbt(msg.suites());
  }

  public static SPacketSuites decode(FriendlyByteBuf buf) {
    CompoundTag tag = buf.readNbt();
    return new SPacketSuites(tag == null ? new CompoundTag() : tag);
  }

  public static void handle(SPacketSuites msg) {
    DietClientPacketReceiver.handleSuites(msg.suites());
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
