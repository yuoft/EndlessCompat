package com.yuo.ec;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuo.ec.Botania.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.botania.api.recipe.ManaInfusionRecipe;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.client.gui.HUDHandler;
import vazkii.botania.common.helper.PlayerHelper;

@Mod.EventBusSubscriber(modid = EndlessCompat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ECEvents {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "hud", (gui, poseStack, partialTick, width, height) -> {
           onDrawScreenPost(poseStack); //注册物品合成hud
        });
    }

    public static void onDrawScreenPost(GuiGraphics gui){
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui && mc.player != null && mc.level != null) {
            ProfilerFiller profiler = mc.getProfiler();
            profiler.push("botania-hud");

            HitResult pos = mc.hitResult;
            if (pos instanceof BlockHitResult result) {
                BlockPos bpos = result.getBlockPos();
                BlockEntity tile = mc.level.getBlockEntity(bpos);
                if (PlayerHelper.hasAnyHeldItem(mc.player)) {
                    boolean alternateRecipeHudPosition = false;
                    if (tile instanceof InfinityManaPoolTile pool) {
                        if (!mc.player.getMainHandItem().isEmpty()) {
                            renderPoolRecipeHUD(gui, pool, mc.player.getMainHandItem(), alternateRecipeHudPosition);
                        }
                    }
                }

            }
        }
    }

    private static void renderPoolRecipeHUD(GuiGraphics gui, InfinityManaPoolTile tile, ItemStack stack, boolean alternateRecipeHudPosition) {
        Minecraft mc = Minecraft.getInstance();
        ProfilerFiller profiler = mc.getProfiler();
        profiler.push("poolRecipe");
        ManaInfusionRecipe recipe = tile.getMatchingRecipe(stack, tile.getLevel().getBlockState(tile.getBlockPos().below()));
        if (recipe != null) {
            int x = mc.getWindow().getGuiScaledWidth() / 2 - 11;
            int y = mc.getWindow().getGuiScaledHeight() / 2 + (alternateRecipeHudPosition ? -25 : 10);
            int u = tile.getCurrentMana() >= recipe.getManaToConsume() ? 0 : 22;
            int v = mc.player.getName().getString().equals("haighyorkie") && mc.player.isShiftKeyDown() ? 23 : 8;
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 771);
            RenderHelper.drawTexturedModalRect(gui, HUDHandler.manaBar, x, y, u, v, 22, 15);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            gui.renderItem(stack, x - 20, y);
            ItemStack result = recipe.getResultItem(mc.level.registryAccess());
            gui.renderItem(result, x + 26, y);
            gui.renderItemDecorations(mc.font, result, x + 26, y);
            RenderSystem.disableBlend();
        }

        profiler.pop();
    }
}
