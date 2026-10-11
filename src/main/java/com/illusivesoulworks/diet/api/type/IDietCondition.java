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

package com.illusivesoulworks.diet.api.type;

import com.illusivesoulworks.diet.common.data.effect.DietEffect;
import java.util.Map;
import net.minecraft.world.entity.player.Player;

public interface IDietCondition {

  DietEffect.MatchMethod getMatchMethod();

  int getMatches(Player player, Map<String, Float> values);

  // Port additions: used to explain effects in tooltips (#178, #166).
  default java.util.Set<String> getGroups() {
    return java.util.Set.of();
  }

  default double getAbove() {
    return 0.0;
  }

  default double getBelow() {
    return 1.0;
  }
}
