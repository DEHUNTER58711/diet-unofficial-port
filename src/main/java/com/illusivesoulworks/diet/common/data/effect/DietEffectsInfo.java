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
 * - Holder-based attributes/effects, Optional NBT getters
 */




package com.illusivesoulworks.diet.common.data.effect;

import net.minecraft.core.Holder;
import com.illusivesoulworks.diet.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class DietEffectsInfo {

  private final List<AttributeModifier> modifiers = new ArrayList<>();
  private final List<StatusEffect> effects = new ArrayList<>();

  public void addModifier(Holder<Attribute> attribute,
                          net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation,
                          float amount) {
    modifiers.add(new AttributeModifier(attribute, operation, amount));
  }

  public void addModifier(Holder<Attribute> attribute,
                          net.minecraft.world.entity.ai.attributes.AttributeModifier modifier) {
    modifiers.add(new AttributeModifier(attribute, modifier));
  }

  public void addEffect(Holder<MobEffect> effect, int amplifier) {
    effects.add(new StatusEffect(effect, amplifier));
  }

  public void addEffect(MobEffectInstance effectInstance) {
    effects.add(new StatusEffect(effectInstance));
  }

  public List<AttributeModifier> getModifiers() {
    return modifiers;
  }

  public List<StatusEffect> getEffects() {
    return effects;
  }

  public CompoundTag write() {
    CompoundTag tag = new CompoundTag();
    ListTag modifiersList = new ListTag();

    for (AttributeModifier modifier : modifiers) {
      CompoundTag modifierTag = new CompoundTag();
      modifierTag.putString("AttributeName",
          Objects.requireNonNull(Services.REGISTRY.getAttributeKey(modifier.attribute)).toString());
      modifierTag.putFloat("Amount", modifier.amount);
      modifierTag.putInt("Operation", modifier.operation.id());
      modifiersList.add(modifierTag);
    }
    ListTag effectsList = new ListTag();

    for (StatusEffect effect : effects) {
      CompoundTag effectTag = new CompoundTag();
      effectTag.putString("EffectName",
          Objects.requireNonNull(Services.REGISTRY.getStatusEffectKey(effect.effect)).toString());
      effectTag.putInt("Amplifier", effect.amplifier);
      effectsList.add(effectTag);
    }
    tag.put("Modifiers", modifiersList);
    tag.put("Effects", effectsList);
    return tag;
  }

  public static DietEffectsInfo read(CompoundTag tag) {
    DietEffectsInfo info = new DietEffectsInfo();
    ListTag modifiersList = tag.getListOrEmpty("Modifiers");

    for (int i = 0; i < modifiersList.size(); i++) {
      CompoundTag modifierTag = modifiersList.getCompoundOrEmpty(i);
      String name = modifierTag.getStringOr("AttributeName", "");
      Holder<Attribute> attribute = Services.REGISTRY.getAttribute(Identifier.parse(name)).orElse(null);

      if (attribute == null) {
        continue;
      }
      float amount = modifierTag.getFloatOr("Amount", 0.0f);
      net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation =
          net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.BY_ID
              .apply(modifierTag.getIntOr("Operation", 0));
      info.addModifier(attribute, operation, amount);
    }
    ListTag effectsList = tag.getListOrEmpty("Effects");

    for (int i = 0; i < effectsList.size(); i++) {
      CompoundTag effectTag = effectsList.getCompoundOrEmpty(i);
      String name = effectTag.getStringOr("EffectName", "");
      Holder<MobEffect> effect = Services.REGISTRY.getStatusEffect(Identifier.parse(name)).orElse(null);

      if (effect == null) {
        continue;
      }
      int amplifier = effectTag.getIntOr("Amplifier", 0);
      info.addEffect(effect, amplifier);
    }
    return info;
  }

  public static final class AttributeModifier {

    private final Holder<Attribute> attribute;
    private final net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation;
    private final float amount;

    private AttributeModifier(Holder<Attribute> attributeIn,
                              net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operationIn,
                              float amountIn) {
      attribute = attributeIn;
      operation = operationIn;
      amount = amountIn;
    }

    private AttributeModifier(Holder<Attribute> attributeIn,
                              net.minecraft.world.entity.ai.attributes.AttributeModifier modifier) {
      attribute = attributeIn;
      operation = modifier.operation();
      amount = (float) modifier.amount();
    }

    public Holder<Attribute> getAttribute() {
      return attribute;
    }

    public net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation getOperation() {
      return operation;
    }

    public float getAmount() {
      return amount;
    }
  }

  public static final class StatusEffect {

    private final Holder<MobEffect> effect;
    private final int amplifier;

    private StatusEffect(Holder<MobEffect> effectIn, int amplifierIn) {
      effect = effectIn;
      amplifier = amplifierIn;
    }

    private StatusEffect(MobEffectInstance effectInstance) {
      effect = effectInstance.getEffect();
      amplifier = effectInstance.getAmplifier();
    }

    public Holder<MobEffect> getEffect() {
      return effect;
    }

    public int getAmplifier() {
      return amplifier;
    }
  }
}
