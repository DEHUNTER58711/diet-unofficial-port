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




package com.illusivesoulworks.diet.common.data.group;

import com.illusivesoulworks.diet.DietCommonMod;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.util.DietColor;
import com.illusivesoulworks.diet.platform.Services;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DietGroup implements IDietGroup {

  private final String name;
  private final Item icon;
  private final DietColor color;
  private final float defaultValue;
  private final int order;
  private final double gainMultiplier;
  private final double decayMultiplier;
  private final boolean beneficial;
  private final TagKey<Item> tag;
  // Port feature: extra members / removals from the datapack format (see DietItemList).
  private final DietItemList added;
  private final DietItemList removed;

  private DietGroup(Builder builder) {
    this.name = builder.name;
    this.icon = builder.icon;
    this.color = builder.color;
    this.defaultValue = builder.defaultValue;
    this.order = builder.order;
    this.gainMultiplier = builder.gainMultiplier;
    this.decayMultiplier = builder.decayMultiplier;
    this.beneficial = builder.beneficial;
    this.tag = TagKey.create(Registries.ITEM, DietCommonMod.resource(this.name));
    this.added = builder.added;
    this.removed = builder.removed;
  }

  public static IDietGroup load(CompoundTag tag) {
    String name = tag.getStringOr("Name", "");
    String icon = tag.getStringOr("Icon", "minecraft:air");
    Item item = Services.REGISTRY.getItem(Identifier.parse(icon)).orElse(null);
    int order = tag.getIntOr("Order", 1);
    boolean beneficial = tag.getBooleanOr("Beneficial", true);
    int color = tag.getIntOr("Color", 0xFFFFFF);
    int r = ((color >> 16) & 0xff);
    int g = ((color >> 8) & 0xff);
    int b = ((color) & 0xff);
    Builder builder = new Builder(name);
    builder.icon(item);
    builder.order(order);
    builder.beneficial(beneficial);
    builder.color(new DietColor(r, g, b));
    builder.gainMultiplier((float) tag.getDoubleOr("Gain", 1.0));
    builder.decayMultiplier((float) tag.getDoubleOr("Decay", 1.0));
    builder.add(DietItemList.load(tag.getListOrEmpty("Add")));
    builder.remove(DietItemList.load(tag.getListOrEmpty("Remove")));
    return builder.build();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Item getIcon() {
    return icon;
  }

  @Override
  public DietColor getColor() {
    return color;
  }

  @Override
  public float getDefaultValue() {
    return defaultValue;
  }

  @Override
  public int getOrder() {
    return order;
  }

  @Override
  public double getGainMultiplier() {
    return gainMultiplier;
  }

  @Override
  public double getDecayMultiplier() {
    return decayMultiplier;
  }

  @Override
  public boolean isBeneficial() {
    return beneficial;
  }

  @Override
  public TagKey<Item> getTag() {
    return tag;
  }

  @Override
  public boolean contains(ItemStack stack) {
    // Port feature / fix #227: "remove" entries win over the tag and the "items" list.
    return !this.removed.matches(stack) && (stack.is(this.tag) || this.added.matches(stack));
  }

  @Override
  public boolean isExcluded(ItemStack stack) {
    return this.removed.matches(stack);
  }

  public DietItemList getAdded() {
    return this.added;
  }

  public DietItemList getRemoved() {
    return this.removed;
  }

  @Override
  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putString("Name", this.name);
    tag.putString("Icon", Services.REGISTRY.getItemKey(this.icon).toString());
    tag.putInt("Order", this.order);
    tag.putBoolean("Beneficial", this.beneficial);
    tag.putInt("Color", this.color.getRGB());
    tag.putDouble("Gain", this.gainMultiplier);
    tag.putDouble("Decay", this.decayMultiplier);
    tag.put("Add", this.added.save());
    tag.put("Remove", this.removed.save());
    return tag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DietGroup dietGroup = (DietGroup) o;
    return name.equals(dietGroup.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  public static class Builder {

    private final String name;
    private Item icon = Items.AIR;
    private DietColor color = new DietColor(255, 255, 255);
    private float defaultValue = 0.0f;
    private int order = 1;
    private float gainMultiplier = 1.0f;
    private float decayMultiplier = 1.0f;
    private boolean beneficial = true;
    private final DietItemList added = new DietItemList();
    private final DietItemList removed = new DietItemList();

    public Builder(String name) {
      this.name = name;
    }

    public Builder icon(Item icon) {
      this.icon = icon;
      return this;
    }

    public Builder color(DietColor color) {
      this.color = color;
      return this;
    }

    public Builder defaultValue(float defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder order(int order) {
      this.order = order;
      return this;
    }

    public Builder gainMultiplier(float gainMultiplier) {
      this.gainMultiplier = gainMultiplier;
      return this;
    }

    public Builder decayMultiplier(float decayMultiplier) {
      this.decayMultiplier = decayMultiplier;
      return this;
    }

    public Builder beneficial(boolean beneficial) {
      this.beneficial = beneficial;
      return this;
    }

    public String getName() {
      return this.name;
    }

    public Builder add(DietItemList list) {
      this.added.addAll(list);
      return this;
    }

    public Builder remove(DietItemList list) {
      this.removed.addAll(list);
      return this;
    }

    public IDietGroup build() {
      return new DietGroup(this);
    }
  }
}
