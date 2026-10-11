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
 * - direct service instance instead of ServiceLoader
 */




package com.illusivesoulworks.diet.platform;

import com.illusivesoulworks.diet.platform.services.ForgeClientService;
import com.illusivesoulworks.diet.platform.services.IClientService;

public class ClientServices {

  // Port: direct instance instead of java.util.ServiceLoader (see Services).
  public static final IClientService INSTANCE = new ForgeClientService();
}
