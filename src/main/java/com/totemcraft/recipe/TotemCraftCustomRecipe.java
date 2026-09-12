package com.totemcraft.recipe;

import com.totemcraft.TotemCraftMod;
import com.totemcraft.config.TotemCraftConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class TotemCraftCustomRecipe extends SpecialCraftingRecipe {
    public TotemCraftCustomRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        TotemCraftConfig config = TotemCraftConfig.getInstance();
        if (!config.enabled) {
            return false;
        }
        if (input.getWidth() < 3 || input.getHeight() < 3) {
            return false;
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int slotIndex = y * 3 + x;
                Item requiredItem = config.getItemAt(slotIndex);
                ItemStack stack = input.getStackInSlot(x, y);
                if (requiredItem == Items.AIR) {
                    if (!stack.isEmpty()) {
                        return false;
                    }
                } else {
                    if (stack.isEmpty() || !stack.isOf(requiredItem)) {
                        return false;
                    }
                }
            }
        }

        // Ensure no leftover items outside of the 3x3 region if grid is larger
        if (input.getWidth() > 3 || input.getHeight() > 3) {
            for (int y = 0; y < input.getHeight(); y++) {
                for (int x = 0; x < input.getWidth(); x++) {
                    if (x >= 3 || y >= 3) {
                        if (!input.getStackInSlot(x, y).isEmpty()) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        TotemCraftConfig config = TotemCraftConfig.getInstance();
        return new ItemStack(config.getResultItem(), config.resultCount);
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return TotemCraftMod.RECIPE_SERIALIZER;
    }
}
