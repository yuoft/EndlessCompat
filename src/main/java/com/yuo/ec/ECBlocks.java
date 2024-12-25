package com.yuo.ec;

import com.yuo.ec.Botania.AsgardFlowerTile;
import com.yuo.ec.Botania.InfinityManaSpreader;
import com.yuo.ec.Botania.InfinityPotato;
import com.yuo.ec.Botania.VariantEC;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractBlock.Properties;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DragonEggBlock;
import net.minecraft.entity.EntityType;
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

    private static final AbstractBlock.IExtendedPositionPredicate<EntityType<?>> NO_SPAWN = (state, world, pos, et) -> false;

    //Botania
    public static RegistryObject<Block> infinityPotato = BLOCKS.register("infinity_potato", InfinityPotato::new);
    public static RegistryObject<Block> asgardFlower = BLOCKS.register("asgard_flower", () -> new BlockSpecialFlower(Effects.HEALTH_BOOST, 360, FLOWER_PROPS, AsgardFlowerTile::new));
    public static RegistryObject<Block> asgardFlowerFloating = BLOCKS.register("asgard_flower_floating", () -> new BlockFloatingSpecialFlower(ModBlocks.FLOATING_PROPS, AsgardFlowerTile::new));
    public static RegistryObject<Block> infinityManaSpreader = BLOCKS.register("infinity_mana_spreader",
            () -> new InfinityManaSpreader(VariantEC.INFINITY, Properties.from(Blocks.BIRCH_WOOD).setAllowsSpawn(NO_SPAWN).hardnessAndResistance(5.0f, 9999.0f)));

    public static RegistryObject<Block> infinityEgg = BLOCKS.register("infinity_egg", () -> new DragonEggBlock(Properties.from(Blocks.DRAGON_EGG)));
}
