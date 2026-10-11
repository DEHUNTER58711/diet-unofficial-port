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
 * - Forge capability -> NeoForge data attachment diet:diet_tracker
 */




package com.illusivesoulworks.diet.common.capability;

import net.minecraft.core.Holder;
import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Port: the Forge capability was replaced by a NeoForge data attachment with the same id
 * (diet:diet_tracker). The stored NBT layout of the tracker (group values, Modifiers, Eaten,
 * Active) is unchanged; it is nested under the key "Tracker" of the attachment.
 * Copying on respawn/dimension change is done explicitly in PlayerEvent.Clone (with the
 * configurable death penalty), syncing to the client on login/respawn/dimension change.
 */
public class DietCapability {

  public static final Identifier DIET_TRACKER_ID =
      Identifier.fromNamespaceAndPath(DietConstants.MOD_ID, "diet_tracker");

  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DietConstants.MOD_ID);

  public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerDietTracker>>
      DIET_TRACKER = ATTACHMENTS.register(DIET_TRACKER_ID.getPath(),
      () -> AttachmentType.builder(DietCapability::create).serialize(new Serializer()).build());

  public static void register(IEventBus modBus) {
    ATTACHMENTS.register(modBus);
  }

  private static PlayerDietTracker create(IAttachmentHolder holder) {

    if (holder instanceof Player player) {
      return new PlayerDietTracker(player);
    }
    throw new IllegalStateException("Diet tracker can only be attached to players, not " + holder);
  }

  public static Optional<IDietTracker> get(final Player player) {

    if (player == null) {
      return Optional.empty();
    }
    return Optional.of(player.getData(DIET_TRACKER));
  }

  private static final class Serializer implements IAttachmentSerializer<PlayerDietTracker> {

    @Override
    public PlayerDietTracker read(IAttachmentHolder holder, ValueInput input) {
      PlayerDietTracker tracker = create(holder);
      input.read("Tracker", CompoundTag.CODEC).ifPresent(tracker::load);
      return tracker;
    }

    @Override
    public boolean write(PlayerDietTracker attachment, ValueOutput output) {
      CompoundTag tag = new CompoundTag();
      attachment.save(tag);
      output.store("Tracker", CompoundTag.CODEC, tag);
      return true;
    }
  }
}
