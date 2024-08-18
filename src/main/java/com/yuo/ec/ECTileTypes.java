package com.yuo.ec;

import com.yuo.ec.Botania.AsgardFlowerTile;
import com.yuo.ec.Botania.InfinityPotatoTile;
import com.yuo.endless.Endless;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ECTileTypes {
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Endless.MOD_ID);

    //Botania
    public static final RegistryObject<TileEntityType<InfinityPotatoTile>> INFINITY_POTATO_TILE = TILE_ENTITIES.register("infinity_potato_tile",
            () -> TileEntityType.Builder.create(InfinityPotatoTile::new, ECBlocks.infinityPotato.get()).build(null));
    public static final RegistryObject<TileEntityType<AsgardFlowerTile>> ASGARD_FLOWER_TILE = TILE_ENTITIES.register("asgard_flower_tile",
            () -> TileEntityType.Builder.create(AsgardFlowerTile::new,
                    ECBlocks.asgardFlower.get(), ECBlocks.asgardFlowerFloating.get()).build(null));
}