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
 * Modified (new file) 2026 by DEHunter58711 for the unofficial NeoForge 26.3 port of Diet
 * (original: Copyright (C) 2021-2023 Illusive Soulworks, LGPL-3.0-or-later).
 * - new file: client-only @Mod entry point (key mapping, client events, payload handlers)
 */




package com.illusivesoulworks.diet;

import com.illusivesoulworks.diet.client.DietClientEventsListener;
import com.illusivesoulworks.diet.client.DietKeys;
import com.illusivesoulworks.diet.common.network.DietForgeNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Port: client-only entry point (new file). Replaces the @Mod.EventBusSubscriber client classes
 * of the Forge version (key mapping registration, client game events, packet handlers).
 */
@Mod(value = DietConstants.MOD_ID, dist = Dist.CLIENT)
public class DietClientMod {

  public DietClientMod(IEventBus eventBus) {
    eventBus.addListener(this::registerKeys);
    eventBus.addListener(this::clientSetup);
    eventBus.addListener(DietForgeNetwork::registerClient);
    DietClientEventsListener.register(NeoForge.EVENT_BUS);
  }

  private void clientSetup(final FMLClientSetupEvent evt) {

    // Port QA helper (runClient only, -Ddiet.selfcheck=true): loads every client class so broken
    // 26.3 API references fail loudly even without a working renderer.
    if (Boolean.getBoolean("diet.selfcheck")) {
      String[] classes = {"com.illusivesoulworks.diet.client.screen.DietScreen",
          "com.illusivesoulworks.diet.client.screen.DynamicButton",
          "com.illusivesoulworks.diet.client.screen.DietTooltip",
          "com.illusivesoulworks.diet.client.DietClientEvents",
          "com.illusivesoulworks.diet.client.DietClientPacketReceiver"};

      for (String name : classes) {
        try {
          Class.forName(name, true, DietClientMod.class.getClassLoader());
          DietConstants.LOG.info("[diet selfcheck] {} OK", name);
        } catch (Throwable t) {
          throw new IllegalStateException("diet self-check failed for " + name, t);
        }
      }
      DietConstants.LOG.info("[diet selfcheck] key mapping {} registered in category {}",
          DietKeys.OPEN_GUI.getName(), DietKeys.CATEGORY.id());
      DietConstants.LOG.info("[diet selfcheck] all client classes OK");
    }
  }

  private void registerKeys(final RegisterKeyMappingsEvent evt) {
    evt.registerCategory(DietKeys.CATEGORY);
    evt.register(DietKeys.get());
  }
}
