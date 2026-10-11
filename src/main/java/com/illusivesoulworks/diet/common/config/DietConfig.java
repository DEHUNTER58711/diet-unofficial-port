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
 * - SpectreLib config -> NeoForge ModConfigSpec
 */




package com.illusivesoulworks.diet.common.config;

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.platform.Services;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.tuple.Pair;

public class DietConfig {

  public static final ModConfigSpec CLIENT_SPEC;
  public static final Client CLIENT;

  public static final ModConfigSpec SERVER_SPEC;
  public static final Server SERVER;

  private static final String CONFIG_PREFIX = "gui." + DietConstants.MOD_ID + ".config.";

  static {
    final Pair<Client, ModConfigSpec> clientPair =
        new ModConfigSpec.Builder().configure(Client::new);
    CLIENT_SPEC = clientPair.getRight();
    CLIENT = clientPair.getLeft();
    final Pair<Server, ModConfigSpec> generalPair = new ModConfigSpec.Builder()
        .configure(Server::new);
    SERVER_SPEC = generalPair.getRight();
    SERVER = generalPair.getLeft();
  }

  public static class Client {

    public final ModConfigSpec.IntValue buttonX;
    public final ModConfigSpec.IntValue buttonY;
    public final ModConfigSpec.ConfigValue<String> textColor;
    public final ModConfigSpec.BooleanValue addButton;
    // Port features (new keys, all default to the behaviour described in PORTING_NOTES.md)
    public final ModConfigSpec.ConfigValue<List<? extends String>> buttonScreens;
    public final ModConfigSpec.BooleanValue buttonDragging;
    public final ModConfigSpec.BooleanValue foodTooltips;
    public final ModConfigSpec.BooleanValue groupEffectTooltip;
    public final ModConfigSpec.BooleanValue groupRowTooltips;
    public final ModConfigSpec.BooleanValue effectSourceTooltips;
    public final ModConfigSpec.BooleanValue effectNotifications;

    public Client(ModConfigSpec.Builder builder) {
      addButton =
          builder.comment("If enabled, a button to the Diet GUI appears in player inventories.")
              .translation(CONFIG_PREFIX + "addButton").define("addButton", true);

      buttonX = builder.comment("The x-position of the Diet GUI button in player inventories.")
          .translation(CONFIG_PREFIX + "buttonX").defineInRange("buttonX", 126, -10000, 10000);

      buttonY = builder.comment("The y-position of the Diet GUI button in player inventories.")
          .translation(CONFIG_PREFIX + "buttonY").defineInRange("buttonY", -22, -10000, 10000);

      textColor =
          builder.comment("The primary text color of the Diet GUI, as an integer or hexadecimal.")
              .translation(CONFIG_PREFIX + "textColor").define("textColor", "4210752");

      buttonScreens = builder.comment("""
              Screens that get the Diet button (fully qualified class or interface names; sub-classes
              match too). Add other inventory screens here; remove entries to hide the button there.""")
          .translation(CONFIG_PREFIX + "buttonScreens")
          .defineList("buttonScreens", List.of(
                  "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                  "top.theillusivec4.curios.api.client.ICuriosScreen",
                  "top.theillusivec4.curios.client.screen.CuriosScreen"),
              () -> "net.minecraft.client.gui.screens.inventory.InventoryScreen",
              s -> s instanceof String);

      buttonDragging = builder.comment("""
              If enabled, the Diet button can be moved by dragging it with Shift + left mouse button.
              The new position is saved to buttonX/buttonY.""")
          .translation(CONFIG_PREFIX + "buttonDragging").define("buttonDragging", true);

      foodTooltips = builder.comment(
              "If disabled, Diet adds no food group (\"When eaten\") lines to item tooltips.")
          .translation(CONFIG_PREFIX + "foodTooltips").define("foodTooltips", true);

      groupEffectTooltip = builder.comment("""
              If enabled, the Diet effects tooltip lists every active effect separately with its title
              and the food group condition that causes it. If disabled, all effects are merged like
              in the original mod.""")
          .translation(CONFIG_PREFIX + "groupEffectTooltip").define("groupEffectTooltip", true);

      groupRowTooltips = builder.comment(
              "If enabled, hovering a food group in the Diet screen shows the effects it influences.")
          .translation(CONFIG_PREFIX + "groupRowTooltips").define("groupRowTooltips", true);

      effectSourceTooltips = builder.comment(
              "If enabled, status effect tooltips in the inventory show which diet condition causes them.")
          .translation(CONFIG_PREFIX + "effectSourceTooltips").define("effectSourceTooltips", true);

      effectNotifications = builder.comment(
              "If enabled, an on-screen message names the cause when a harmful diet effect starts.")
          .translation(CONFIG_PREFIX + "effectNotifications").define("effectNotifications", true);
    }
  }

