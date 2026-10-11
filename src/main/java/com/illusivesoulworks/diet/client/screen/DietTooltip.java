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
 * - Holder-based attributes/effects, renamed operations
 */




package com.illusivesoulworks.diet.client.screen;

import net.minecraft.core.Holder;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.client.DietEffectSources;
import com.illusivesoulworks.diet.common.data.effect.DietEffectsInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class DietTooltip {

  public static List<Component> getEffects() {
    List<DietEffectsInfo.AttributeModifier> modifiers = DietScreen.tooltip.getModifiers();
    List<DietEffectsInfo.StatusEffect> effects = DietScreen.tooltip.getEffects();

    if (modifiers.isEmpty() && effects.isEmpty()) {
      return new ArrayList<>();
    }
    // Port feature (#53, #178): grouped by the effect that causes them (configurable).
    if (com.illusivesoulworks.diet.common.config.DietConfig.CLIENT.groupEffectTooltip.get()) {
      List<Component> grouped = DietEffectSources.groupedTooltip();

      if (!grouped.isEmpty()) {
        return grouped;
      }
    }
    List<Component> tooltips = new ArrayList<>();
    tooltips.add(Component.translatable("tooltip.diet.effects"));
    tooltips.add(Component.empty());
    tooltips.addAll(getLines(DietScreen.tooltip));
    return tooltips;
  }

  /**
   * Port: the merged attribute/effect lines of an info object, without header.
   */
  public static List<Component> getLines(DietEffectsInfo source) {
    List<DietEffectsInfo.AttributeModifier> modifiers = source.getModifiers();
    List<DietEffectsInfo.StatusEffect> effects = source.getEffects();
    List<Component> tooltips = new ArrayList<>();
    Map<Holder<Attribute>, AttributeTooltip> mergedAttributes = new HashMap<>();

    for (DietEffectsInfo.AttributeModifier modifier : modifiers) {
      mergedAttributes.computeIfAbsent(modifier.getAttribute(), (k) -> new AttributeTooltip())
          .merge(modifier);
    }

    for (Map.Entry<Holder<Attribute>, AttributeTooltip> attribute : mergedAttributes.entrySet()) {
      AttributeTooltip info = attribute.getValue();
      Holder<Attribute> key = attribute.getKey();

      if (key.is(DietApi.getInstance().getNaturalRegeneration())) {
        float val = (info.added + info.added * info.baseMultiplier) * info.totalMultiplier;

        if (val < 1.0f) {
          tooltips.add(Component.translatable("attribute.diet.modifier.disabled",
              Component.translatable(key.value().getDescriptionId())).withStyle(ChatFormatting.RED));
        }
      } else {
        addAttributeTooltip(tooltips, info.added, AttributeModifier.Operation.ADD_VALUE, key);
        addAttributeTooltip(tooltips, info.baseMultiplier,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, key);
        addAttributeTooltip(tooltips, info.totalMultiplier - 1.0f,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, key);
      }
    }
    Map<Holder<MobEffect>, Integer> mergedEffects = new HashMap<>();

    for (DietEffectsInfo.StatusEffect effect : effects) {
      mergedEffects.compute(effect.getEffect(),
          (k, v) -> v == null ? effect.getAmplifier() : Math.max(v, effect.getAmplifier()));
    }

    for (Map.Entry<Holder<MobEffect>, Integer> effect : mergedEffects.entrySet()) {
      Holder<MobEffect> effect1 = effect.getKey();
      MutableComponent iformattabletextcomponent =
          Component.translatable(effect1.value().getDescriptionId());

      if (effect.getValue() > 0) {
        iformattabletextcomponent =
            Component.translatable("potion.withAmplifier", iformattabletextcomponent,
                Component.translatable("potion.potency." + effect.getValue()));
      }
      tooltips.add(
          iformattabletextcomponent.withStyle(effect1.value().getCategory().getTooltipFormatting()));
    }
    return tooltips;
  }

  private static void addAttributeTooltip(List<Component> tooltips, float amount,
                                          AttributeModifier.Operation operation,
                                          Holder<Attribute> attribute) {
    double formattedAmount;

    if (operation != AttributeModifier.Operation.ADD_MULTIPLIED_BASE &&
        operation != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {

      if (attribute.is(Attributes.KNOCKBACK_RESISTANCE)) {
        formattedAmount = amount * 10.0D;
      } else {
        formattedAmount = amount;
      }
    } else {
      formattedAmount = amount * 100.0D;
    }

    if (amount > 0.0D) {
      tooltips.add((Component.translatable("attribute.modifier.plus." + operation.id(),
          ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(formattedAmount),
          Component.translatable(attribute.value().getDescriptionId()))).withStyle(ChatFormatting.BLUE));
    } else if (amount < 0.0D) {
      formattedAmount = formattedAmount * -1.0D;
      tooltips.add((Component.translatable("attribute.modifier.take." + operation.id(),
          ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(formattedAmount),
          Component.translatable(attribute.value().getDescriptionId()))).withStyle(ChatFormatting.RED));
    }
  }

  private static class AttributeTooltip {

    float added = 0;
    float baseMultiplier = 0.0f;
    float totalMultiplier = 1.0f;

    void merge(DietEffectsInfo.AttributeModifier modifier) {
      float amount = modifier.getAmount();

      if (modifier.getOperation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
        baseMultiplier += amount;
      } else if (modifier.getOperation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
        totalMultiplier *= 1.0f + amount;
      } else {
        added += amount;
      }
    }
  }
}
