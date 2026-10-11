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
 * - direct service instances instead of ServiceLoader
 */




package com.illusivesoulworks.diet.platform;

import com.illusivesoulworks.diet.platform.services.ForgeCapabilityService;
import com.illusivesoulworks.diet.platform.services.ForgeEventService;
import com.illusivesoulworks.diet.platform.services.ForgeNetworkService;
import com.illusivesoulworks.diet.platform.services.ForgePlatformService;
import com.illusivesoulworks.diet.platform.services.ForgeRegistryService;
import com.illusivesoulworks.diet.platform.services.ICapabilityService;
import com.illusivesoulworks.diet.platform.services.IEventService;
import com.illusivesoulworks.diet.platform.services.INetworkService;
import com.illusivesoulworks.diet.platform.services.IPlatformService;
import com.illusivesoulworks.diet.platform.services.IRegistryService;

public class Services {

  // Port: the NeoForge-only port has exactly one implementation per service, so the
  // java.util.ServiceLoader lookup (META-INF/services) was replaced by direct instances.
  public static final IPlatformService PLATFORM = new ForgePlatformService();
  public static final ICapabilityService CAPABILITY = new ForgeCapabilityService();
  public static final IRegistryService REGISTRY = new ForgeRegistryService();
  public static final IEventService EVENT = new ForgeEventService();
  public static final INetworkService NETWORK = new ForgeNetworkService();
}
