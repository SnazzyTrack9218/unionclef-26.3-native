package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasksystem.ITaskUsesCraftingGrid;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.RecipeBookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class CraftGenericWithRecipeBooksTask extends Task implements ITaskUsesCraftingGrid {

    /** States this task passes through per tick; read over py4j in placeStats(). */
    public static volatile int cgTick, cgBigOpen, cgInvOpen, cgNoScreen, cgSent, cgOutputReady, cgCraftable, cgNotCraftable, cgBookCraftable, cgBookNone;
    /** Ticks a 3x3 recipe was attempted with only the 2x2 inventory grid open. Read as cgSmall. */
    public static volatile int cgTooSmallGrid;
    /** Item of the last recipe actually sent; read over py4j. */
    public static volatile String cgLastSent = "-";
    /** Screen handler the last recipe was sent from; read over py4j. */
    public static volatile String cgScreen = "-";

    private final RecipeTarget target;
    private long recipeSentAt;
    private boolean useManualCrafting;

    public CraftGenericWithRecipeBooksTask(RecipeTarget target) {
        this.target = target;
    }

    /**
     * This method is called when the mod starts.
     */
    @Override
    protected void onStart() {
        recipeSentAt = 0L;
        useManualCrafting = false;
    }

    /**
     * This method handles the logic for the onTick event.
     * It checks various conditions and performs actions accordingly.
     *
     * @return The next task to execute.
     */
    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        // Check if the big crafting UI or player inventory UI is open
        boolean isBigCraftingOpen = StorageHelper.isBigCraftingOpen();
        boolean isPlayerInventoryOpen = StorageHelper.isPlayerInventoryOpen();
        // WHERE DOES A CRAFT DIE? Measured: twelve minutes with twelve wood items in the pack --
        // a table needs four planks -- and the crafting rung never arrives, while the chain sits
        // in this very task. Count the states it passes through before touching any of them.
        cgTick++;
        if (isBigCraftingOpen) cgBigOpen++;
        else if (isPlayerInventoryOpen) cgInvOpen++;
        else cgNoScreen++;

        // A 3x3 RECIPE CANNOT BE MADE IN A 2x2 GRID, AND PRETENDING OTHERWISE COSTS THE RUN.
        // Read out of a stall capture: the bot stood still for ninety seconds inside
        // <Doing stuff in crafting_table container> while trying to make a wooden_pickaxe, with
        // cgScreen=class_1723 -- the PLAYER inventory, a 2x2 grid -- and cgBookNone=655. The
        // recipe book was answering correctly every single time; nothing here listened, so the
        // task retried an impossible craft for ever instead of admitting it needs a table.
        // Saying so lets the parent open one, which is the whole point of having a parent.
        if (!isBigCraftingOpen && target.getRecipe() != null && target.getRecipe().isBig()) {
            cgTooSmallGrid++;
            setDebugState("Recipe needs a 3x3 grid; the 2x2 inventory cannot make it");
            stop();
            return null;
        }

        // The manual child owns its cursor and grid for the whole placement sequence.
        if (useManualCrafting) return new CraftGenericManuallyTask(target);

        // Get the item stack in the cursor slot
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();

        // Declare variables for the slots to move to and the garbage slot
        Optional<Slot> moveTo;
        Optional<Slot> garbage;

        // Check if neither the big crafting UI nor the player inventory UI is open
        if (!isBigCraftingOpen && !isPlayerInventoryOpen) {
            // Check if the cursor stack is not empty
            if (!cursorStack.isEmpty()) {
                // Find a slot in the player's inventory to move the item to
                moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    // Click the slot to move the item to the player's inventory
                    mod.getSlotHandler().clickSlot(moveTo.get(), 0, ContainerInput.PICKUP);
                    return null;
                }
                // Check if the item can be thrown away
                if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                    // Click an undefined slot to throw away the item
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
                    return null;
                }
                // Find the garbage slot and click it to move the item there
                garbage = StorageHelper.getGarbageSlot(mod);
                if (garbage.isPresent()) {
                    mod.getSlotHandler().clickSlot(garbage.get(), 0, ContainerInput.PICKUP);
                }
                // Click an undefined slot to clear the cursor stack
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
            } else {
                // Close the screen
                StorageHelper.closeScreen();
            }
        }

        // Determine the output slot based on whether the big crafting UI is open
        Slot outputSlot = isBigCraftingOpen ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;
        // Get the item stack in the output slot
        ItemStack output = StorageHelper.getItemStackInSlot(outputSlot);

        // Check if the output item matches the target item and the target count has not been reached
        // DOES THE CLICK PRODUCE ANYTHING? A recipe going out (cgSent 3/2/1) is not the same as
        // an item appearing in the output slot, and those two need opposite fixes.
        if (target.getOutputItem() == output.getItem()) cgOutputReady++;
        if (target.getOutputItem() == output.getItem() && mod.getItemStorage().getItemCount(target.getOutputItem()) < target.getTargetCount()) {
            // Return a task to receive the crafting output slot
            return new ReceiveCraftingOutputSlotTask(outputSlot, target.getTargetCount());
        }

        // Check if the cursor stack is not empty
        if (!cursorStack.isEmpty()) {
            // Find a slot in the player's inventory to move the item to
            moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            if (moveTo.isPresent()) {
                // Click the slot to move the item to the player's inventory
                mod.getSlotHandler().clickSlot(moveTo.get(), 0, ContainerInput.PICKUP);
                return null;
            }
            // Check if the item can be thrown away
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                // Click an undefined slot to throw away the item
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
                return null;
            }
            // Find the garbage slot and click it to move the item there
            garbage = StorageHelper.getGarbageSlot(mod);
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));
            // Click an undefined slot to clear the cursor stack
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
            return null;
        }

        // Let a request finish before clearing its grid or sending it again. The table
        // crafter already uses this response budget and manual fallback.
        if (recipeSentAt != 0L) {
            if (System.currentTimeMillis() - recipeSentAt < RecipeBookHelper.RESPONSE_TIMEOUT_MS) {
                setDebugState("Waiting for recipe-book output");
                return null;
            }
            recipeSentAt = 0L;
            useManualCrafting = true;
        }
        if (useManualCrafting) {
            setDebugState("Placing ingredients by hand");
            return new CraftGenericManuallyTask(target);
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || mod.getWorld() == null) return null;
        if (!isBigCraftingOpen && !StorageHelper.isPlayerInventoryScreenOpen()) {
            Minecraft.getInstance().setScreenAndShow(
                    new net.minecraft.client.gui.screens.inventory.InventoryScreen(player));
            return null;
        }
        if (mod.getSlotHandler().canDoSlotAction()) {
            var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(mod.getWorld());
            var contents = new net.minecraft.world.entity.player.StackedItemContents();
            player.getInventory().fillStackedContents(contents);
            if (player.containerMenu instanceof net.minecraft.world.inventory.AbstractCraftingMenu menu) {
                menu.fillCraftSlotsStackedContents(contents);
            }
            var recipe = RecipeBookHelper.findCraftable(player.getRecipeBook().getCollections(),
                    target.getOutputItem(), context, contents,
                    isBigCraftingOpen ? 3 : 2, isBigCraftingOpen ? 3 : 2);
            if (recipe.isPresent()) {
                cgCraftable++;
                cgScreen = player.containerMenu.getClass().getSimpleName();
                cgSent++;
                cgLastSent = String.valueOf(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(target.getOutputItem()));
                mod.getController().handlePlaceRecipe(
                        player.containerMenu.containerId, recipe.get().id(), true);
                mod.getSlotHandler().registerSlotAction();
                recipeSentAt = System.currentTimeMillis();
            } else {
                cgNotCraftable++;
                useManualCrafting = true;
                return new CraftGenericManuallyTask(target);
            }
        }

        return null;
    }

    /**
     * This method is called when the task is interrupted.
     *
     * @param interruptTask The task that interrupted the current task.
     */
    @Override
    protected void onStop(Task interruptTask) {

    }

    /**
     * Checks if a given Task object is equal to this CraftGenericWithRecipeBooksTask object.
     *
     * @param other The Task object to compare with.
     * @return True if the given Task is equal to this CraftGenericWithRecipeBooksTask, false otherwise.
     */
    @Override
    protected boolean isEqual(Task other) {
        // Check if the other Task is an instance of CraftGenericWithRecipeBooksTask
        if (other instanceof CraftGenericWithRecipeBooksTask) {
            CraftGenericWithRecipeBooksTask task = (CraftGenericWithRecipeBooksTask) other;

            // Check if the target of the other task is equal to the target of this task
            boolean isEqual = task.target.equals(target);

            // Log a message if the targets are not equal
            if (!isEqual) {
                Debug.logInternal("Task targets are not equal");
            }

            // Return the result of the equality check
            return isEqual;
        }

        // Log a message if the other Task is not an instance of CraftGenericWithRecipeBooksTask
        Debug.logInternal("Task is not an instance of CraftGenericWithRecipeBooksTask");

        // Return false if the other Task is not an instance of CraftGenericWithRecipeBooksTask
        return false;
    }

    /**
     * Returns a debug string representation of the object.
     *
     * @return The debug string representation.
     */
    @Override
    protected String toDebugString() {
        // Return the debug string.
        return getClass().getSimpleName() + " (w/ RECIPE): " + target;
    }
}
