package com.yuo.ec;

import com.yuo.ec.Bm3.ArmokBloodOrb;
import com.yuo.ec.Botania.ItemBlockPotato;
import com.yuo.endless.EndlessTab;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.BloodOrb;
import wayoftime.bloodmagic.common.registration.impl.BloodOrbDeferredRegister;
import wayoftime.bloodmagic.common.registration.impl.BloodOrbRegistryObject;

public class ECItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EndlessCompat.MOD_ID);
    public static final Item.Properties BLOCK_TAB = new Item.Properties().group(EndlessTab.endless);

    //Botania
    public static RegistryObject<BlockItem> infinityPotato = ITEMS.register("infinity_potato", () -> new ItemBlockPotato(ECBlocks.infinityPotato.get(), BLOCK_TAB.maxStackSize(64)));
    public static RegistryObject<BlockItem> asgardFlower = ITEMS.register("asgard_flower", () -> new BlockItem(ECBlocks.asgardFlower.get(),  BLOCK_TAB.maxStackSize(64)));
    public static RegistryObject<BlockItem> asgardFlowerFloating = ITEMS.register("asgard_flower_floating", () -> new BlockItem(ECBlocks.asgardFlowerFloating.get(),  BLOCK_TAB.maxStackSize(64)));

    //Bm3
    public static final BloodOrbDeferredRegister BLOOD_ORBS = new BloodOrbDeferredRegister("endless_compat");
    public static final BloodOrbRegistryObject<BloodOrb> ORB_ARMOK = BLOOD_ORBS.register("orb_armok",
            ()  -> new BloodOrb(new ResourceLocation(EndlessCompat.MOD_ID, "armok_blood_orb"), 9, Integer.MAX_VALUE, 99));
    public static RegistryObject<Item> armokBloodOrb = ITEMS.register("armok_blood_orb", () -> new ArmokBloodOrb(ORB_ARMOK));
    public static RegistryObject<Item> archmageBloodOrb = ITEMS.register("archmage_blood_orb", () -> new ArmokBloodOrb(BloodMagicItems.ORB_ARCHMAGE));
}
