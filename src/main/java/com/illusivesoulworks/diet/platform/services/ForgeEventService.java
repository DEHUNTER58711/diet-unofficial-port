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

import com.illusivesoulworks.diet.api.DietEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

public class ForgeEventService implements IEventService {

  // Port: NeoForge's post() returns the event; cancellation is queried via isCanceled().
  @Override
  public boolean fireApplyDecayEvent(Player player) {
    return NeoForge.EVENT_BUS.post(new DietEvent.ApplyDecay(player)).isCanceled();
  }

  @Override
  public boolean fireApplyEffectEvent(Player player) {
    return NeoForge.EVENT_BUS.post(new DietEvent.ApplyEffect(player)).isCanceled();
  }

  @Override
  public boolean fireConsumeStackEvent(ItemStack stack, Player player) {
    return NeoForge.EVENT_BUS.post(new DietEvent.ConsumeItemStack(stack, player)).isCanceled();
  }
}
