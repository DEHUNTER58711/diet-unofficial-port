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
 * - NeoForge client game events, registered manually
 */




package com.illusivesoulworks.diet.client;

import com.illusivesoulworks.diet.client.screen.DynamicButton;
import com.illusivesoulworks.diet.common.integration.CuriosIntegration;
import com.illusivesoulworks.diet.common.integration.IntegrationManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Port: registered from DietClientMod on the NeoForge game bus (was @Mod.EventBusSubscriber).
public class DietClientEventsListener {

  public static void register(IEventBus bus) {
    bus.addListener(DietClientEventsListener::initGui);
    bus.addListener(DietClientEventsListener::tick);
    bus.addListener(DietClientEventsListener::tooltip);
  }

  public static void initGui(final ScreenEvent.Init.Post evt) {
    Screen screen = evt.getScreen();

    if (screen instanceof InventoryScreen ||
        (IntegrationManager.isCuriosLoaded() && CuriosIntegration.isCuriosScreen(screen))) {
      DynamicButton button = DietClientEvents.getButton(screen);

      if (button != null) {
        evt.addListener(button);
      }
    }
  }

  public static void tick(final ClientTickEvent.Post evt) {
    DietClientEvents.tick(Minecraft.getInstance());
  }

  public static void tooltip(final ItemTooltipEvent evt) {
    Player player = evt.getEntity();
    List<Component> tooltips = evt.getToolTip();
    ItemStack stack = evt.getItemStack();
    DietClientEvents.renderItemTooltip(player, stack, tooltips);
  }
}
