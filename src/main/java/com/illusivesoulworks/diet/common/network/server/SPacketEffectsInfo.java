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
import com.illusivesoulworks.diet.common.data.effect.DietEffectsInfo;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Port: CustomPacketPayload (id diet:effects_info); payload is still the NBT form of the info.
public record SPacketEffectsInfo(DietEffectsInfo info) implements CustomPacketPayload {

  public static final Type<SPacketEffectsInfo> TYPE =
      new Type<>(DietCommonMod.resource("effects_info"));
  public static final StreamCodec<FriendlyByteBuf, SPacketEffectsInfo> STREAM_CODEC =
      StreamCodec.of((buf, msg) -> encode(msg, buf), SPacketEffectsInfo::decode);

  public static void encode(SPacketEffectsInfo msg, FriendlyByteBuf buf) {
    buf.writeNbt(msg.info.write());
  }

  public static SPacketEffectsInfo decode(FriendlyByteBuf buf) {
    return new SPacketEffectsInfo(DietEffectsInfo.read(Objects.requireNonNull(buf.readNbt())));
  }

  public static void handle(SPacketEffectsInfo msg) {
    DietClientPacketReceiver.handleEffectsInfo(msg.info);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
