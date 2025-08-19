package com.yuo.ec;

import com.yuo.ec.Botania.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.FloatingSpecialFlowerBlock;
import vazkii.botania.xplat.XplatAbstractions;

public class ECBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EndlessCompat.MOD_ID);
    public static final Properties FLOWER_PROPS = Properties.copy(Blocks.POPPY);

    private static final BlockBehaviour.StateArgumentPredicate<EntityType<?>> NO_SPAWN = (state, world, pos, et) -> false;
    public static final BlockEntityType<AsgardFlowerTile> ASGARD = XplatAbstractions.INSTANCE.createBlockEntityType(AsgardFlowerTile::new);
    public static final BlockEntityType<InfinityTileSpreader> INFINITY_TILE = XplatAbstractions.INSTANCE.createBlockEntityType(InfinityTileSpreader::new);

    //Botania
    public static RegistryObject<Block> infinityPotato = BLOCKS.register("infinity_potato", InfinityPotato::new);
    public static RegistryObject<Block> asgardFlower = BLOCKS.register("asgard_flower", () -> new ModFlowerBlock(MobEffects.HEALTH_BOOST, 360, FLOWER_PROPS, () -> ASGARD));
    public static RegistryObject<Block> asgardFlowerFloating = BLOCKS.register("asgard_flower_floating", () -> new FloatingSpecialFlowerBlock(BotaniaBlocks.FLOATING_PROPS, () -> ASGARD));
    public static RegistryObject<Block> infinityManaSpreader = BLOCKS.register("infinity_mana_spreader",
            () -> new InfinityManaSpreader(VariantEC.INFINITY, Properties.copy(Blocks.BIRCH_WOOD).isValidSpawn(NO_SPAWN).strength(5.0f, 9999.0f)));

    public static RegistryObject<Block> infinityEgg = BLOCKS.register("infinity_egg", () -> new DragonEggBlock(Properties.copy(Blocks.DRAGON_EGG)));
}