  public static class Server {

    public final ModConfigSpec.IntValue deathPenaltyMin;
    public final ModConfigSpec.IntValue deathPenaltyLoss;
    public final ModConfigSpec.EnumValue<DeathPenaltyMethod> deathPenaltyMethod;

    public final ModConfigSpec.IntValue gainPenaltyPerGroup;
    public final ModConfigSpec.IntValue decayPenaltyPerGroup;
    public final ModConfigSpec.ConfigValue<List<? extends String>> foodOverrides;

    public final ModConfigSpec.BooleanValue hideTooltipsUntilEaten;
    public final ModConfigSpec.BooleanValue creativeBypassesHiddenTooltips;
    public final ModConfigSpec.BooleanValue generateGroupsForEmptyItems;

    public Server(ModConfigSpec.Builder builder) {
      deathPenaltyMin =
          builder.comment("The minimum percentage that diet groups can be reduced to upon death.")
              .translation(CONFIG_PREFIX + "deathPenaltyMin")
              .defineInRange("deathPenaltyMin", 0, 0, 100);

      deathPenaltyLoss =
          builder.comment("The reduction in percentage applied to all diet groups upon death.")
              .translation(CONFIG_PREFIX + "deathPenaltyLoss")
              .defineInRange("deathPenaltyLoss", 100, 0, 100);

      deathPenaltyMethod = builder
          .comment("""
              The method to apply for losses due to death penalties.
              AMOUNT = Reduce by a flat percentage amount
              PERCENT = Reduce by a percent of the current value
              RESET = Reset value to defaults""")
          .translation(CONFIG_PREFIX + "deathPenaltyMethod")
          .defineEnum("deathPenaltyMethod", DeathPenaltyMethod.AMOUNT);

      gainPenaltyPerGroup = builder
          .comment("The percentage reduction in total gain for each diet group consumed at once.")
          .translation(CONFIG_PREFIX + "gainPenaltyPerGroup")
          .defineInRange("gainPenaltyPerGroup", 15, 0, 100);

      decayPenaltyPerGroup = builder
          .comment("The percentage reduction in total decay for each diet group decayed at once.")
          .translation(CONFIG_PREFIX + "decayPenaltyPerGroup")
          .defineInRange("decayPenaltyPerGroup", 15, 0, 100);

      foodOverrides = builder.comment(
              "List of food quality overrides for diet gain values." +
                  "\nFormat: \"modid:name;quality\"")
          .translation(CONFIG_PREFIX + "foodOverrides")
          .defineList("foodOverrides", new ArrayList<>(), () -> "minecraft:apple;1.0", s -> s instanceof String);

      generateGroupsForEmptyItems = builder.comment(
              "If enabled, food groups are assigned to unclassified items based on ingredients.")
          .translation(CONFIG_PREFIX + "generateGroupsForEmptyItems")
          .define("generateGroupsForEmptyItems", true);

      hideTooltipsUntilEaten = builder.comment(
              "If enabled, food group tooltips are hidden until player has eaten that type of item.")
          .translation(CONFIG_PREFIX + "hideTooltipsUntilEaten")
          .define("hideTooltipsUntilEaten", false);

      creativeBypassesHiddenTooltips = builder.comment(
              "If enabled, players in Creative mode always see food group tooltips (see hideTooltipsUntilEaten).")
          .translation(CONFIG_PREFIX + "creativeBypassesHiddenTooltips")
          .define("creativeBypassesHiddenTooltips", true);
    }

    private final Map<Item, Float> overrideMap = new HashMap<>();
    public boolean initializedFoodOverrides = false;

    public Float getFoodOverride(Item item) {

      if (!initializedFoodOverrides) {
        overrideMap.clear();
        initializedFoodOverrides = true;

        for (String s : foodOverrides.get()) {
          String[] split = s.split(";");
          Services.REGISTRY.getItem(Identifier.parse(split[0].trim()))
              .ifPresent(item1 -> overrideMap.put(item1, Float.parseFloat(split[1])));
        }
      }
      return overrideMap.get(item);
    }
  }

  public enum DeathPenaltyMethod {
    AMOUNT,
    PERCENT,
    RESET
  }
}
