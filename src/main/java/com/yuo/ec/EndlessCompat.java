package com.yuo.ec;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.block.Wandable;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.lib.ResourceLocationHelper;
import vazkii.botania.forge.CapabilityUtil;

@Mod("endless_compat")
@Mod.EventBusSubscriber(modid = EndlessCompat.MOD_ID)
public class EndlessCompat
{
    public static final String MOD_ID = "endless_compat";
    public static CommonProxy PROXY = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    public EndlessCompat() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        ECItems.ITEMS.register(modEventBus);
        ECItems.BLOOD_ORBS.register(modEventBus);
        ECBlocks.BLOCKS.register(modEventBus);
        ECTabs.TABS.register(modEventBus);
        ECTileTypes.TILE_ENTITIES.register(modEventBus);
        PROXY.init(modEventBus);
    }

    private void setup(final FMLCommonSetupEvent event) {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addGenericListener(BlockEntity.class, this::attachBeCaps);
    }

    private void attachBeCaps(AttachCapabilitiesEvent<BlockEntity> e) {
        BlockEntity be = e.getObject();

        if (be.getType() == ECTileTypes.INFINITY_POOL.get())  { //注册魔力池识别 和 模式转换识别 能力
            e.addCapability(ResourceLocationHelper.prefix("mana_receiver"), CapabilityUtil.makeProvider(BotaniaForgeCapabilities.MANA_RECEIVER, (ManaReceiver)be));
            e.addCapability(ResourceLocationHelper.prefix("wandable"), CapabilityUtil.makeProvider(BotaniaForgeCapabilities.WANDABLE, (Wandable)be));
        }

    }
}
