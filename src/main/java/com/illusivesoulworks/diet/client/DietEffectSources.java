/*
 * Unofficial NeoForge 26.3 port of Diet - new file.
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

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.IDietAttribute;
import com.illusivesoulworks.diet.api.type.IDietCondition;
import com.illusivesoulworks.diet.api.type.IDietEffect;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietStatusEffect;
import com.illusivesoulworks.diet.api.type.IDietSuite;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.client.screen.DietTooltip;
import com.illusivesoulworks.diet.common.data.effect.DietEffect;
import com.illusivesoulworks.diet.common.data.effect.DietEffectsInfo;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import com.illusivesoulworks.diet.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;

/**
 * Port feature (new, #178, #166, #53): explains which food group thresholds cause which diet
 * effect. Everything is computed on the client from the synced suite (incl. effects, now part of
 * the suite sync) and the synced group values, using the same matching rules as the server.
 */
public final class DietEffectSources {

  public record Evaluated(IDietEffect effect, boolean active, int multiplier) {
  }

  private DietEffectSources() {
  }

  private static Optional<Player> player() {
    return Optional.ofNullable(Minecraft.getInstance().player);
  }

  public static List<Evaluated> evaluate(Player player) {
    List<Evaluated> result = new ArrayList<>();
    Optional<? extends IDietTracker> tracker = Services.CAPABILITY.get(player);

    if (tracker.isEmpty() || !tracker.get().isActive()) {
      return result;
    }
    Map<String, Float> values = tracker.get().getValues();
    Optional<IDietSuite> suite = DietSuites.getSuite(player.level(), tracker.get().getSuite());

    if (suite.isEmpty()) {
      return result;
    }

    for (IDietEffect effect : suite.get().getEffects()) {
      boolean match = true;
      int multiplier = 0;

      for (IDietCondition condition : effect.getConditions()) {
        int matches = condition.getMatches(player, values);

        if (matches == 0) {
          match = false;
          break;
        }

        if (condition.getMatchMethod() == DietEffect.MatchMethod.EVERY) {
          multiplier += matches;
        }
      }
      result.add(new Evaluated(effect, match, Math.max(1, multiplier)));
    }
    return result;
  }

  private static String percent(double value) {
    return Math.round(value * 100.0) + "%";
  }

  private static Component groupName(String group) {
    return Component.translatable("groups." + DietConstants.MOD_ID + "." + group + ".name");
  }

  public static Component describeCondition(IDietCondition condition) {
    MutableComponent groups = Component.empty();
    boolean first = true;

    for (String group : condition.getGroups().stream().sorted().toList()) {

      if (!first) {
        groups.append(", ");
      }
      groups.append(groupName(group));
      first = false;
    }
    Component range = Component.translatable("tooltip.diet.source.range",
        percent(condition.getAbove()), percent(condition.getBelow()));
    return Component.translatable(
        "tooltip.diet.source.match." + condition.getMatchMethod().name().toLowerCase(),
        groups, range);
  }

  public static Component describeConditions(IDietEffect effect) {
    MutableComponent text = Component.empty();
    boolean first = true;

    for (IDietCondition condition : effect.getConditions()) {

      if (!first) {
        text.append(Component.translatable("tooltip.diet.source.and"));
      }
      text.append(describeCondition(condition));
      first = false;
    }
    return text;
  }

  public static Component title(IDietEffect effect) {
    Optional<String> title = effect.getTitle();

    if (title.isPresent()) {
      String t = title.get();
      return Language.getInstance().has(t) ? Component.translatable(t) : Component.literal(t);
    }
    return describeConditions(effect);
  }

  private static boolean harmful(IDietEffect effect) {

    for (IDietStatusEffect status : effect.getStatusEffects()) {

      if (status.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
        return true;
      }
    }

    for (IDietAttribute attribute : effect.getAttributes()) {

      if (attribute.getBaseAmount() < 0) {
        return true;
      }
    }
    return false;
  }

