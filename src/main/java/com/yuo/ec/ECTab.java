package com.yuo.ec;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public class ECTab extends ItemGroup {
    public static ItemGroup EC_TAB = new ECTab();

    public ECTab() {
        super(ItemGroup.GROUPS.length, "endless_compat");
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ECItems.infinityPotato.get());
    }
}
