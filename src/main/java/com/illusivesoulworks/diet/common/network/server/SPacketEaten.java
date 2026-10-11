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
import com.illusivesoulworks.diet.platform.Services;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

// Port: CustomPacketPayload (id diet:eaten); entries are now length-prefixed instead of
// "read until the buffer is empty".
public record SPacketEaten(Set<Item> items) implements CustomPacketPayload {

  public static final Type<SPacketEaten> TYPE = new Type<>(DietCommonMod.resource("eaten"));
  public static final StreamCodec<FriendlyByteBuf, SPacketEaten> STREAM_CODEC =
      StreamCodec.of((buf, msg) -> encode(msg, buf), SPacketEaten::decode);

  public static void encode(SPacketEaten msg, FriendlyByteBuf buf) {
    List<Identifier> ids = new ArrayList<>();

    for (Item item : msg.items) {
      Identifier rl = Services.REGISTRY.getItemKey(item);

      if (rl != null) {
        ids.add(rl);
      }
    }
    buf.writeVarInt(ids.size());

    for (Identifier id : ids) {
      buf.writeIdentifier(id);
    }
  }

  public static SPacketEaten decode(FriendlyByteBuf buf) {
    Set<Item> items = new HashSet<>();
    int size = buf.readVarInt();

    for (int i = 0; i < size; i++) {
      Identifier rl = buf.readIdentifier();
      Services.REGISTRY.getItem(rl).ifPresent(items::add);
    }
    return new SPacketEaten(items);
  }

  public static void handle(SPacketEaten msg) {
    DietClientPacketReceiver.handleEaten(msg);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
