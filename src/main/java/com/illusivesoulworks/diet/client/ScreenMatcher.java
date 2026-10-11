/*
 * Unofficial NeoForge 26.3 port of Diet - new file (replaces CuriosIntegration).
 * Copyright (C) 2026 DEHunter58711. Based on Diet, Copyright (C) 2021-2023 Illusive Soulworks.
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

package com.illusivesoulworks.diet.client;

import com.illusivesoulworks.diet.common.config.DietConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;

/**
 * Port feature (#194): decides by class / interface name (config buttonScreens) on which screens
 * the Diet button is shown, so Curios (whose screen class moved in Curios 9+/17) and other
 * inventory mods work without compile-time dependencies.
 */
public final class ScreenMatcher {

  private ScreenMatcher() {
  }

  public static boolean matches(Screen screen) {
    Set<String> names = new HashSet<>();

    for (String s : DietConfig.CLIENT.buttonScreens.get()) {
      names.add(s.trim());
    }

    for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass()) {

      if (names.contains(c.getName()) || matchesInterfaces(c, names)) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesInterfaces(Class<?> c, Set<String> names) {

    for (Class<?> i : c.getInterfaces()) {

      if (names.contains(i.getName()) || matchesInterfaces(i, names)) {
        return true;
      }
    }
    return false;
  }
}
