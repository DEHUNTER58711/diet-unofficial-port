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
 * - 26.3 FoodData#tick(ServerPlayer) and gamerule access
 */




package com.illusivesoulworks.diet.mixin;

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.common.util.DietRegeneration;
import com.illusivesoulworks.diet.common.util.PlayerSensitive;
import com.illusivesoulworks.diet.platform.Services;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(FoodData.class)
public class DietMixinFoodData implements PlayerSensitive {

  Player diet_player;

  // Captured-stack path (e.g. eating cake): CakeBlock still calls eat(int, float) in 26.3.
  @Inject(at = @At("TAIL"), method = "eat(IF)V")
  public void diet$eat(int healing, float saturationModifier, CallbackInfo ci) {

    if (diet_player == null) {
      DietConstants.LOG.error("Attempted to add food stats to a null player!");
      return;
    }
    Services.CAPABILITY.get(diet_player).ifPresent(tracker -> {
      ItemStack captured = tracker.getCapturedStack();

      if (!captured.isEmpty()) {
        tracker.consume(captured, healing, saturationModifier);
        tracker.captureStack(ItemStack.EMPTY);
      }
    });
  }

  // Port: the natural regeneration gamerule is read via GameRules#get(GameRule) in 26.3 and
  // tick() now receives the ServerPlayer directly.
  @ModifyVariable(
      at = @At("STORE"), ordinal = 0,
      method = "tick(Lnet/minecraft/server/level/ServerPlayer;)V")
  public boolean diet$tick(boolean flag) {
    return diet_player != null ? DietRegeneration.hasRegen(diet_player, flag) : flag;
  }

  @Override
  public void setPlayer(Player playerIn) {
    diet_player = playerIn;
  }
}
