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
 * - Minecraft 26.3 / NeoForge API migration (ResourceLocation -> Identifier, Optional NBT
 *   getters, registry holders)
 */




package com.illusivesoulworks.diet.common.data.effect;

import com.illusivesoulworks.diet.platform.Services;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder;
import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.IDietAttribute;
import com.illusivesoulworks.diet.api.type.IDietCondition;
import com.illusivesoulworks.diet.api.type.IDietEffect;
import com.illusivesoulworks.diet.api.type.IDietStatusEffect;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class DietEffect implements IDietEffect {

  public final List<IDietAttribute> attributes;
  public final List<IDietStatusEffect> statusEffects;
  public final List<IDietCondition> conditions;
  public final UUID uuid;
  // Port feature (#53): optional "title" of an effect (translation key or literal text).
  public final String title;

  public DietEffect(UUID uuid, List<IDietAttribute> attributes,
                    List<IDietStatusEffect> statusEffects, List<IDietCondition> conditions) {
    this(uuid, attributes, statusEffects, conditions, null);
  }

  public DietEffect(UUID uuid, List<IDietAttribute> attributes,
                    List<IDietStatusEffect> statusEffects, List<IDietCondition> conditions,
                    String title) {
    this.attributes = attributes;
    this.statusEffects = statusEffects;
    this.conditions = conditions;
    this.uuid = uuid;
    this.title = title == null || title.isEmpty() ? null : title;
  }

  @Override
  public Optional<String> getTitle() {
    return Optional.ofNullable(this.title);
  }

  /**
   * Port feature: effects are now synced to clients (inside the suite), so the Diet screen can
   * show which group thresholds cause which effect (#178, #166, #53).
   */
  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putString("UUID", this.uuid.toString());

    if (this.title != null) {
      tag.putString("Title", this.title);
    }
    ListTag conds = new ListTag();

    for (IDietCondition condition : this.conditions) {
      CompoundTag c = new CompoundTag();
      ListTag groups = new ListTag();
      condition.getGroups().forEach(g -> groups.add(StringTag.valueOf(g)));
      c.put("Groups", groups);
      c.putString("Match", condition.getMatchMethod().name());
      c.putDouble("Above", condition.getAbove());
      c.putDouble("Below", condition.getBelow());
      conds.add(c);
    }
    tag.put("Conditions", conds);
    ListTag atts = new ListTag();

    for (IDietAttribute attribute : this.attributes) {
      CompoundTag a = new CompoundTag();
      a.putString("Name", Services.REGISTRY.getAttributeKey(attribute.getAttribute()).toString());
      a.putInt("Operation", attribute.getOperation().id());
      a.putDouble("Amount", attribute.getBaseAmount());
      a.putDouble("Increment", attribute.getIncrement());
      atts.add(a);
    }
    tag.put("Attributes", atts);
    ListTag effs = new ListTag();

    for (IDietStatusEffect effect : this.statusEffects) {
      CompoundTag e = new CompoundTag();
      e.putString("Name", Services.REGISTRY.getStatusEffectKey(effect.getEffect()).toString());
      e.putInt("Power", effect.getBasePower());
      e.putInt("Increment", effect.getIncrement());
      effs.add(e);
    }
    tag.put("StatusEffects", effs);
    return tag;
  }

  public static DietEffect load(CompoundTag tag) {
    UUID uuid;

    try {
      uuid = UUID.fromString(tag.getStringOr("UUID", ""));
    } catch (IllegalArgumentException e) {
      uuid = UUID.randomUUID();
    }
    List<IDietCondition> conditions = new ArrayList<>();

    for (Tag t : tag.getListOrEmpty("Conditions")) {

      if (t instanceof CompoundTag c) {
        Set<String> groups = new HashSet<>();
        c.getListOrEmpty("Groups").forEach(g -> g.asString().ifPresent(groups::add));
        MatchMethod match;

        try {
          match = MatchMethod.valueOf(c.getStringOr("Match", "ANY"));
        } catch (IllegalArgumentException e) {
          match = MatchMethod.ANY;
        }
        conditions.add(new DietCondition(groups, match, c.getDoubleOr("Above", 0.0),
            c.getDoubleOr("Below", 1.0)));
      }
    }
    List<IDietAttribute> attributes = new ArrayList<>();

    for (Tag t : tag.getListOrEmpty("Attributes")) {

      if (t instanceof CompoundTag a) {
        Services.REGISTRY.getAttribute(Identifier.parse(a.getStringOr("Name", "minecraft:luck")))
            .ifPresent(att -> attributes.add(new DietAttribute(att,
                AttributeModifier.Operation.BY_ID.apply(a.getIntOr("Operation", 0)),
                a.getDoubleOr("Amount", 0.0), a.getDoubleOr("Increment", 0.0))));
      }
    }
    List<IDietStatusEffect> effects = new ArrayList<>();

    for (Tag t : tag.getListOrEmpty("StatusEffects")) {

      if (t instanceof CompoundTag e) {
        Services.REGISTRY.getStatusEffect(Identifier.parse(e.getStringOr("Name", "minecraft:luck")))
            .ifPresent(eff -> effects.add(new DietStatusEffect(eff, e.getIntOr("Power", 0),
                e.getIntOr("Increment", 1))));
      }
    }
    return new DietEffect(uuid, attributes, effects, conditions, tag.getStringOr("Title", ""));
  }

  @Override
  public List<IDietCondition> getConditions() {
    return this.conditions;
  }

  @Override
  public List<IDietAttribute> getAttributes() {
    return this.attributes;
  }

  @Override
  public List<IDietStatusEffect> getStatusEffects() {
    return this.statusEffects;
  }

  @Override
  public UUID getUuid() {
    return this.uuid;
  }

  public static class DietAttribute implements IDietAttribute {

    public final Holder<Attribute> attribute;
    public final AttributeModifier.Operation operation;
    public final double amount;
    public final double increment;

    public DietAttribute(Holder<Attribute> attribute, AttributeModifier.Operation operation,
                         double amount) {
      this(attribute, operation, amount, amount);
    }

    public DietAttribute(Holder<Attribute> attribute, AttributeModifier.Operation operation,
                         double amount, double increment) {
      this.attribute = attribute;
      this.operation = operation;
      this.amount = amount;
      this.increment = increment;
    }

    @Override
    public Holder<Attribute> getAttribute() {
      return this.attribute;
    }

    @Override
    public AttributeModifier.Operation getOperation() {
      return this.operation;
    }

    @Override
    public double getBaseAmount() {
      return this.amount;
    }

    @Override
    public double getIncrement() {
      return this.increment;
    }
  }

  public static class DietStatusEffect implements IDietStatusEffect {

    public final Holder<MobEffect> effect;
    public final int power;
    public final int increment;

    public DietStatusEffect(Holder<MobEffect> effect, int power) {
      this(effect, power, power);
    }

    public DietStatusEffect(Holder<MobEffect> effect, int power, int increment) {
      this.effect = effect;
      this.power = power;
      this.increment = increment;
    }

    @Override
    public Holder<MobEffect> getEffect() {
      return this.effect;
    }

    @Override
    public int getBasePower() {
      return this.power;
    }

    @Override
    public int getIncrement() {
      return this.increment;
    }
  }

  public static class DietCondition implements IDietCondition {

    public final Set<String> groups;
    public final MatchMethod match;
    public final double above;
    public final double below;

    public DietCondition(Set<String> groups, MatchMethod match, double above, double below) {
      this.groups = groups;
      this.match = match;
      this.above = above;
      this.below = below;
    }

    @Override
    public MatchMethod getMatchMethod() {
      return this.match;
    }

    @Override
    public Set<String> getGroups() {
      return this.groups;
    }

    @Override
    public double getAbove() {
      return this.above;
    }

    @Override
    public double getBelow() {
      return this.below;
    }

    public int getMatches(Player player, Map<String, Float> values) {
      return this.match.getMatches(this.groups, values, (float) this.above, (float) this.below);
    }
  }

  public enum MatchMethod {
    EVERY {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {
        int count = 0;

        for (String group : groups) {
          Float value = values.get(group);

          if (value != null && MatchMethod.inRange(value, above, below)) {
            count++;
          }
        }
        return count;
      }
    },
    ANY {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {

        for (String group : groups) {
          Float value = values.get(group);

          if (value != null && MatchMethod.inRange(value, above, below)) {
            return 1;
          }
        }
        return 0;
      }
    },
    AVERAGE {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {
        float sum = 0;

        for (String group : groups) {
          Float value = values.get(group);

          if (value != null) {
            sum += value;
          }
        }
        return MatchMethod.inRange(sum / (float) groups.size(), above, below) ? 1 : 0;
      }
    },
    ALL {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {

        for (String group : groups) {
          Float value = values.get(group);

          if (value == null || !MatchMethod.inRange(value, above, below)) {
            return 0;
          }
        }
        return 1;
      }
    },
    NONE {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {

        for (String group : groups) {
          Float value = values.get(group);

          if (value != null && MatchMethod.inRange(value, above, below)) {
            return 0;
          }
        }
        return 1;
      }
    };

    abstract int getMatches(Set<String> groups, Map<String, Float> values, float above,
                            float below);

    public static MatchMethod findOrDefault(String val, MatchMethod def) {
      try {
        return MatchMethod.valueOf(val.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        DietConstants.LOG.error("No such match method " + val);
      }
      return def;
    }

    private static boolean inRange(float value, float above, float below) {
      return value >= above && value <= below;
    }
  }
}
