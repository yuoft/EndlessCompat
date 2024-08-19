package com.yuo.ec;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("endless_compat")
@Mod.EventBusSubscriber(modid = EndlessCompat.MOD_ID)
public class EndlessCompat
{
    public static final String MOD_ID = "endless_compat";
    public static boolean ISWR = false;
    public static CommonProxy PROXY = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    public EndlessCompat() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        checkMods();
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        ECItems.ITEMS.register(modEventBus);
        ECItems.BLOOD_ORBS.register(modEventBus);
        ECBlocks.BLOCKS.register(modEventBus);
        ECTileTypes.TILE_ENTITIES.register(modEventBus);
        PROXY.init();
    }

    private void setup(final FMLCommonSetupEvent event) {
        ECRecipeManager.addRecipe();
        if (ISWR) ECRecipeManager.addAltar();
    }

    private void checkMods() {
        ISWR = checkMod("witchery_rewitched");
    }

    /**
     * 检查模组是否存在
     * @param modId 模组id
     * @return 存在 true
     */
    private boolean checkMod(String modId){
        return ModList.get().isLoaded(modId);
    }

//    @SubscribeEvent
//    public static void addReloadListenerEvent(AddReloadListenerEvent event) {
//        event.addListener(new AltarUpgradeManager("altar/upgrades"));
//    }
}
