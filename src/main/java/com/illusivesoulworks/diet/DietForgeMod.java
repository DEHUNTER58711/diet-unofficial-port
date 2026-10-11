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
 * - NeoForge mod constructor, DeferredRegister for attribute/argument type/attachment,
 *   SYNCED/CLIENT config registration, AddServerReloadListenersEvent with explicit
 *   groups->suites order; datagen removed (tags shipped as JSON)
 */




package com.illusivesoulworks.diet;

import net.minecraft.core.Holder;
import com.illusivesoulworks.diet.common.DietEvents;
import com.illusivesoulworks.diet.common.capability.DietCapability;
import com.illusivesoulworks.diet.common.capability.DietCapabilityEventsListener;
import com.illusivesoulworks.diet.common.command.DietCommand;
import com.illusivesoulworks.diet.common.command.DietGroupArgument;
import com.illusivesoulworks.diet.common.config.DietConfig;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import com.illusivesoulworks.diet.common.network.DietForgeNetwork;
import com.illusivesoulworks.diet.common.util.DietOverride;
import com.illusivesoulworks.diet.common.util.DietValueGenerator;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DietConstants.MOD_ID)
public class DietForgeMod {

  private static final DeferredRegister<Attribute> ATTRIBUTES =
      DeferredRegister.create(Registries.ATTRIBUTE, DietConstants.MOD_ID);
  private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
      DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, DietConstants.MOD_ID);

  // Port: same registry id as upstream (diet:natural_regeneration). The attribute is now created
  // here (registered holder) instead of as a static field in DietApiImpl, because 26.3 works
  // with Holder<Attribute> everywhere.
  public static final DeferredHolder<Attribute, Attribute> NATURAL_REGEN =
      ATTRIBUTES.register("natural_regeneration",
          () -> new RangedAttribute("diet.naturalRegeneration", 1.0d, 0.0d, 2.0d)
              .setSyncable(true));

  // Port: argument types must be registered in the registry on NeoForge (was registerByClass in
  // common setup). The id diet:group is new; upstream used an unnamed registration.
  public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> GROUP_ARGUMENT =
      ARGUMENT_TYPES.register("group", () -> ArgumentTypeInfos.registerByClass(
          DietGroupArgument.class, SingletonArgumentInfo.contextFree(DietGroupArgument::group)));

  public DietForgeMod(IEventBus eventBus, ModContainer container) {
    eventBus.addListener(this::process);
    eventBus.addListener(this::modifyAttributes);
    eventBus.addListener(this::configChanged);
    eventBus.addListener(this::configLoaded);
    eventBus.addListener(DietForgeNetwork::register);
    container.registerConfig(ModConfig.Type.CLIENT, DietConfig.CLIENT_SPEC, "diet-client.toml");
    // Port: SpectreLib server config -> NeoForge SYNCED config (config/diet-server.toml, synced
    // to clients, same file name and keys as before).
    container.registerConfig(ModConfig.Type.SYNCED, DietConfig.SERVER_SPEC, "diet-server.toml");
    ATTRIBUTES.register(eventBus);
    ARGUMENT_TYPES.register(eventBus);
    DietCapability.register(eventBus);
    NeoForge.EVENT_BUS.addListener(this::serverStarting);
    NeoForge.EVENT_BUS.addListener(this::onDatapackSync);
    NeoForge.EVENT_BUS.addListener(this::setupCommands);
    NeoForge.EVENT_BUS.addListener(this::addReloaders);
    DietCapabilityEventsListener.register(NeoForge.EVENT_BUS);
  }

  private void configChanged(final ModConfigEvent.Reloading evt) {
    if (evt.getConfig().getSpec() == DietConfig.SERVER_SPEC) {
      DietConfig.SERVER.initializedFoodOverrides = false;
    }
  }

  private void configLoaded(final ModConfigEvent.Loading evt) {
    if (evt.getConfig().getSpec() == DietConfig.SERVER_SPEC) {
      DietConfig.SERVER.initializedFoodOverrides = false;
    }
  }

  private void addReloaders(final AddServerReloadListenersEvent evt) {
    // Port feature: group, group entry and suite files support "neoforge:conditions".
    ConditionalOps<JsonElement> ops = new ConditionalOps<>(
        RegistryOps.create(JsonOps.INSTANCE, evt.getServerResources().getRegistryLookup()), evt.getConditionContext());
    DietGroups.SERVER.setConditionOps(ops);
    DietSuites.SERVER.setConditionOps(ops);
    evt.addListener(DietGroups.ID, DietGroups.SERVER);
    evt.addListener(DietSuites.ID, DietSuites.SERVER);
    // Suites resolve group names while loading, so groups must be applied first.
    evt.addDependency(DietGroups.ID, DietSuites.ID);
  }

  private void process(final InterModProcessEvent evt) {
    DietOverride.process(evt.getIMCStream());
  }

  private void modifyAttributes(final EntityAttributeModificationEvent evt) {
    evt.add(EntityTypes.PLAYER, NATURAL_REGEN);
  }

  private void serverStarting(final ServerStartingEvent evt) {
    DietValueGenerator.reload(evt.getServer());
  }

  private void onDatapackSync(final OnDatapackSyncEvent evt) {

    if (evt.getPlayer() == null) {
      DietEvents.syncDatapack(evt.getPlayerList().getServer());
    } else {
      DietEvents.syncDatapack(evt.getPlayer());
    }
  }

  private void setupCommands(final RegisterCommandsEvent evt) {
    DietCommand.register(evt.getDispatcher());
  }
}
