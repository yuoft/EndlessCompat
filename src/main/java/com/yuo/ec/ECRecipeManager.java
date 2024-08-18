package com.yuo.ec;

import com.yuo.endless.Items.EndlessItems;
import com.yuo.endless.Recipe.ExtremeCraftingManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import vazkii.botania.common.block.ModBlocks;

public class ECRecipeManager {

    public static void addRecipe(){
        ExtremeCraftingManager.getInstance().addRecipe(new ItemStack(ECItems.infinityPotato.get()),
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAACACAAA",
                "AAAAAAAAA",
                "AACABACAA",
                "AAACCCAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                'A', new ItemStack(ModBlocks.tinyPotato),
                'B', new ItemStack(EndlessItems.infinityCatalyst.get()),
                'C', new ItemStack(Items.DIAMOND));
        ExtremeCraftingManager.getInstance().addRecipe(new ItemStack(ECItems.asgardFlower.get()),
                "   AAA   ",
                "  AAAAA  ",
                "  AABAA  ",
                "  AAAAA  ",
                "   AAA   ",
                " CC D CC ",
                "CCCCDCCCC",
                " CC D CC ",
                "    D    ",
                'A', new ItemStack(EndlessItems.infinityIngot.get()),
                'B', new ItemStack(EndlessItems.infinityCatalyst.get()),
                'C', new ItemStack(EndlessItems.neutroniumNugget.get()),
                'D', new ItemStack(EndlessItems.neutroniumIngot.get()));
        ExtremeCraftingManager.getInstance().addRecipe(new ItemStack(ECItems.armokBloodOrb.get()),
                "   AAA   ",
                "  ACACA  ",
                "  AABAA  ",
                " DACACAD ",
                "DDDAAADDD",
                " DDDDDDD ",
                "   DDD   ",
                "         ",
                "         ",
                'A', new ItemStack(EndlessItems.infinityIngot.get()),
                'B', new ItemStack(EndlessItems.infinityCatalyst.get()),
                'C', new ItemStack(ECItems.archmageBloodOrb.get()),
                'D', new ItemStack(EndlessItems.neutroniumIngot.get()));
    }
}
