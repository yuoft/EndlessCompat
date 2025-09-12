package com.yuo.ec;

import com.yuo.ec.Botania.*;
import com.yuo.endless.Endless;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ECTileTypes {
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Endless.MOD_ID);

    //Botania
    public static final RegistryObject<BlockEntityType<InfinityPotatoTile>> INFINITY_POTATO = TILE_ENTITIES.register("infinity_potato_tile",
            () -> BlockEntityType.Builder.of(InfinityPotatoTile::new, ECBlocks.infinityPotato.get()).build(null));
    public static final RegistryObject<BlockEntityType<AsgardFlowerTile>> ASGARD_FLOWER = TILE_ENTITIES.register("asgard_flower_tile",
            () -> BlockEntityType.Builder.of(AsgardFlowerTile::new,
                    ECBlocks.asgardFlower.get(), ECBlocks.asgardFlowerFloating.get()).build(null));
    public static final RegistryObject<BlockEntityType<SoarLeanderFlowerTile>> SOAR_LEANDER = TILE_ENTITIES.register("soarleander_flower_tile",
            () -> BlockEntityType.Builder.of(SoarLeanderFlowerTile::new,
                    ECBlocks.soarleander.get(), ECBlocks.soarleanderFloating.get()).build(null));
    public static final RegistryObject<BlockEntityType<InfinityTileSpreader>> INFINITY_SPREADER = TILE_ENTITIES.register("infinity_spreader_tile",
            () -> BlockEntityType.Builder.of(InfinityTileSpreader::new,
                    ECBlocks.infinityManaSpreader.get()).build(null));
    public static final RegistryObject<BlockEntityType<InfinityManaPoolTile>> INFINITY_POOL = TILE_ENTITIES.register("infinity_pool_tile",
            () -> BlockEntityType.Builder.of(InfinityManaPoolTile::new,
                    ECBlocks.infinityManaPool.get()).build(null));
}