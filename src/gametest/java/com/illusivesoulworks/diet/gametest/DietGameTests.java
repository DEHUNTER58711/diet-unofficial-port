/*
 * Unofficial NeoForge 26.3 port of Diet - GameTests (new file).
 * Copyright (C) 2026 DEHunter58711. Licensed under the GNU Lesser General Public License v3.0
 * or (at your option) any later version, like Diet itself.
 *
 * These tests are NOT part of the release jar; they are only built into the "gametest" jar
 * used on the test server (see build.gradle).
 */

package com.illusivesoulworks.diet.gametest;

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.DietForgeMod;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.common.capability.DietCapability;
import com.illusivesoulworks.diet.common.capability.PlayerDietTracker;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import com.illusivesoulworks.diet.common.util.DietValueGenerator;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DietGameTests.MOD_ID)
public class DietGameTests {

  public static final String MOD_ID = "diet_gametest";

  private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
      DeferredRegister.create(Registries.TEST_FUNCTION, DietConstants.MOD_ID);

  private static final String[] TESTS =
      {"datapack_loaded", "eat_bread", "eat_cake_block", "save_reload", "effects_at_threshold",
          "death_penalty", "clone_without_death", "natural_regen_attribute", "decay", "dump_foods", "group_entries", "death_penalty_disabled",
          "suite_sync_roundtrip"};

  public DietGameTests(IEventBus bus) {
    FUNCTIONS.register("datapack_loaded", () -> DietGameTests::datapackLoaded);
    FUNCTIONS.register("eat_bread", () -> DietGameTests::eatBread);
    FUNCTIONS.register("eat_cake_block", () -> DietGameTests::eatCakeBlock);
    FUNCTIONS.register("save_reload", () -> DietGameTests::saveReload);
    FUNCTIONS.register("effects_at_threshold", () -> DietGameTests::effectsAtThreshold);
    FUNCTIONS.register("death_penalty", () -> DietGameTests::deathPenalty);
    FUNCTIONS.register("clone_without_death", () -> DietGameTests::cloneWithoutDeath);
    FUNCTIONS.register("natural_regen_attribute", () -> DietGameTests::naturalRegen);
    FUNCTIONS.register("decay", () -> DietGameTests::decay);
    FUNCTIONS.register("dump_foods", () -> DietGameTests::dumpFoods);
    FUNCTIONS.register("group_entries", () -> DietGameTests::groupEntries);
    FUNCTIONS.register("death_penalty_disabled", () -> DietGameTests::deathPenaltyDisabled);
    FUNCTIONS.register("suite_sync_roundtrip", () -> DietGameTests::suiteSyncRoundtrip);
    FUNCTIONS.register(bus);
    bus.addListener(DietGameTests::registerTests);
  }

