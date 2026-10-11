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
 * - BuiltInRegistries, holders, legacy attribute ids, food data component
 */




package com.illusivesoulworks.diet.platform.services;

import com.illusivesoulworks.diet.DietCommonMod;
import com.illusivesoulworks.diet.common.util.DietOverride;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.server.command.ModIdArgument;
import org.apache.commons.lang3.tuple.Triple;

public class ForgeRegistryService implements IRegistryService {

  private static final TagKey<Item> INGREDIENTS =
      TagKey.create(Registries.ITEM, DietCommonMod.resource("ingredients"));
  private static final TagKey<Item> SPECIAL_FOOD =
      TagKey.create(Registries.ITEM, DietCommonMod.resource("special_food"));

  @Override
  public Optional<Item> getItem(Identifier resourceLocation) {
    return BuiltInRegistries.ITEM.getOptional(resourceLocation);
  }

  @Override
  public Identifier getItemKey(Item item) {
    return BuiltInRegistries.ITEM.getKey(item);
  }

  @Override
  public Optional<Holder<Attribute>> getAttribute(Identifier resourceLocation) {
    Optional<Holder<Attribute>> result =
        BuiltInRegistries.ATTRIBUTE.get(resourceLocation).map(h -> (Holder<Attribute>) h);

    // Port: 1.21.2 dropped the "generic."/"player."/"zombie." prefixes of vanilla attribute ids
    // (minecraft:generic.max_health -> minecraft:max_health). Old datapack ids keep working.
    if (result.isEmpty()) {
      String path = resourceLocation.getPath();
      int dot = path.indexOf('.');

      if (dot > 0) {
        String prefix = path.substring(0, dot);

        if (prefix.equals("generic") || prefix.equals("player") || prefix.equals("zombie")) {
          result = BuiltInRegistries.ATTRIBUTE.get(
                  Identifier.fromNamespaceAndPath(resourceLocation.getNamespace(),
                      path.substring(dot + 1)))
              .map(h -> (Holder<Attribute>) h);
        }
      }
    }
    return result;
  }

  @Override
  public Identifier getAttributeKey(Holder<Attribute> attribute) {
    return attribute.unwrapKey().map(k -> k.identifier())
        .orElseGet(() -> BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()));
  }

  @Override
  public Optional<Holder<MobEffect>> getStatusEffect(Identifier resourceLocation) {
    return BuiltInRegistries.MOB_EFFECT.get(resourceLocation).map(h -> (Holder<MobEffect>) h);
  }

  @Override
  public Identifier getStatusEffectKey(Holder<MobEffect> effect) {
    return effect.unwrapKey().map(k -> k.identifier())
        .orElseGet(() -> BuiltInRegistries.MOB_EFFECT.getKey(effect.value()));
  }

  @Override
  public FoodProperties getFoodProperties(ItemStack stack, Player player) {
    // Port: food is the minecraft:food data component in 26.3.
    return stack.get(DataComponents.FOOD);
  }

  @Override
  public BiFunction<Player, ItemStack, Triple<List<ItemStack>, Integer, Float>> getOverride(
      Item item) {
    return DietOverride.get(item);
  }

  @Override
  public boolean isIngredient(ItemStack stack) {
    return stack.is(INGREDIENTS);
  }

  @Override
  public boolean isSpecialFood(ItemStack stack) {
    return stack.is(SPECIAL_FOOD);
  }

  @Override
  public Collection<Item> getItems() {
    List<Item> items = new ArrayList<>();
    BuiltInRegistries.ITEM.forEach(items::add);
    return items;
  }

  @Override
  public Collection<Item> getTagItems(TagKey<Item> tagKey) {
    List<Item> items = new ArrayList<>();
    BuiltInRegistries.ITEM.getTagOrEmpty(tagKey).forEach(holder -> items.add(holder.value()));
    return items;
  }

  @Override
  public ItemStack getPickStack(BlockState state, BlockHitResult result, Level world, BlockPos pos,
                                ServerPlayer player) {
    return state.getCloneItemStack(pos, world, false, player);
  }

  @Override
  public ArgumentType<String> getModIdArgument() {
    return ModIdArgument.modIdArgument();
  }
}
