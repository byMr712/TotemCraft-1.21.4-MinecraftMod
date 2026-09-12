package com.totemcraft.recipe;

import com.totemcraft.TotemCraftMod;
import com.totemcraft.config.TotemCraftConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TotemCraftCustomRecipe extends ShapedRecipe {

    public TotemCraftCustomRecipe(CraftingRecipeCategory category) {
        super("totemcraft", category, createDefaultRaw(), new ItemStack(Items.TOTEM_OF_UNDYING), true);
    }

    private static RawShapedRecipe createDefaultRaw() {
        Map<Character, Ingredient> key = Map.of(
                'A', Ingredient.ofItem(Items.GOLDEN_APPLE),
                'G', Ingredient.ofItem(Items.GHAST_TEAR)
        );
        return RawShapedRecipe.create(key, "AAA", "AGA", "AAA");
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public boolean isIgnoredInRecipeBook() {
        return false;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
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
    public List<Optional<Ingredient>> getIngredients() {
        TotemCraftConfig config = TotemCraftConfig.getInstance();
        List<Optional<Ingredient>> list = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            Item item = config.getItemAt(i);
            if (item == Items.AIR) {
                list.add(Optional.empty());
            } else {
                list.add(Optional.of(Ingredient.ofItem(item)));
            }
        }
        return list;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forMultipleSlots(getIngredients());
    }

    @Override
    public List<RecipeDisplay> getDisplays() {
        TotemCraftConfig config = TotemCraftConfig.getInstance();
        if (!config.enabled) {
            return Collections.emptyList();
        }

        List<SlotDisplay> ingredients = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            Item item = config.getItemAt(i);
            if (item == Items.AIR) {
                ingredients.add(SlotDisplay.EmptySlotDisplay.INSTANCE);
            } else {
                ingredients.add(new SlotDisplay.ItemSlotDisplay(item));
            }
        }

        SlotDisplay result = new SlotDisplay.StackSlotDisplay(new ItemStack(config.getResultItem(), config.resultCount));
        SlotDisplay craftingStation = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);

        return Collections.singletonList(new ShapedCraftingRecipeDisplay(3, 3, ingredients, result, craftingStation));
    }

    @Override
    public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
        return TotemCraftMod.RECIPE_SERIALIZER;
    }
}