  private static void registerTests(RegisterGameTestsEvent evt) {
    Holder<TestEnvironmentDefinition<?>> env = evt.registerEnvironment(
        Identifier.fromNamespaceAndPath(DietConstants.MOD_ID, "default"),
        new TestEnvironmentDefinition.AllOf());

    for (String name : TESTS) {
      Identifier id = Identifier.fromNamespaceAndPath(DietConstants.MOD_ID, name);
      evt.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
          new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 100, 0,
              true, Rotation.NONE, false, 1, 1, false, 0)));
    }
  }

  private static ServerPlayer player(GameTestHelper helper) {
    return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
  }

  private static IDietTracker tracker(GameTestHelper helper, ServerPlayer player) {
    return DietCapability.get(player).orElseThrow(() -> helper.assertionException("no tracker"));
  }

  private static void datapackLoaded(GameTestHelper helper) {
    helper.assertTrue(DietGroups.SERVER.getGroups().size() == 5,
        "expected 5 groups, got " + DietGroups.SERVER.getGroups().size());
    helper.assertTrue(DietSuites.getSuite(helper.getLevel(), "builtin").isPresent(),
        "builtin suite missing");
    helper.assertTrue(DietSuites.getSuite(helper.getLevel(), "builtin").get().getEffects().size() >= 3,
        "builtin suite effects missing (attribute ids?)");
    IDietGroup fruits = DietGroups.SERVER.getGroup("fruits").orElseThrow();
    helper.assertTrue(fruits.contains(new ItemStack(Items.APPLE)), "apple not in #diet:fruits");
    // cooked beef is not tagged; its group comes from the recipe-based value generator
    helper.assertTrue(DietApi.getInstance().getGroups(player(helper), new ItemStack(Items.COOKED_BEEF))
        .stream().anyMatch(g -> g.getName().equals("proteins")), "cooked beef not a protein");
    helper.assertTrue(!DietValueGenerator.getAll().isEmpty(), "no generated values");
    helper.succeed();
  }

  private static void eat(ServerPlayer player, ItemStack stack) {
    ItemStack copy = stack.copy();
    ItemStack result = stack.finishUsingItem(player.level(), player);
    NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, copy, 0, result));
  }

  private static void eatBread(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    IDietTracker diet = tracker(helper, player);
    player.getFoodData().setFoodLevel(10);
    diet.tick();
    eat(player, new ItemStack(Items.BREAD));
    helper.assertTrue(player.getFoodData().getFoodLevel() > 10, "bread did not feed");
    float grains = diet.getValue("grains");
    helper.assertTrue(grains > 0.0f, "grains did not increase: " + grains);
    helper.assertTrue(diet.getValue("proteins") == 0.0f, "proteins changed");
    helper.assertTrue(diet.getEaten().contains(Items.BREAD), "bread not recorded as eaten");
    helper.succeed();
  }

  private static void eatCakeBlock(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    IDietTracker diet = tracker(helper, player);
    player.getFoodData().setFoodLevel(10);
    diet.tick();
    // Same path as the block-use mixin: capture the pick stack, then FoodData#eat(int, float).
    diet.captureStack(new ItemStack(Items.CAKE));
    player.getFoodData().eat(2, 0.1F);
    helper.assertTrue(diet.getValue("sugars") > 0.0f, "cake did not add sugars");
    helper.assertTrue(diet.getCapturedStack().isEmpty(), "captured stack not cleared");
    helper.succeed();
  }

  private static void saveReload(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    IDietTracker diet = tracker(helper, player);
    diet.setValue("fruits", 0.42f);
    diet.setActive(false);
    diet.addEaten(Items.APPLE);
    CompoundTag tag = new CompoundTag();
    diet.save(tag);
    PlayerDietTracker copy = new PlayerDietTracker(player(helper));
    copy.load(tag);
    helper.assertTrue(Math.abs(copy.getValue("fruits") - 0.42f) < 1e-4, "fruits not restored");
    helper.assertTrue(!copy.isActive(), "active flag not restored");
    helper.assertTrue(copy.getEaten().contains(Items.APPLE), "eaten not restored");
    helper.succeed();
  }

  @SuppressWarnings("removal")
  private static void effectsAtThreshold(GameTestHelper helper) {
    // needs a connection because vanilla sends effect packets in addEffect
    ServerPlayer player = helper.makeMockServerPlayerInLevel();
    player.setGameMode(GameType.SURVIVAL);
    IDietTracker diet = tracker(helper, player);
    AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
    double before = maxHealth.getValue();

    for (String g : new String[] {"proteins", "fruits", "vegetables", "grains", "sugars"}) {
      diet.setValue(g, 0.9f);
    }
    player.tickCount = 80;
    diet.tick();
    helper.assertTrue(maxHealth.getValue() > before,
        "max health did not increase: " + before + " -> " + maxHealth.getValue());
    helper.assertTrue(maxHealth.getModifiers().stream()
        .anyMatch(m -> m.id().getNamespace().equals(DietConstants.MOD_ID)), "no diet modifier");
    helper.assertTrue(player.hasEffect(MobEffects.HUNGER), "sugar > 80% did not give hunger");
    // Below threshold: modifiers are removed at the next 80-tick refresh.
    for (String g : new String[] {"proteins", "fruits", "vegetables", "grains", "sugars"}) {
      diet.setValue(g, 0.5f);
    }
    player.tickCount = 160;
    diet.tick();
    helper.assertTrue(maxHealth.getModifiers().stream()
            .noneMatch(m -> m.id().getNamespace().equals(DietConstants.MOD_ID)),
        "diet modifier not removed");
    helper.succeed();
  }

  private static void deathPenalty(GameTestHelper helper) {
    ServerPlayer old = player(helper);
    tracker(helper, old).setValue("fruits", 0.5f);
    ServerPlayer respawned = player(helper);
    tracker(helper, respawned).copy(old, true);
    // default config: AMOUNT, loss 100 %, min 0 % -> reset to 0
    helper.assertTrue(tracker(helper, respawned).getValue("fruits") == 0.0f,
        "death penalty not applied: " + tracker(helper, respawned).getValue("fruits"));
    helper.succeed();
  }

  private static void cloneWithoutDeath(GameTestHelper helper) {
    ServerPlayer old = player(helper);
    tracker(helper, old).setValue("fruits", 0.5f);
    ServerPlayer moved = player(helper);
    tracker(helper, moved).copy(old, false);
    helper.assertTrue(Math.abs(tracker(helper, moved).getValue("fruits") - 0.5f) < 1e-4,
        "values lost on non-death clone (dimension change / End return)");
    helper.succeed();
  }

  private static void naturalRegen(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    helper.assertTrue(player.getAttribute(DietForgeMod.NATURAL_REGEN) != null,
        "diet:natural_regeneration not on player");
    helper.succeed();
  }

  // QA helper: with -Ddiet.dumpFoods=<file>, writes every food item and its Diet groups
  // (tagged / generated / none). Used to build and verify the mod-support data.
  private static void dumpFoods(GameTestHelper helper) {
    String file = System.getProperty("diet.dumpFoods");

    if (file != null && !file.isEmpty()) {
      ServerPlayer player = player(helper);
      StringBuilder sb = new StringBuilder();
      net.minecraft.core.registries.BuiltInRegistries.ITEM.forEach(item -> {
        ItemStack stack = new ItemStack(item);
        var food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        boolean special = stack.is(net.minecraft.tags.TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(DietConstants.MOD_ID, "special_food")));

        if (food != null || special) {
          java.util.List<String> tagged = new java.util.ArrayList<>();

          for (IDietGroup g : DietGroups.SERVER.getGroups()) {
            if (g.contains(stack)) {
              tagged.add(g.getName());
            }
          }
          java.util.List<String> all = DietApi.getInstance().getGroups(player, stack).stream()
              .map(IDietGroup::getName).sorted().toList();
          sb.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)).append('\t')
              .append(food == null ? "-" : food.nutrition()).append('\t')
              .append(tagged.isEmpty() ? (all.isEmpty() ? "NONE" : "generated") : "tagged").append('\t')
              .append(String.join(",", all)).append('\n');
        }
      });
      try {
        java.nio.file.Files.writeString(java.nio.file.Path.of(file), sb.toString());
      } catch (java.io.IOException e) {
        throw helper.assertionException("cannot write " + file);
      }
    }
    helper.succeed();
  }

  // datapack format: group_entries add/remove (#227) and neoforge:conditions
  private static void groupEntries(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    java.util.Set<String> carrot = DietApi.getInstance()
        .getGroups(player, new ItemStack(Items.GOLDEN_CARROT)).stream()
        .map(IDietGroup::getName).collect(java.util.stream.Collectors.toSet());
    helper.assertTrue(carrot.contains("fruits") && carrot.contains("vegetables"),
        "golden carrot should be fruits+vegetables, got " + carrot);
    java.util.Set<String> cookie = DietApi.getInstance()
        .getGroups(player, new ItemStack(Items.COOKIE)).stream()
        .map(IDietGroup::getName).collect(java.util.stream.Collectors.toSet());
    helper.assertTrue(!cookie.contains("sugars") && cookie.contains("grains"),
        "removed group still generated for cookie: " + cookie);
    helper.assertTrue(!DietGroups.SERVER.getGroup("proteins").orElseThrow()
        .contains(new ItemStack(Items.APPLE)), "conditional entry file was applied");
    // synced form keeps the entries
    IDietGroup synced = com.illusivesoulworks.diet.common.data.group.DietGroup.load(
        DietGroups.SERVER.getGroup("sugars").orElseThrow().save());
    helper.assertTrue(synced.isExcluded(new ItemStack(Items.COOKIE)), "removal not synced");
    helper.succeed();
  }

  // #219: with deathPenaltyLoss = 0 nothing may be lost on death
  private static void deathPenaltyDisabled(GameTestHelper helper) {
    var loss = com.illusivesoulworks.diet.common.config.DietConfig.SERVER.deathPenaltyLoss;
    int previous = loss.get();
    loss.set(0);

    try {
      ServerPlayer old = player(helper);
      tracker(helper, old).setValue("fruits", 0.5f);
      ServerPlayer respawned = player(helper);
      tracker(helper, respawned).copy(old, true);
      helper.assertTrue(Math.abs(tracker(helper, respawned).getValue("fruits") - 0.5f) < 1e-4,
          "value lost on death although deathPenaltyLoss = 0: "
              + tracker(helper, respawned).getValue("fruits"));
    } finally {
      loss.set(previous);
    }
    helper.succeed();
  }

  // effects (with titles/conditions) are now part of the suite sync
  private static void suiteSyncRoundtrip(GameTestHelper helper) {
    var suite = DietSuites.getSuite(helper.getLevel(), "builtin").orElseThrow();
    var copy = com.illusivesoulworks.diet.common.data.suite.DietSuite.load(suite.save());
    helper.assertTrue(copy.getEffects().size() == suite.getEffects().size(), "effects not synced");
    var sugar = copy.getEffects().get(2);
    helper.assertTrue(sugar.getTitle().orElse("").equals("diet.effect.sugar"), "title not synced");
    helper.assertTrue(sugar.getConditions().get(0).getGroups().contains("sugars"),
        "condition groups not synced");
    helper.assertTrue(Math.abs(sugar.getConditions().get(0).getAbove() - 0.8) < 1e-6,
        "condition range not synced");
    helper.assertTrue(sugar.getStatusEffects().get(0).getEffect().is(MobEffects.HUNGER),
        "status effect not synced");
    helper.succeed();
  }

  private static void decay(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    IDietTracker diet = tracker(helper, player);
    diet.setValue("fruits", 0.5f);
    player.getFoodData().setFoodLevel(20);
    diet.tick();
    player.getFoodData().setFoodLevel(10);
    diet.tick();
    helper.assertTrue(diet.getValue("fruits") < 0.5f, "no decay after losing hunger");
    helper.succeed();
  }
}
