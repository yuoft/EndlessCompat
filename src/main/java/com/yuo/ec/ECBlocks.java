package com.yuo.ec;

import com.yuo.ec.Botania.AsgardFlowerTile;
import com.yuo.ec.Botania.InfinityPotato;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractBlock.Properties;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DragonEggBlock;
import net.minecraft.potion.Effects;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.botania.common.block.BlockFloatingSpecialFlower;
import vazkii.botania.common.block.BlockSpecialFlower;
import vazkii.botania.common.block.ModBlocks;

public class ECBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EndlessCompat.MOD_ID);
    public static final AbstractBlock.Properties FLOWER_PROPS = AbstractBlock.Properties.from(Blocks.POPPY);

    //Botania
    public static RegistryObject<Block> infinityPotato = BLOCKS.register("infinity_potato", InfinityPotato::new);
    public static RegistryObject<Block> asgardFlower = BLOCKS.register("asgard_flower", () -> new BlockSpecialFlower(Effects.HEALTH_BOOST, 360, FLOWER_PROPS, AsgardFlowerTile::new));
    public static RegistryObject<Block> asgardFlowerFloating = BLOCKS.register("asgard_flower_floating", () -> new BlockFloatingSpecialFlower(ModBlocks.FLOATING_PROPS, AsgardFlowerTile::new));

    public static RegistryObject<Block> infinityEgg = BLOCKS.register("infinity_egg", () -> new DragonEggBlock(Properties.from(Blocks.DRAGON_EGG)));
}
