package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.RecipeBookHelper;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CraftingRecipeTracker extends Tracker{


    private final HashMap<Item, List<adris.altoclef.util.CraftingRecipe>> itemRecipeMap = new HashMap<>();
    private final HashMap<adris.altoclef.util.CraftingRecipe, ItemStack> recipeResultMap = new HashMap<>();

    private boolean shouldRebuild;
    private List<RecipeCollection> cachedCollections;

    /**
     * How many distinct items the tracker has a recipe for. Read as recipesKnown.
     *
     * <p>Published because it is the claim: this was structurally 0 on 1.21.11, and "the port is
     * done" is only worth saying if the number moved. A course can fail on it, which is the point.
     */
    public static volatile int recipesKnown;

    public CraftingRecipeTracker(TrackerManager manager) {
        super(manager);
        shouldRebuild = true;
    }

    public List<adris.altoclef.util.CraftingRecipe> getRecipeForItem(Item item) {
        ensureUpdated();

        if (!hasRecipeForItem(item)) {
            mod.logWarning("trying to access recipe for unknown item: "+item);
            return null;
        }

        return itemRecipeMap.get(item);
    }

    public adris.altoclef.util.CraftingRecipe getFirstRecipeForItem(Item item) {
        ensureUpdated();

        if (!hasRecipeForItem(item)) {
            mod.logWarning("trying to access recipe for unknown item: "+item);
            return null;
        }

        return itemRecipeMap.get(item).get(0);
    }

    public List<RecipeTarget> getRecipeTarget(Item item, int targetCount) {
        ensureUpdated();

        List<RecipeTarget> targets = new ArrayList<>();
        List<adris.altoclef.util.CraftingRecipe> recipes = getRecipeForItem(item);
        if (recipes == null) return targets;
        for (adris.altoclef.util.CraftingRecipe recipe : recipes) {
            targets.add(new RecipeTarget(item, targetCount, recipe));
        }

        return targets;
    }

    public RecipeTarget getFirstRecipeTarget(Item item, int targetCount) {
        ensureUpdated();

        return new RecipeTarget(item, targetCount, getFirstRecipeForItem(item));
    }

    public boolean hasRecipeForItem(Item item) {
        ensureUpdated();
        return itemRecipeMap.containsKey(item);
    }

    public ItemStack getRecipeResult(adris.altoclef.util.CraftingRecipe recipe) {
        ensureUpdated();

        if (!hasRecipe(recipe)) {
            mod.logWarning("Trying to get result for unknown recipe: "+recipe);
            return null;
        }
        ItemStack result = recipeResultMap.get(recipe);

        return new ItemStack(result.getItem(), result.getCount());
    }

    public boolean hasRecipe(adris.altoclef.util.CraftingRecipe recipe) {
        ensureUpdated();
        return recipeResultMap.containsKey(recipe);
    }


    @Override
    protected void updateState() {
        if (!AltoClef.inGame()) return;
        var player = Minecraft.getInstance().player;
        if (player == null || mod.getWorld() == null) return;
        var collections = player.getRecipeBook().getCollections();
        var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(mod.getWorld());
        itemRecipeMap.clear();
        recipeResultMap.clear();
        for (var collection : collections) {
            for (var entry : collection.getRecipes()) {
                var outputs = entry.resultItems(context);
                if (outputs.isEmpty()) continue;
                var result = outputs.getFirst().copy();
                var recipe = RecipeBookHelper.toCraftingRecipe(entry.display(), result.getCount(), context);
                if (recipe == null) continue;
                itemRecipeMap.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(recipe);
                recipeResultMap.put(recipe, result);
            }
        }
        // MC 26.3 replaces this immutable list in ClientRecipeBook.rebuildCollections().
        // Identity detects recipe additions/removals without scanning the book each tick.
        cachedCollections = collections;
        itemRecipeMap.replaceAll((k, v) -> Collections.unmodifiableList(v));
        recipesKnown = itemRecipeMap.size();
        shouldRebuild = false;
    }


    @Override
    protected void reset() {
       shouldRebuild = true;
       cachedCollections = null;
       recipesKnown = 0;
       itemRecipeMap.clear();
       recipeResultMap.clear();
    }

    @Override
    protected boolean isDirty() {
        var player = Minecraft.getInstance().player;
        return shouldRebuild || (player != null
                && player.getRecipeBook().getCollections() != cachedCollections);
    }
}
