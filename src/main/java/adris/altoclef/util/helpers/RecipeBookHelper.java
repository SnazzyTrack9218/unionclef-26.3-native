package adris.altoclef.util.helpers;

import adris.altoclef.util.CraftingRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.*;

import java.util.List;
import java.util.Optional;

/** Shared interpretation of the server's recipe displays. */
public final class RecipeBookHelper {
    // The table crafter's existing response budget, shared with inventory crafting.
    public static final long RESPONSE_TIMEOUT_MS = 1500L;

    private RecipeBookHelper() { }

    public static boolean fitsGrid(RecipeDisplay display, int gridWidth, int gridHeight) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return shaped.width() > 0 && shaped.height() > 0
                    && shaped.width() <= gridWidth && shaped.height() <= gridHeight;
        }
        return display instanceof ShapelessCraftingRecipeDisplay shapeless
                && !shapeless.ingredients().isEmpty()
                && shapeless.ingredients().size() <= gridWidth * gridHeight;
    }

    public static Optional<RecipeDisplayEntry> findCraftable(
            List<RecipeCollection> collections, Item output, ContextMap context,
            StackedItemContents contents, int gridWidth, int gridHeight) {
        for (RecipeCollection collection : collections) {
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                if (!fitsGrid(entry.display(), gridWidth, gridHeight)) continue;
                // Match the requested output before doing the ingredient calculation. There
                // is no need to recompute every collection's UI state just to craft one item.
                if (entry.resultItems(context).stream().anyMatch(stack -> stack.is(output))
                        && entry.canCraft(contents)) {
                    return Optional.of(entry);
                }
            }
        }
        return Optional.empty();
    }

    public static CraftingRecipe toCraftingRecipe(RecipeDisplay display, int outputCount, ContextMap context) {
        if (!fitsGrid(display, 3, 3)) return null;
        List<SlotDisplay> ingredients;
        int width, height;
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            ingredients = shaped.ingredients();
            width = shaped.width();
            height = shaped.height();
            if (ingredients.size() != width * height) return null;
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            ingredients = shapeless.ingredients();
            // Up to four shapeless ingredients fit in the player's 2x2 grid.
            width = ingredients.size() <= 4 ? Math.min(ingredients.size(), 2) : 3;
            height = (ingredients.size() + width - 1) / width;
        } else {
            return null;
        }
        int gridWidth = width <= 2 && height <= 2 ? 2 : 3;
        Item[][] cells = new Item[gridWidth * gridWidth][];
        for (int i = 0; i < ingredients.size(); i++) {
            SlotDisplay ingredient = ingredients.get(i);
            if (ingredient instanceof SlotDisplay.Empty) continue;
            Item[] matching = ingredient.resolveForStacks(context).stream()
                    .filter(stack -> !stack.isEmpty()).map(ItemStack::getItem)
                    .distinct().toArray(Item[]::new);
            // A missing modded tag is not an empty slot: do not advertise a free recipe.
            if (matching.length == 0) return null;
            cells[(i / width) * gridWidth + i % width] = matching;
        }
        return CraftingRecipe.newShapedRecipe(cells, outputCount);
    }
}
