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
 * - deferred tooltips, Gui#setScreen
 */




package com.illusivesoulworks.diet.client;

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietResult;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import com.illusivesoulworks.diet.client.screen.DynamicButton;
import com.illusivesoulworks.diet.common.config.DietConfig;
import com.illusivesoulworks.diet.common.util.DietResult;
import com.illusivesoulworks.diet.platform.ClientServices;
import com.illusivesoulworks.diet.platform.Services;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DietClientEvents {

  private static final TagKey<Item> SPECIAL_FOOD = TagKey.create(Registries.ITEM,
      Identifier.fromNamespaceAndPath(DietConstants.MOD_ID, "special_food"));
  private static final DecimalFormat DECIMALFORMAT = Util.make(new DecimalFormat("#.#"),
      (num) -> num.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));
  private static final Identifier ICONS =
      Identifier.fromNamespaceAndPath(DietConstants.MOD_ID, "textures/gui/icons.png");


  private static final java.util.Set<java.util.UUID> NOTIFIED = new java.util.HashSet<>();

  public static void tick(Minecraft mc) {
    LocalPlayer player = mc.player;
    notifyHarmfulEffects(mc, player);

    if (player != null && mc.isWindowActive() && !(mc.gui.screen() instanceof DietScreen) &&
        DietKeys.OPEN_GUI.consumeClick()) {
      mc.gui.setScreen(new DietScreen(mc.gui.screen() instanceof InventoryScreen));
    }
  }

  /**
   * Port feature (#166): when a harmful diet effect becomes active, show its cause once in the
   * action bar ("Hunger, Movement Speed: caused by Sugars at 80%-100%").
   */
  private static void notifyHarmfulEffects(Minecraft mc, LocalPlayer player) {

    if (player == null) {
      NOTIFIED.clear();
      return;
    }

    if (player.tickCount % 20 != 0 || !DietConfig.CLIENT.effectNotifications.get()) {
      return;
    }
    java.util.Set<java.util.UUID> current = new java.util.HashSet<>();

    for (com.illusivesoulworks.diet.api.type.IDietEffect effect :
        DietEffectSources.activeHarmful(player)) {
      current.add(effect.getUuid());

      if (!NOTIFIED.contains(effect.getUuid())) {
        mc.gui.hud.setOverlayMessage(Component.translatable("message.diet.harmful_effect",
                DietEffectSources.title(effect), DietEffectSources.describeConditions(effect))
            .withStyle(ChatFormatting.RED), false);
      }
    }
    NOTIFIED.retainAll(current);
    NOTIFIED.addAll(current);
  }

  public static DynamicButton getButton(Screen screen) {

    if (DietConfig.CLIENT.addButton.get() && screen instanceof AbstractContainerScreen<?>) {
      AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
      // Port: the button is only created for AbstractContainerScreens; with addButton = false
      // null is returned and the listener skips it.
      return new DynamicButton(containerScreen,
          ClientServices.INSTANCE.getGuiLeft(containerScreen) + DietConfig.CLIENT.buttonX.get(),
          containerScreen.height / 2 + DietConfig.CLIENT.buttonY.get(), 20, 18, 0, 0, 19, ICONS,
          (button) -> Minecraft.getInstance().gui.setScreen(new DietScreen(true)));
    }
    return null;
  }

  public static void renderItemTooltip(Player player, ItemStack stack, List<Component> tooltips) {

    // Port feature #209: food tooltips can be disabled completely.
    if (player != null && DietConfig.CLIENT.foodTooltips.get()) {

      // Port feature #212: Creative players see all tooltips (configurable).
      boolean bypass = player.isCreative() && DietConfig.SERVER.creativeBypassesHiddenTooltips.get();

      if (!bypass && DietConfig.SERVER.hideTooltipsUntilEaten.get() &&
          Services.CAPABILITY.get(player)
              .map(tracker -> !tracker.getEaten().contains(stack.getItem())).orElse(false)) {
        return;
      }
      IDietResult result = DietApi.getInstance().get(player, stack);

      if (result != DietResult.EMPTY) {
        Map<IDietGroup, Float> groups = result.get();
        boolean specialFood = stack.is(SPECIAL_FOOD);

        if (!groups.isEmpty()) {
          List<Component> groupsTooltips = new ArrayList<>();
          List<Component> beneficial = new ArrayList<>();
          List<Component> harmful = new ArrayList<>();

          for (Map.Entry<IDietGroup, Float> entry : groups.entrySet()) {
            float value = entry.getValue();
            Component groupName = Component.translatable(
                "groups." + DietConstants.MOD_ID + "." + entry.getKey().getName() + ".name");
            MutableComponent tooltip = null;

            if (specialFood) {
              tooltip =
                  Component.translatable("tooltip." + DietConstants.MOD_ID + ".group_", groupName);
            } else if (value > 0.0f) {
              tooltip = Component.translatable("tooltip." + DietConstants.MOD_ID + ".group",
                  DECIMALFORMAT.format(entry.getValue() * 100), groupName);
            }

            if (tooltip != null) {

              if (entry.getKey().isBeneficial()) {
                tooltip.withStyle(ChatFormatting.GREEN);
                beneficial.add(tooltip);
              } else {
                tooltip.withStyle(ChatFormatting.RED);
                harmful.add(tooltip);
              }
            }
          }
          groupsTooltips.addAll(beneficial);
          groupsTooltips.addAll(harmful);

          if (!groupsTooltips.isEmpty()) {
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("tooltip." + DietConstants.MOD_ID + ".eaten")
                .withStyle(ChatFormatting.GRAY));
            tooltips.addAll(groupsTooltips);
          }
        }
      }
    }
  }
}
