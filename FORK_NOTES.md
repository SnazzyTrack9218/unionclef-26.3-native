# Gathering and equipment fixes — 1.0.2-snazz.1+26.3

## Crafting improvements in 1.0.3

- Refresh the recipe cache when Minecraft replaces the recipe-book collection
  snapshot, including recipes unlocked after joining. Clear obsolete entries on
  rebuild and preserve every valid ingredient alternative instead of only the
  first wood/item type. Missing ingredient displays are not treated as free slots.
- Keep three- and four-ingredient shapeless recipes in the 2x2 inventory grid.
- Select a craftable recipe for the requested output and current grid size without
  recalculating the entire recipe book's UI state. Include ingredients already
  staged in the grid when checking availability.
- Inventory recipe-book crafting waits for its request, preserves the active grid,
  and uses the table crafter's existing 1.5-second manual fallback if no output
  arrives. Its manual child keeps control of its cursor and grid. An open table
  is no longer replaced by the inventory screen.
- Manual crafting batches use the number of outputs still needed, rather than
  repeating the full absolute target when some are already owned.
- Material checks inspect all nine actual table inputs even for 2x2 recipes;
  previously that scan skipped some inputs and inspected inventory slots instead.

The local installation enables recipe-book crafting to reduce individual clicks;
manual placement remains the fallback. Existing click pacing is preserved. This
is a configuration change outside the jar and is backed up with the installation.

Validation: 39 JUnit tests passed, including real Minecraft 26.3 display parsing,
recipe selection, recipe-book snapshot replacement, manual batch sizing and the
previous equipment/food/pickup regressions. No live survival playthrough has been
completed with this release, so these checks do not establish a timing improvement
or prove that all navigation stalls are fixed.

## Gathering fixes in 1.0.2

The Baritone pickup branch returned before the existing inventory, progress and
unreachable-target checks. A stalled pickup could therefore run indefinitely,
and its follow process continued even after the parent switched to crafting.

- Apply the existing 25-second no-progress and 40-second pursuit budgets to
  Baritone pickups. Track actual closing distance per entity across task rebuilds
  and target changes; path-search restarts do not count as progress.
- Bind the pickup filter to the selected live entity, cancel it on task exit,
  handle full inventories, and use the existing tracker cool-off when a target
  exhausts its budget. This applies to every dropped resource, not only hay.
- Return control to the gatherer when no reachable drops remain, rather than
  keeping an empty pickup task active forever. Ignore removed item entities.
- Food collection makes bread from partial available ingredients before going
  back for the remainder of its requested target. Wheat and hay are counted once,
  whole-recipe quantities are used, and existing wheat/bread are included in the
  absolute crafting targets.
- A food pickup requests additional items instead of a target already satisfied
  by ingredients in the inventory. Respect the source search radius for drops,
  and let unavailable pickups yield to harvestable blocks.

These changes do not reduce the requested amount of food, alter navigation
distances, speed up vanilla crafting/smelting, or change server settings.
Regression tests cover both the pickup progress model and bread conversion
planning, alongside the earlier equipment tests. Live unattended survival
performance remains unverified.

Based on smallduuude/unionclef-26.3-native at
`41d7c01f74dda8440b4129f3d737a3f70eea6103` (UnionClef 1.0.0 for Minecraft 26.3).
Original authorship, GPL-3.0 license, bundled components and their notices are retained.

## Behavior

- Self-Care accepts the same tool type at an equal or higher tier instead of
  requiring a wooden, stone, iron, diamond and netherite copy of every tool.
- Each role is checked independently. A netherite pickaxe does not stand in for
  a missing sword, axe or shovel; mixed-tier sets are supported.
- Only missing tools and armor slots are requested. Equipped better armor is
  preserved, and better armor already in the inventory is preferred over
  crafting weaker armor.
- Equipment targets are re-evaluated while working so newly acquired upgrades
  cancel obsolete lower-tier crafting targets.
- Beat the Game recognizes netherite throughout gear preparation, crafting,
  loot recovery and its repeated replacement-pickaxe checks. It no longer
  crafts or recovers weaker tools when a suitable upgrade is already owned.
- Exact resource and crafting requests remain exact. Gold helmets still satisfy
  the piglin protection requirement only when they are actually gold.

The fix covers vanilla wood, stone, iron, diamond and netherite tool progression
and iron, diamond and netherite armor. It does not infer tiers for arbitrary
modded equipment. These equipment rules do not add support for arbitrary modded tool tiers.

## Build and checks

Use JDK 25:

```powershell
.\gradlew.bat build
```

The upstream checkout omitted `gradle-wrapper.jar`. This fork includes the
official Gradle 9.7.1 wrapper and pins its distribution checksum, allowing a
fresh checkout to build without a separately installed Gradle.

JUnit tests bootstrap Minecraft's real item registry and cover full netherite
sets, mixed tiers, missing roles, lower-tier rejection, mixed armor, choosing
the best owned armor, upgrades arriving during a task, and exact gold/resource
requirements. These tests do not claim an unattended survival playthrough.

## Install or roll back

Keep one enabled UnionClef jar in the instance's `mods` folder. This fork retains
the `altoclef` mod ID and the bundled Baritone dependency, and uses the existing
configuration. Quit and relaunch Minecraft after replacing the jar.

To roll back, disable this fork's jar and re-enable the saved 1.0.0 jar. No world,
player inventory or server data needs to be modified.
