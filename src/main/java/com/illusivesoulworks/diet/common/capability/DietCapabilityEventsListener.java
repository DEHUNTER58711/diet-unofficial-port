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
 * - NeoForge game events, attachment instead of capability
 */




package com.illusivesoulworks.diet.common.capability;

import com.illusivesoulworks.diet.api.type.IDietTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class DietCapabilityEventsListener {

  // Port: registered manually on the NeoForge game bus (was @Mod.EventBusSubscriber); the
  // AttachCapabilitiesEvent is gone, the tracker is created lazily by the attachment type.
  public static void register(IEventBus bus) {
    bus.addListener(DietCapabilityEventsListener::playerClone);
    bus.addListener(DietCapabilityEventsListener::playerRespawned);
    bus.addListener(DietCapabilityEventsListener::playerLoggedIn);
    bus.addListener(DietCapabilityEventsListener::playerDimensionTravel);
    bus.addListener(DietCapabilityEventsListener::playerTick);
    bus.addListener(EventPriority.LOW, DietCapabilityEventsListener::finishItemUse);
  }

  public static void playerClone(final PlayerEvent.Clone evt) {

    if (evt.getEntity() instanceof ServerPlayer player) {
      Player original = evt.getOriginal();
      DietCapability.get(player).ifPresent(tracker -> tracker.copy(original, evt.isWasDeath()));
    }
  }

  public static void playerRespawned(final PlayerEvent.PlayerRespawnEvent evt) {

    if (evt.getEntity() instanceof ServerPlayer player) {
      DietCapability.get(player).ifPresent(IDietTracker::sync);
    }
  }

  public static void playerLoggedIn(final PlayerEvent.PlayerLoggedInEvent evt) {

    if (evt.getEntity() instanceof ServerPlayer player) {
      DietCapability.get(player).ifPresent(IDietTracker::sync);
    }
  }

  public static void playerDimensionTravel(final PlayerEvent.PlayerChangedDimensionEvent evt) {

    if (evt.getEntity() instanceof ServerPlayer player) {
      DietCapability.get(player).ifPresent(IDietTracker::sync);
    }
  }

  public static void playerTick(final PlayerTickEvent.Post evt) {

    if (evt.getEntity() instanceof ServerPlayer player) {
      DietCapability.get(player).ifPresent(IDietTracker::tick);
    }
  }

  public static void finishItemUse(final LivingEntityUseItemEvent.Finish evt) {
    ItemStack stack = evt.getItem();
    LivingEntity livingEntity = evt.getEntity();

    if (!livingEntity.level().isClientSide() && livingEntity instanceof Player player) {

      // Port: food is a data component in 26.3 (was Item#getFoodProperties).
      if (stack.get(DataComponents.FOOD) != null) {
        DietCapability.get(player).ifPresent(diet -> diet.consume(stack));
      }
    }
  }
}
