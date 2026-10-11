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
 * - Curios screen detected by class name (no compile dependency)
 */




package com.illusivesoulworks.diet.common.integration;

import net.minecraft.client.gui.screens.Screen;

public class CuriosIntegration {

  // Port: compared by class name so the port has no compile-time dependency on Curios
  // (Curios for 26.3 is only available as a beta). Same class name as Curios 5.x - 17.x.
  private static final String CURIOS_SCREEN = "top.theillusivec4.curios.client.gui.CuriosScreen";

  public static boolean isCuriosScreen(Screen screen) {

    for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass()) {

      if (c.getName().equals(CURIOS_SCREEN)) {
        return true;
      }
    }
    return false;
  }
}
