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
 * - own Gson parsing on top of a SimplePreparableReloadListener (26.3 json listener is codec-
 *   based)
 */




package com.illusivesoulworks.diet.common.data.group;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.util.DietColor;
import com.illusivesoulworks.diet.common.util.DietValueGenerator;
import com.illusivesoulworks.diet.platform.Services;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class DietGroups extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {

  private static final Gson GSON =
      (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
  public static final Identifier ID = Identifier.fromNamespaceAndPath(DietConstants.MOD_ID, "groups");
  public static final DietGroups SERVER = Services.CAPABILITY.getGroupsListener();
  public static final DietGroups CLIENT = Services.CAPABILITY.getGroupsListener();

  private Map<String, IDietGroup> groups = new HashMap<>();

  public DietGroups() {
    // Port: 26.3's SimpleJsonResourceReloadListener is codec-based; Diet keeps its own Gson
    // parsing and only reuses the directory scan (same path: data/<namespace>/diet/groups/).
  }

  @Override
  protected Map<Identifier, JsonElement> prepare(@Nonnull ResourceManager resourceManager,
                                                 @Nonnull ProfilerFiller profilerFiller) {
    Map<Identifier, JsonElement> map = new HashMap<>();
    SimpleJsonResourceReloadListener.scanDirectory(resourceManager,
        FileToIdConverter.json("diet/groups"), JsonOps.INSTANCE, Codec.PASSTHROUGH.xmap(
            d -> d.convert(JsonOps.INSTANCE).getValue(), j -> new Dynamic<>(JsonOps.INSTANCE, j)),
        map);
    return map;
  }

  public static Set<IDietGroup> getGroups(Level level) {
    DietGroups instance = level.isClientSide() ? CLIENT : SERVER;
    return ImmutableSet.copyOf(instance.groups.values());
  }

  public static Optional<IDietGroup> getGroup(Level level, String name) {
    DietGroups instance = level.isClientSide() ? CLIENT : SERVER;
    return Optional.ofNullable(instance.groups.get(name));
  }

  public Set<IDietGroup> getGroups() {
    return ImmutableSet.copyOf(this.groups.values());
  }

  public Optional<IDietGroup> getGroup(String name) {
    return Optional.ofNullable(this.groups.get(name));
  }

  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();

    for (Map.Entry<String, IDietGroup> entry : this.groups.entrySet()) {
      tag.put(entry.getKey(), entry.getValue().save());
    }
    return tag;
  }

  public void load(CompoundTag tag) {
    Map<String, IDietGroup> loaded = new HashMap<>();

    for (String key : tag.keySet()) {
      loaded.put(key, DietGroup.load((CompoundTag) Objects.requireNonNull(tag.get(key))));
    }
    this.groups = loaded;
  }

  public void sync(ServerPlayer player) {
    Map<Item, Set<String>> items = new HashMap<>();

    for (Map.Entry<Item, Set<IDietGroup>> entry : DietValueGenerator.getAll().entrySet()) {
      Set<String> groups = new HashSet<>();

      for (IDietGroup group : entry.getValue()) {
        groups.add(group.getName());
      }
      items.put(entry.getKey(), groups);
    }
    Services.NETWORK.sendDietGroupsS2C(player, this.save(), items);
  }

  @Override
  protected void apply(@Nonnull Map<Identifier, JsonElement> object,
                       @Nonnull ResourceManager resourceManager,
                       @Nonnull ProfilerFiller profilerFiller) {
    Map<String, DietGroup.Builder> map = new HashMap<>();

    for (Map.Entry<Identifier, JsonElement> entry : object.entrySet()) {
      Identifier resourcelocation = entry.getKey();

      if (resourcelocation.getNamespace().equals(DietConstants.MOD_ID)) {

        try {
          buildGroup(map.computeIfAbsent(resourcelocation.getPath(), DietGroup.Builder::new),
              GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
        } catch (IllegalArgumentException | JsonParseException e) {
          DietConstants.LOG.error("Parsing error loading diet group {}", resourcelocation, e);
        }
      }
    }

    for (Map.Entry<Identifier, JsonElement> entry : object.entrySet()) {
      Identifier resourcelocation = entry.getKey();

      if (resourcelocation.getPath().startsWith("_") ||
          resourcelocation.getNamespace().equals(DietConstants.MOD_ID)) {
        continue;
      }

      try {
        buildGroup(map.computeIfAbsent(resourcelocation.getPath(), DietGroup.Builder::new),
            GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
      } catch (IllegalArgumentException | JsonParseException e) {
        DietConstants.LOG.error("Parsing error loading diet group {}", resourcelocation, e);
      }
    }
    groups = map.entrySet().stream()
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().build()));
    DietConstants.LOG.info("Loaded {} diet groups", map.size());
  }

  private void buildGroup(DietGroup.Builder builder, JsonObject topElement) {

    if (topElement.has("icon")) {
      String icon = GsonHelper.getAsString(topElement, "icon");
      Services.REGISTRY.getItem(Identifier.parse(icon)).ifPresent(builder::icon);
    }

    if (topElement.has("color")) {
      String color = GsonHelper.getAsString(topElement, "color");
      int col;

      if (color.startsWith("#")) {
        col = Integer.parseInt(color.substring(1), 16);
      } else {
        col = Integer.parseInt(color);
      }
      int r = ((col >> 16) & 0xff);
      int g = ((col >> 8) & 0xff);
      int b = ((col) & 0xff);
      builder.color(new DietColor(r, g, b));
    }

    if (topElement.has("order")) {
      builder.order(GsonHelper.getAsInt(topElement, "order"));
    }

    if (topElement.has("default_value")) {
      builder.defaultValue(GsonHelper.getAsFloat(topElement, "default_value"));
    }

    if (topElement.has("gain_multiplier")) {
      builder.gainMultiplier(GsonHelper.getAsFloat(topElement, "gain_multiplier"));
    }

    if (topElement.has("decay_multiplier")) {
      builder.decayMultiplier(GsonHelper.getAsFloat(topElement, "decay_multiplier"));
    }

    if (topElement.has("beneficial")) {
      builder.beneficial(GsonHelper.getAsBoolean(topElement, "beneficial"));
    }
  }
}
