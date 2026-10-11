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
 * - Identifier-based key category
 */




package com.illusivesoulworks.diet.client;

import com.illusivesoulworks.diet.DietConstants;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class DietKeys {

  // Port: 26.3 key categories are Identifier-based (translation key key.category.diet.diet).
  public static final KeyMapping.Category CATEGORY =
      new KeyMapping.Category(Identifier.fromNamespaceAndPath(DietConstants.MOD_ID, DietConstants.MOD_ID));

  public static final KeyMapping OPEN_GUI =
      new KeyMapping("key.diet.open.desc", InputConstants.UNKNOWN.getValue(), CATEGORY);

  public static KeyMapping get() {
    return OPEN_GUI;
  }
}
