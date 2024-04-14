package com.ordwen.odailyquests.events.listeners.item;

import com.ordwen.odailyquests.configuration.essentials.Debugger;

import com.ordwen.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.*;

import java.util.Set;

public class CraftItemListener extends PlayerProgressor implements Listener {

    private static final Set<String> COMPLEX_RECIPES = Set.of(
            "REPAIR_ITEM", "ARMOR_DYE", "SHULKER_BOX_COLORING", "SHIELD_DECORATION", "BANNER_DUPLICATE", "MAP_CLONING", "BOOK_CLONING"
    );

    @EventHandler
    public void onCraftItemEvent(CraftItemEvent event) {

        if (event.isCancelled()) return;
        if (event.getCurrentItem() == null) return;

        ItemStack test;
        final Player player = (Player) event.getWhoClicked();

        if (event.getRecipe() instanceof ComplexRecipe complexRecipe) {
            if (COMPLEX_RECIPES.contains(complexRecipe.getKey().getKey())) return;
            test = new ItemStack(Material.valueOf(complexRecipe.getKey().getKey().toUpperCase()));
        } else {
            test = event.getCurrentItem().clone();
        }

        final ClickType click = event.getClick();
        int recipeAmount = test.getAmount();

        if (movingItem(test, recipeAmount, player, click)) return;

        switch (click) {
            case NUMBER_KEY -> {
                if (event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) != null)
                    recipeAmount = 0;
            }
            case DROP, CONTROL_DROP -> {
                ItemStack cursor = event.getCursor();
                if (!(cursor == null || cursor.getType() == Material.AIR))
                    recipeAmount = 0;
            }
            case SHIFT_RIGHT, SHIFT_LEFT -> {
                if (recipeAmount == 0)
                    break;
                int maxCraftable = getMaxCraftAmount(event.getInventory());
                int capacity = fits(test, event.getView().getBottomInventory());
                if (capacity < maxCraftable)
                    maxCraftable = ((capacity + recipeAmount - 1) / recipeAmount) * recipeAmount;
                recipeAmount = maxCraftable;
            }
        }

        if (recipeAmount == 0)
            return;

        test.setAmount(recipeAmount);

        Debugger.addDebug("=========================================================================================");
        Debugger.addDebug("CraftItemListener: onCraftItemEvent summoned by " + player.getName() + " for " + test.getType() + " x" + test.getAmount() + ".");

        setPlayerQuestProgression(event, player, test.getAmount(), "CRAFT");
    }

    /**
     * Returns the maximum amount of items that can be crafted in the given inventory.
     *
     * @param inv the inventory to check.
     * @return the maximum.
     */
    private int getMaxCraftAmount(CraftingInventory inv) {
        if (inv.getResult() == null) return 0;

        int resultCount = inv.getResult().getAmount();
        int materialCount = Integer.MAX_VALUE;

        for (ItemStack is : inv.getMatrix())
            if (is != null && is.getAmount() < materialCount) materialCount = is.getAmount();

        return resultCount * materialCount;
    }
}
