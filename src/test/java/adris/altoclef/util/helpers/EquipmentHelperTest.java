package adris.altoclef.util.helpers;

import adris.altoclef.util.ItemTarget;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentHelperTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void netheriteToolsSatisfyEveryEarlierTier() {
        Set<Item> inventory = Set.of(Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL,
                Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_HOE);
        for (Item[] tier : new Item[][]{ItemHelper.WOODEN_TOOLS, ItemHelper.STONE_TOOLS,
                ItemHelper.IRON_TOOLS, ItemHelper.DIAMOND_TOOLS, ItemHelper.NETHERITE_TOOLS}) {
            assertEquals(0, EquipmentHelper.missingEquipment(inventory::contains, tier).length);
        }
    }

    @Test
    void mixedToolTiersSatisfyTheMatchingRoles() {
        Set<Item> inventory = Set.of(Items.NETHERITE_PICKAXE, Items.DIAMOND_SWORD,
                Items.IRON_AXE, Items.STONE_SHOVEL);
        assertEquals(0, EquipmentHelper.missingEquipment(inventory::contains,
                Items.STONE_PICKAXE, Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_SHOVEL).length);
        ItemTarget[] missing = EquipmentHelper.missingEquipment(inventory::contains,
                Items.IRON_PICKAXE, Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_SHOVEL);
        assertEquals(1, missing.length);
        assertArrayEquals(new Item[]{Items.IRON_SHOVEL}, missing[0].getMatches());
    }

    @Test
    void oneToolCannotStandInForAnEntireSet() {
        ItemTarget[] missing = EquipmentHelper.missingEquipment(Set.of(Items.NETHERITE_PICKAXE)::contains,
                Items.WOODEN_PICKAXE, Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_SHOVEL);
        assertEquals(3, missing.length);
        assertTrue(Arrays.stream(missing).noneMatch(target -> target.matches(Items.WOODEN_PICKAXE)));
    }

    @Test
    void lowerTiersAndDifferentToolTypesDoNotSatisfyRequirements() {
        assertFalse(EquipmentHelper.hasAtLeast(Set.of(Items.IRON_PICKAXE)::contains, Items.DIAMOND_PICKAXE));
        assertFalse(EquipmentHelper.hasAtLeast(Set.of(Items.NETHERITE_AXE)::contains, Items.WOODEN_PICKAXE));
        assertFalse(EquipmentHelper.hasAtLeast(Set.of(Items.GOLDEN_PICKAXE)::contains, Items.IRON_PICKAXE));
        assertFalse(EquipmentHelper.hasAtLeast(Set.of(Items.NETHERITE_SWORD)::contains, Items.WOODEN_AXE));
    }

    @Test
    void equippedMixedArmorDoesNotNeedWeakerReplacements() {
        Set<Item> equipped = Set.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_LEGGINGS,
                Items.IRON_HELMET, Items.NETHERITE_BOOTS);
        assertEquals(0, EquipmentHelper.missingEquipment(equipped::contains, ItemHelper.IRON_ARMORS).length);
        ItemTarget[] missing = EquipmentHelper.missingEquipment(equipped::contains, ItemHelper.DIAMOND_ARMORS);
        assertEquals(1, missing.length);
        assertTrue(missing[0].matches(Items.DIAMOND_HELMET));
    }

    @Test
    void equipTheBestOwnedItemWithoutDowngradingOtherSlots() {
        Set<Item> inventory = Set.of(Items.IRON_CHESTPLATE, Items.NETHERITE_CHESTPLATE, Items.DIAMOND_BOOTS);
        assertSame(Items.NETHERITE_CHESTPLATE, EquipmentHelper.bestAvailable(inventory::contains, Items.IRON_CHESTPLATE));
        assertSame(Items.DIAMOND_BOOTS, EquipmentHelper.bestAvailable(inventory::contains, Items.IRON_BOOTS));
        assertSame(Items.IRON_HELMET, EquipmentHelper.bestAvailable(inventory::contains, Items.IRON_HELMET));
    }

    @Test
    void requirementUpdatesWhenBetterEquipmentArrivesDuringCrafting() {
        assertEquals(1, EquipmentHelper.missingEquipment(item -> false, Items.WOODEN_PICKAXE).length);
        assertEquals(0, EquipmentHelper.missingEquipment(Set.of(Items.NETHERITE_PICKAXE)::contains,
                Items.WOODEN_PICKAXE).length);
    }

    @Test
    void nonEquipmentAndPiglinGoldRemainExactRequirements() {
        Set<Item> inventory = Set.of(Items.NETHERITE_HELMET, Items.IRON_INGOT);
        assertFalse(EquipmentHelper.hasAtLeast(inventory::contains, Items.GOLDEN_HELMET));
        assertFalse(EquipmentHelper.hasAtLeast(inventory::contains, Items.DIAMOND));
        assertTrue(EquipmentHelper.hasAtLeast(inventory::contains, Items.IRON_INGOT));
    }
}
