package adris.altoclef.util.helpers;

import net.minecraft.SharedConstants;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class RecipeBookHelperTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var lookup = net.minecraft.data.registries.VanillaRegistries.createWorldLookup();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
                .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
    }

    private static SlotDisplay item(Item item) { return new SlotDisplay.ItemSlotDisplay(item); }
    private static ShapelessCraftingRecipeDisplay shapeless(List<SlotDisplay> inputs, Item output) {
        return new ShapelessCraftingRecipeDisplay(inputs, item(output), item(Items.CRAFTING_TABLE));
    }
    private static RecipeDisplayEntry entry(int id, Item input, Item output) {
        return new RecipeDisplayEntry(new RecipeDisplayId(id), shapeless(List.of(item(input)), output),
                OptionalInt.empty(), RecipeBookCategories.CRAFTING_MISC,
                Optional.of(List.of(Ingredient.of(input))));
    }

    @Test void preservesEveryValidIngredientAlternative() {
        var alternatives = new SlotDisplay.Composite(List.of(item(Items.OAK_PLANKS), item(Items.SPRUCE_PLANKS)));
        var recipe = RecipeBookHelper.toCraftingRecipe(
                shapeless(List.of(alternatives), Items.STICK), 4, ContextMap.EMPTY);
        assertArrayEquals(new Item[]{Items.OAK_PLANKS, Items.SPRUCE_PLANKS}, recipe.getSlot(0).getMatches());
        assertEquals(4, recipe.outputCount());
    }

    @Test void removesDuplicateIngredientAlternatives() {
        var alternatives = new SlotDisplay.Composite(List.of(item(Items.OAK_PLANKS), item(Items.OAK_PLANKS)));
        var recipe = RecipeBookHelper.toCraftingRecipe(
                shapeless(List.of(alternatives), Items.STICK), 4, ContextMap.EMPTY);
        assertArrayEquals(new Item[]{Items.OAK_PLANKS}, recipe.getSlot(0).getMatches());
    }

    @Test void threeShapelessIngredientsFitTheInventoryGrid() {
        var display = shapeless(List.of(item(Items.WHEAT), item(Items.WHEAT), item(Items.WHEAT)), Items.BREAD);
        var recipe = RecipeBookHelper.toCraftingRecipe(display, 1, ContextMap.EMPTY);
        assertFalse(recipe.isBig());
        assertTrue(RecipeBookHelper.fitsGrid(display, 2, 2));
        assertEquals(4, recipe.getSlotCount());
    }

    @Test void fiveShapelessIngredientsNeedATable() {
        var display = shapeless(java.util.Collections.nCopies(5, item(Items.WHEAT)), Items.BREAD);
        assertTrue(RecipeBookHelper.toCraftingRecipe(display, 1, ContextMap.EMPTY).isBig());
        assertFalse(RecipeBookHelper.fitsGrid(display, 2, 2));
    }

    @Test void preservesShapedGridPositionsAndEmptySlots() {
        var display = new ShapedCraftingRecipeDisplay(1, 2,
                List.of(SlotDisplay.Empty.INSTANCE, item(Items.OAK_PLANKS)),
                item(Items.STICK), item(Items.CRAFTING_TABLE));
        var recipe = RecipeBookHelper.toCraftingRecipe(display, 4, ContextMap.EMPTY);
        assertEquals(0, recipe.getSlot(0).getMatches().length);
        assertEquals(0, recipe.getSlot(1).getMatches().length);
        assertArrayEquals(new Item[]{Items.OAK_PLANKS}, recipe.getSlot(2).getMatches());
    }

    @Test void threeWideShapedRecipeCannotUseInventory() {
        var display = new ShapedCraftingRecipeDisplay(3, 1,
                java.util.Collections.nCopies(3, item(Items.WHEAT)), item(Items.BREAD), item(Items.CRAFTING_TABLE));
        assertFalse(RecipeBookHelper.fitsGrid(display, 2, 2));
        assertTrue(RecipeBookHelper.toCraftingRecipe(display, 1, ContextMap.EMPTY).isBig());
    }

    @Test void rejectsUnresolvedIngredientInsteadOfTreatingItAsFree() {
        var display = shapeless(List.of(new SlotDisplay.Composite(List.of())), Items.BREAD);
        assertNull(RecipeBookHelper.toCraftingRecipe(display, 1, ContextMap.EMPTY));
    }

    @Test void fourShapelessIngredientsUseAllFourInventorySlots() {
        var display = shapeless(java.util.Collections.nCopies(4, item(Items.WHEAT)), Items.BREAD);
        var recipe = RecipeBookHelper.toCraftingRecipe(display, 1, ContextMap.EMPTY);
        assertFalse(recipe.isBig());
        for (int i = 0; i < 4; i++) assertArrayEquals(new Item[]{Items.WHEAT}, recipe.getSlot(i).getMatches());
    }

    @Test void selectsCraftableVariantAfterAnUnavailableVariant() {
        var oak = entry(1, Items.OAK_LOG, Items.OAK_PLANKS);
        var spruce = entry(2, Items.SPRUCE_LOG, Items.OAK_PLANKS);
        var collection = new RecipeCollection(List.of(oak, spruce));
        var contents = new StackedItemContents();
        contents.accountSimpleStack(new ItemStack(Items.SPRUCE_LOG));
        assertEquals(spruce, RecipeBookHelper.findCraftable(List.of(collection), Items.OAK_PLANKS,
                ContextMap.EMPTY, contents, 2, 2).orElseThrow());
        // Selection does not mutate the recipe book UI's craftable set.
        assertFalse(collection.hasCraftable());
    }

    @Test void unavailableRecipesDoNotProduceCraftRequests() {
        var collection = new RecipeCollection(List.of(entry(1, Items.OAK_LOG, Items.OAK_PLANKS)));
        assertTrue(RecipeBookHelper.findCraftable(List.of(collection), Items.OAK_PLANKS,
                ContextMap.EMPTY, new StackedItemContents(), 2, 2).isEmpty());
    }

    @Test void unrelatedCraftableOutputDoesNotGetSelected() {
        var contents = new StackedItemContents();
        contents.accountSimpleStack(new ItemStack(Items.OAK_LOG));
        var collection = new RecipeCollection(List.of(entry(1, Items.OAK_LOG, Items.OAK_PLANKS)));
        assertTrue(RecipeBookHelper.findCraftable(List.of(collection), Items.BREAD,
                ContextMap.EMPTY, contents, 2, 2).isEmpty());
    }

    @Test void serverRecipeChangesReplaceTheCollectionSnapshot() {
        // The tracker uses this actual 26.3 API property for O(1) cache invalidation.
        var book = new ClientRecipeBook();
        var empty = book.getCollections();
        var recipe = entry(1, Items.OAK_LOG, Items.OAK_PLANKS);
        book.add(recipe);
        book.rebuildCollections();
        var added = book.getCollections();
        assertNotSame(empty, added);
        assertSame(added, book.getCollections());
        assertTrue(added.stream().anyMatch(c -> c.getRecipes().contains(recipe)));
        book.remove(recipe.id());
        book.rebuildCollections();
        assertNotSame(added, book.getCollections());
        assertTrue(book.getCollections().isEmpty());
    }
}
