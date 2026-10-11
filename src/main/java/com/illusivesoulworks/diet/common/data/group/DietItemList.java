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

package com.illusivesoulworks.diet.common.data.group;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.platform.Services;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Port feature (new): item list used by the datapack group format ("items" / "remove" in
 * data/&lt;ns&gt;/diet/groups/*.json and data/&lt;ns&gt;/diet/group_entries/*.json).
 * Entries use the vanilla tag-entry syntax: "mod:item", "#mod:tag" or
 * {"id": "mod:item", "required": false}. Unknown optional entries are ignored silently,
 * unknown required item entries are logged. Tags are evaluated lazily, so the list works on
 * both sides and after tag reloads.
 */
public final class DietItemList {

  private final Set<Item> items = new HashSet<>();
  private final Set<TagKey<Item>> tags = new HashSet<>();
  // raw entries, kept for syncing/debugging
  private final List<String> raw = new ArrayList<>();

  public boolean isEmpty() {
    return this.items.isEmpty() && this.tags.isEmpty();
  }

  public boolean matches(ItemStack stack) {

    if (this.items.contains(stack.getItem())) {
      return true;
    }

    for (TagKey<Item> tag : this.tags) {

      if (stack.is(tag)) {
        return true;
      }
    }
    return false;
  }

  public void addAll(DietItemList other) {
    this.items.addAll(other.items);
    this.tags.addAll(other.tags);
    this.raw.addAll(other.raw);
  }

  public void add(String entry, boolean required, String source) {

    if (entry.startsWith("#")) {
      this.tags.add(TagKey.create(Registries.ITEM, Identifier.parse(entry.substring(1))));
      this.raw.add(entry);
    } else {
      Identifier id = Identifier.parse(entry);
      Services.REGISTRY.getItem(id).ifPresentOrElse(item -> {
        this.items.add(item);
        this.raw.add(entry);
      }, () -> {
        if (required) {
          DietConstants.LOG.warn("Unknown item {} in {} (mark it \"required\": false if it "
              + "belongs to an optional mod)", entry, source);
        }
      });
    }
  }

  public static DietItemList parse(JsonElement element, String source) {
    DietItemList list = new DietItemList();

    if (element == null || element.isJsonNull()) {
      return list;
    }
    JsonArray array;

    if (element.isJsonArray()) {
      array = element.getAsJsonArray();
    } else {
      array = new JsonArray();
      array.add(element);
    }

    for (JsonElement e : array) {

      if (e.isJsonPrimitive()) {
        list.add(e.getAsString(), true, source);
      } else if (e.isJsonObject()) {
        JsonObject obj = e.getAsJsonObject();

        if (!obj.has("id")) {
          throw new JsonParseException("Item entry without \"id\" in " + source);
        }
        boolean required = !obj.has("required") || obj.get("required").getAsBoolean();
        list.add(obj.get("id").getAsString(), required, source);
      } else {
        throw new JsonParseException("Invalid item entry " + e + " in " + source);
      }
    }
    return list;
  }

  public ListTag save() {
    ListTag list = new ListTag();

    for (String s : this.raw) {
      list.add(StringTag.valueOf(s));
    }
    return list;
  }

  public static DietItemList load(ListTag tag) {
    DietItemList list = new DietItemList();

    for (Tag t : tag) {
      t.asString().ifPresent(s -> list.add(s, false, "sync"));
    }
    return list;
  }
}
