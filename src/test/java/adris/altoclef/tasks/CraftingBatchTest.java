package adris.altoclef.tasks;

import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.RecipeTarget;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftingBatchTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static RecipeTarget bread(int count) {
        return new RecipeTarget(Items.BREAD, count, CraftingRecipe.newShapedRecipe(new Item[][]{
                {Items.WHEAT}, {Items.WHEAT}, {Items.WHEAT}, null, null, null, null, null, null
        }, 1));
    }

    @Test void onlyFillsForTheRemainingBread() {
        assertEquals(2, CraftGenericManuallyTask.computeRequiredPerSlot(bread(32), 30, items -> 96));
    }

    @Test void dividesSupplyAcrossRepeatedIngredientSlots() {
        assertEquals(3, CraftGenericManuallyTask.computeRequiredPerSlot(bread(32), 0, items -> 9));
    }

    @Test void capsVeryLargeRequestsAtAStackPerSlot() {
        assertEquals(64, CraftGenericManuallyTask.computeRequiredPerSlot(bread(99_999_999), 0, items -> 1000));
    }

    @Test void accountsForRecipesProducingMultipleOutputs() {
        var target = new RecipeTarget(Items.OAK_PLANKS, 10,
                CraftingRecipe.newShapedRecipe(new Item[][]{{Items.OAK_LOG}, null, null, null}, 4));
        assertEquals(2, CraftGenericManuallyTask.computeRequiredPerSlot(target, 5, items -> 64));
    }

    @Test void limitsBatchToTheScarcerOfDifferentIngredients() {
        var target = new RecipeTarget(Items.IRON_AXE, 3, CraftingRecipe.newShapedRecipe(new Item[][]{
                {Items.IRON_INGOT}, {Items.IRON_INGOT}, null,
                {Items.IRON_INGOT}, {Items.STICK}, null,
                null, {Items.STICK}, null
        }, 1));
        assertEquals(1, CraftGenericManuallyTask.computeRequiredPerSlot(target, 0,
                items -> items[0] == Items.IRON_INGOT ? 9 : 2));
    }
}
