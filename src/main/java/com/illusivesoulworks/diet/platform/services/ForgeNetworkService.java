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




package com.illusivesoulworks.diet.platform.services;

import com.illusivesoulworks.diet.common.data.effect.DietEffectsInfo;
import com.illusivesoulworks.diet.common.network.server.SPacketActivate;
import com.illusivesoulworks.diet.common.network.server.SPacketDiet;
import com.illusivesoulworks.diet.common.network.server.SPacketEaten;
import com.illusivesoulworks.diet.common.network.server.SPacketEffectsInfo;
import com.illusivesoulworks.diet.common.network.server.SPacketGroups;
import com.illusivesoulworks.diet.common.network.server.SPacketSuites;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

public class ForgeNetworkService implements INetworkService {

  private static void send(ServerPlayer player, CustomPacketPayload payload) {

    // Port: skip players whose connection does not know Diet's payloads (e.g. fake players).
    if (player.connection != null && player.connection.hasChannel(payload)) {
      PacketDistributor.sendToPlayer(player, payload);
    }
  }

  @Override
  public void sendEffectsInfoS2C(ServerPlayer player, DietEffectsInfo info) {
    send(player, new SPacketEffectsInfo(info));
  }

  @Override
  public void sendDietS2C(ServerPlayer player, String suite, Map<String, Float> groups) {
    send(player, new SPacketDiet(suite, groups));
  }

  @Override
  public void sendActivationS2C(ServerPlayer player, boolean flag) {
    send(player, new SPacketActivate(flag));
  }

  @Override
  public void sendEatenS2C(ServerPlayer player, Set<Item> items) {
    send(player, new SPacketEaten(items));
  }

  @Override
  public void sendDietGroupsS2C(ServerPlayer player, CompoundTag groups,
                                Map<Item, Set<String>> generated) {
    send(player, new SPacketGroups(groups, generated));
  }

  @Override
  public void sendDietSuitesS2C(ServerPlayer player, CompoundTag suites) {
    send(player, new SPacketSuites(suites));
  }
}
