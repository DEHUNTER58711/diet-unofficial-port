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

import com.illusivesoulworks.diet.client.DietClientPacketReceiver;
import com.illusivesoulworks.diet.platform.Services;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

// Port: CustomPacketPayload (id diet:groups).
public record SPacketGroups(CompoundTag groups, Map<Item, Set<String>> generated)
    implements CustomPacketPayload {

  public static final Type<SPacketGroups> TYPE =
      new Type<>(com.illusivesoulworks.diet.DietCommonMod.resource("groups"));
  public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, SPacketGroups>
      STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, msg) -> encode(msg, buf),
      SPacketGroups::decode);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }


  public static void encode(SPacketGroups msg, FriendlyByteBuf buf) {
    CompoundTag compoundNBT = new CompoundTag();

    for (Map.Entry<Item, Set<String>> entry : msg.generated().entrySet()) {
      ListTag listNBT = new ListTag();

      for (String s : entry.getValue()) {
        listNBT.add(StringTag.valueOf(s));
      }
      compoundNBT.put(
          Objects.requireNonNull(Services.REGISTRY.getItemKey(entry.getKey())).toString(), listNBT);
    }
    buf.writeNbt(compoundNBT);
    buf.writeNbt(msg.groups());
  }

  public static SPacketGroups decode(FriendlyByteBuf buf) {
    CompoundTag compoundNBT = buf.readNbt();
    Map<Item, Set<String>> generated = new HashMap<>();

    if (compoundNBT != null) {

      for (String name : compoundNBT.keySet()) {
        Item item = Services.REGISTRY.getItem(Identifier.parse(name)).orElse(null);

        if (item != null) {
          ListTag listNBT = compoundNBT.getListOrEmpty(name);
          Set<String> found = new HashSet<>();

          for (Tag nbt : listNBT) {
            String entry = nbt.asString().orElse("");
            found.add(entry);
          }
          generated.put(item, found);
        }
      }
    }
    CompoundTag groups = buf.readNbt();
    return new SPacketGroups(groups == null ? new CompoundTag() : groups, generated);
  }

  public static void handle(SPacketGroups msg) {
    DietClientPacketReceiver.handleGroups(msg.groups(), msg.generated());
  }
}
