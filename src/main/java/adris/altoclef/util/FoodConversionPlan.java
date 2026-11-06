package adris.altoclef.util;

/** Finite, absolute crafting targets for the ingredients already in the inventory. */
public record FoodConversionPlan(int wheatTarget, int breadTarget) {
    public static int potentialBread(int wheat, int hay) {
        return wheat / 3 + hay * 3;
    }

    public static FoodConversionPlan create(int wheat, int hay, int bread,
                                            int readyFood, double requestedFood, int breadHunger) {
        int neededBread = (int) Math.ceil(Math.max(0, requestedFood - readyFood) / breadHunger);
        int additionalBread = Math.min(wheat / 3, neededBread);
        int wheatShortfall = Math.max(0, neededBread * 3 - wheat);
        int hayToConvert = Math.min(hay, (wheatShortfall + 8) / 9);
        return new FoodConversionPlan(wheat + hayToConvert * 9, bread + additionalBread);
    }
}
