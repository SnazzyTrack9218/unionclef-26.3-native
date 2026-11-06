package adris.altoclef.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodConversionPlanTest {
    @Test void partialWheatBecomesBreadBeforeTheFullGatheringGoalIsReached() {
        var plan = FoodConversionPlan.create(3, 0, 0, 0, 220, 5);
        assertEquals(1, plan.breadTarget());
        assertEquals(3, plan.wheatTarget());
    }

    @Test void aSingleHayBlockCanBeProcessedImmediately() {
        var plan = FoodConversionPlan.create(0, 1, 0, 0, 220, 5);
        assertEquals(9, plan.wheatTarget());
        assertEquals(0, plan.breadTarget());
    }

    @Test void breadTargetIncludesBreadAlreadyOwned() {
        var plan = FoodConversionPlan.create(36, 0, 20, 100, 160, 5);
        assertEquals(32, plan.breadTarget());
    }

    @Test void wheatTargetIncludesWheatAlreadyOwned() {
        var plan = FoodConversionPlan.create(9, 1, 0, 0, 30, 5);
        assertEquals(18, plan.wheatTarget());
        assertEquals(3, plan.breadTarget());
    }

    @Test void conversionDoesNotConsumeEveryHayBlockForASmallShortfall() {
        var plan = FoodConversionPlan.create(2, 20, 0, 0, 5, 5);
        assertEquals(11, plan.wheatTarget());
    }

    @Test void satisfiedFoodGoalDoesNotStartMoreCrafting() {
        var plan = FoodConversionPlan.create(36, 20, 44, 220, 220, 5);
        assertEquals(36, plan.wheatTarget());
        assertEquals(44, plan.breadTarget());
    }

    @Test void potentialCountsWholeBreadOnceAcrossAllIngredients() {
        assertEquals(0, FoodConversionPlan.potentialBread(2, 0));
        assertEquals(1, FoodConversionPlan.potentialBread(3, 0));
        assertEquals(3, FoodConversionPlan.potentialBread(0, 1));
        assertEquals(6, FoodConversionPlan.potentialBread(9, 1));
    }
}
