package com.yuo.ec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ECTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EndlessCompat.MOD_ID);
    public static final RegistryObject<CreativeModeTab> EC_TAB = TABS.register("endless_compat", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tab.endless_compat"))
            .icon(() -> ECItems.infinityPotato.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> entry : ECItems.ITEMS.getEntries()) {
                    output.accept(entry.get());
                }
            }).build());
}