  public static List<Component> lines(IDietEffect effect, int multiplier) {
    DietEffectsInfo info = new DietEffectsInfo();

    for (IDietAttribute attribute : effect.getAttributes()) {
      info.addModifier(attribute.getAttribute(), attribute.getOperation(),
          (float) (attribute.getBaseAmount() + (multiplier - 1) * attribute.getIncrement()));
    }

    for (IDietStatusEffect status : effect.getStatusEffects()) {
      info.addEffect(status.getEffect(),
          status.getBasePower() + (multiplier - 1) * status.getIncrement());
    }
    return DietTooltip.getLines(info);
  }

  /**
   * Effects icon tooltip: every active effect with its cause.
   */
  public static List<Component> groupedTooltip() {
    List<Component> tooltips = new ArrayList<>();
    player().ifPresent(player -> {

      for (Evaluated evaluated : evaluate(player)) {

        if (!evaluated.active()) {
          continue;
        }

        if (tooltips.isEmpty()) {
          tooltips.add(Component.translatable("tooltip.diet.effects"));
        }
        tooltips.add(Component.empty());
        tooltips.add(title(evaluated.effect()).copy().withStyle(
            harmful(evaluated.effect()) ? ChatFormatting.RED : ChatFormatting.GOLD));

        if (evaluated.effect().getTitle().isPresent()) {
          tooltips.add(describeConditions(evaluated.effect()).copy()
              .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        for (Component line : lines(evaluated.effect(), evaluated.multiplier())) {
          tooltips.add(Component.literal("  ").append(line));
        }
      }
    });
    return tooltips;
  }

  /**
   * Group row tooltip in the Diet screen: all effects whose conditions involve the group.
   */
  public static List<Component> groupTooltip(IDietGroup group) {
    List<Component> tooltips = new ArrayList<>();
    player().ifPresent(player -> {

      for (Evaluated evaluated : evaluate(player)) {
        IDietEffect effect = evaluated.effect();
        boolean involved = effect.getConditions().stream()
            .anyMatch(c -> c.getGroups().contains(group.getName()));

        if (!involved) {
          continue;
        }

        if (tooltips.isEmpty()) {
          tooltips.add(Component.translatable("tooltip.diet.source.group_effects")
              .withStyle(ChatFormatting.GRAY));
        }
        ChatFormatting color = !evaluated.active() ? ChatFormatting.DARK_GRAY :
            harmful(effect) ? ChatFormatting.RED : ChatFormatting.GREEN;
        Component state = Component.translatable(
            evaluated.active() ? "tooltip.diet.source.active" : "tooltip.diet.source.inactive");
        tooltips.add(Component.empty().append(state).append(" ").append(title(effect))
            .withStyle(color));

        if (effect.getTitle().isPresent()) {
          tooltips.add(Component.literal("  ").append(describeConditions(effect))
              .withStyle(ChatFormatting.DARK_GRAY));
        }

        for (Component line : lines(effect, evaluated.multiplier())) {
          tooltips.add(Component.literal("    ").append(line));
        }
      }
    });
    return tooltips;
  }

  /**
   * Source of a status effect currently applied by Diet, for the inventory effect tooltip.
   */
  public static List<Component> sourcesOf(Holder<MobEffect> mobEffect) {
    List<Component> result = new ArrayList<>();
    player().ifPresent(player -> {

      for (Evaluated evaluated : evaluate(player)) {

        if (evaluated.active() && evaluated.effect().getStatusEffects().stream()
            .anyMatch(s -> s.getEffect().is(mobEffect))) {
          result.add(Component.translatable("tooltip.diet.source.caused_by",
              title(evaluated.effect())).withStyle(ChatFormatting.GOLD));

          if (evaluated.effect().getTitle().isPresent()) {
            result.add(Component.literal("  ").append(describeConditions(evaluated.effect()))
                .withStyle(ChatFormatting.GRAY));
          }
        }
      }
    });
    return result;
  }

  /**
   * Harmful effects that just became active (for the on-screen notice, #166).
   */
  public static List<IDietEffect> activeHarmful(Player player) {
    List<IDietEffect> result = new ArrayList<>();

    for (Evaluated evaluated : evaluate(player)) {

      if (evaluated.active() && harmful(evaluated.effect())) {
        result.add(evaluated.effect());
      }
    }
    return result;
  }
}
