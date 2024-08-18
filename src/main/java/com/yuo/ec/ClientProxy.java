package com.yuo.ec;

import com.yuo.ec.Botania.InfinityPotatoRender;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import vazkii.botania.client.render.tile.RenderTileSpecialFlower;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy{
    private TileEntity referencedTE = null;

    public TileEntity getRefrencedTE() {
        return referencedTE;
    }

    public void setRefrencedTE(TileEntity tileEntity) {
        referencedTE = tileEntity;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::clientSetup);
    }

    private  void clientSetup(final FMLClientSetupEvent event) {
        //TESR 方块实体渲染
        event.enqueueWork(() ->{
            ClientRegistry.bindTileEntityRenderer(ECTileTypes.INFINITY_POTATO_TILE.get(), InfinityPotatoRender::new);
            ClientRegistry.bindTileEntityRenderer(ECTileTypes.ASGARD_FLOWER_TILE.get(), RenderTileSpecialFlower::new);
        });

        RenderTypeLookup.setRenderLayer(ECBlocks.asgardFlower.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(ECBlocks.asgardFlowerFloating.get(), RenderType.getCutout());
    }

}
