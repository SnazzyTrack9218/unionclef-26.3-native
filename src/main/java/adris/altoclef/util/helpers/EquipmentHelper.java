package adris.altoclef.util.helpers;

import adris.altoclef.util.ItemTarget;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Minimum equipment requirements, separate from requests to craft an exact item. */
public final class EquipmentHelper {
    private EquipmentHelper() {}

    /** Gold is deliberately not an upgrade: a gold pickaxe cannot harvest iron ore. */
    private static final Item[][] TOOL_TIERS = {
            ItemHelper.WOODEN_TOOLS, ItemHelper.STONE_TOOLS, ItemHelper.IRON_TOOLS,
            ItemHelper.DIAMOND_TOOLS, ItemHelper.NETHERITE_TOOLS
    };
    private static final Item[][] ARMOR_TIERS = {
            ItemHelper.IRON_ARMORS, ItemHelper.DIAMOND_ARMORS, ItemHelper.NETHERITE_ARMORS
    };

    public static Item[] equalOrBetter(Item minimum) {
        for (Item[][] tiers : new Item[][][]{TOOL_TIERS, ARMOR_TIERS}) {
            for (int tier = 0; tier < tiers.length; tier++) {
                for (int slot = 0; slot < tiers[tier].length; slot++) {
                    if (tiers[tier][slot] != minimum) continue;
                    Item[] matches = new Item[tiers.length - tier];
                    for (int upgrade = tier; upgrade < tiers.length; upgrade++) {
                        matches[upgrade - tier] = tiers[upgrade][slot];
                    }
                    return matches;
                }
            }
        }
        // Non-equipment items (including a piglin-safe gold helmet) remain exact matches.
        return new Item[]{minimum};
    }

    public static boolean hasAtLeast(Predicate<Item> present, Item minimum) {
        for (Item candidate : equalOrBetter(minimum)) {
            if (present.test(candidate)) return true;
        }
        return false;
    }

    /** Check every tool/armor slot independently so mixed-tier sets also count. */
    public static ItemTarget[] missingEquipment(Predicate<Item> present, Item... minimums) {
        List<ItemTarget> missing = new ArrayList<>();
        for (Item minimum : minimums) {
            if (!hasAtLeast(present, minimum)) missing.add(new ItemTarget(minimum, 1));
        }
        return missing.toArray(ItemTarget[]::new);
    }

    public static Item bestAvailable(Predicate<Item> present, Item minimum) {
        Item[] candidates = equalOrBetter(minimum);
        for (int i = candidates.length - 1; i >= 0; i--) {
            if (present.test(candidates[i])) return candidates[i];
        }
        return minimum;
    }
}
