package com.yuo.ec;

import com.yuo.ec.Botania.*;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.BotaniaForgeClientCapabilities;
import vazkii.botania.api.block_entity.BindableSpecialFlowerBlockEntity;
import vazkii.botania.api.block_entity.BindableSpecialFlowerBlockEntity.BindableFlowerWandHud;
import vazkii.botania.client.render.block_entity.SpecialFlowerBlockEntityRenderer;
import vazkii.botania.common.lib.ResourceLocationHelper;
import vazkii.botania.forge.CapabilityUtil;
import vazkii.botania.forge.client.ForgeClientInitializer;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy{
    private BlockEntity referencedTE = null;

    public BlockEntity getRefrencedTE() {
        return referencedTE;
    }

    public void setRefrencedTE(BlockEntity tileEntity) {
        referencedTE = tileEntity;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void init(IEventBus modBus) {
        modBus.addListener(this::clientSetup);
    }

    private  void clientSetup(final FMLClientSetupEvent event) {
        //TESR 方块实体渲染
        event.enqueueWork(() ->{
            BlockEntityRenderers.register(ECTileTypes.INFINITY_POTATO.get(), InfinityPotatoRender::new);
            BlockEntityRenderers.register(ECTileTypes.ASGARD_FLOWER.get(), SpecialFlowerBlockEntityRenderer::new);
            BlockEntityRenderers.register(ECTileTypes.SOAR_LEANDER.get(), SpecialFlowerBlockEntityRenderer::new);
            BlockEntityRenderers.register(ECTileTypes.INFINITY_SPREADER.get(), InfinityTileSpreaderRender::new);
            BlockEntityRenderers.register(ECTileTypes.INFINITY_POOL.get(), InfinityManaPoolRender::new);
        });

        ItemBlockRenderTypes.setRenderLayer(ECBlocks.asgardFlower.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ECBlocks.asgardFlowerFloating.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ECBlocks.soarleander.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ECBlocks.soarleanderFloating.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ECBlocks.infinityManaSpreader.get(), RenderType.cutout());

        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addGenericListener(BlockEntity.class, ClientProxy::attachBeCapabilities); //魔力 hud
    }


    /**
     * 注册魔力条hud渲染能力
     */
    public static void attachBeCapabilities(AttachCapabilitiesEvent<BlockEntity> e) {
        BlockEntity be = e.getObject();
        if (be instanceof AsgardFlowerTile tile) {
            e.addCapability(ResourceLocationHelper.prefix("wand_hud"),
                    CapabilityUtil.makeProvider(BotaniaForgeClientCapabilities.WAND_HUD, new BindableFlowerWandHud(tile))
            );
        }
        if (be instanceof SoarLeanderFlowerTile tile) {
            e.addCapability(ResourceLocationHelper.prefix("wand_hud"),
                    CapabilityUtil.makeProvider(BotaniaForgeClientCapabilities.WAND_HUD, new BindableFlowerWandHud(tile))
            );
        }
        if (be instanceof InfinityTileSpreader tile) {
            e.addCapability(ResourceLocationHelper.prefix("wand_hud"),
                    CapabilityUtil.makeProvider(BotaniaForgeClientCapabilities.WAND_HUD, new InfinityTileSpreader.WandHud(tile))
            );
        }
        if (be instanceof InfinityManaPoolTile tile) {
            e.addCapability(ResourceLocationHelper.prefix("wand_hud"),
                    CapabilityUtil.makeProvider(BotaniaForgeClientCapabilities.WAND_HUD, new InfinityManaPoolTile.WandHud(tile))
            );
        }
    }
}
