package com.yuo.ec.Botania;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.mana.PoolOverlayProvider;
import vazkii.botania.client.core.handler.ClientTickHandler;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.common.block.mana.ManaPoolBlock;
import vazkii.botania.common.block.mana.ManaPoolBlock.Variant;
import vazkii.botania.common.helper.ColorHelper;
import vazkii.botania.common.helper.MathHelper;
import vazkii.botania.common.helper.VecHelper;
import vazkii.botania.common.lib.ResourceLocationHelper;

import java.util.Objects;
import java.util.Random;

public class InfinityManaPoolRender implements BlockEntityRenderer<InfinityManaPoolTile> {
    public static int cartMana = -1;
    private final TextureAtlasSprite waterSprite;
    private final BlockRenderDispatcher blockRenderDispatcher;

    public InfinityManaPoolRender(BlockEntityRendererProvider.Context ctx) {
        this.blockRenderDispatcher = ctx.getBlockRenderDispatcher();
        this.waterSprite = Objects.requireNonNull(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocationHelper.prefix("block/mana_water")));
    }

    public void render(@Nullable InfinityManaPoolTile pool, float f, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        ms.pushPose();
        boolean fab = pool != null && ((ManaPoolBlock) pool.getBlockState().getBlock()).variant == Variant.FABULOUS;
        boolean diluted = pool != null && ((ManaPoolBlock) pool.getBlockState().getBlock()).variant == Variant.DILUTED;
        boolean creative = pool != null && ((ManaPoolBlock) pool.getBlockState().getBlock()).variant == Variant.CREATIVE;
        int insideUVStart = diluted ? 1 : 2;
        int insideUVEnd = 16 - insideUVStart;
        float poolBottom = (float) insideUVStart / 16.0F + 0.001F;
        float poolTop = (float) (diluted ? 5 : (creative ? 9 : 7)) / 16.0F;
        int maxMana;
        if (fab) {
            float time = (float) ClientTickHandler.ticksInGame + ClientTickHandler.partialTicks;
            time += (float) (new Random(pool.getBlockPos().getX() ^ pool.getBlockPos().getY() ^ pool.getBlockPos().getZ())).nextInt(100000);
            time *= 0.005F;
            maxMana = pool.getColor().map(ColorHelper::getColorValue).orElse(-1);
            int color = MathHelper.multiplyColor(Mth.hsvToRgb(Mth.frac(time), 0.6F, 1.0F), maxMana);
            int red = (color & 16711680) >> 16;
            int green = (color & '\uff00') >> 8;
            int blue = color & 255;
            BlockState state = pool.getBlockState();
            BakedModel model = this.blockRenderDispatcher.getBlockModel(state);
            VertexConsumer buffer = buffers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false));
            this.blockRenderDispatcher.getModelRenderer().renderModel(ms.last(), buffer, state, model, (float) red / 255.0F, (float) green / 255.0F, (float) blue / 255.0F, light, overlay);
        }

        if (pool != null) {
            Block below = pool.getLevel().getBlockState(pool.getBlockPos().below()).getBlock();
            if (below instanceof PoolOverlayProvider) {
                PoolOverlayProvider overlayProvider = (PoolOverlayProvider) below;
                ResourceLocation overlaySpriteId = overlayProvider.getIcon(pool.getLevel(), pool.getBlockPos());
                TextureAtlasSprite overlayIcon = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(overlaySpriteId);
                ms.pushPose();
                float alpha = (float) ((Math.sin((double) ((float) ClientTickHandler.ticksInGame + f) / 20.0) + 1.0) * 0.3 + 0.2);
                ms.translate(0.0F, poolBottom, 0.0F);
                ms.mulPose(VecHelper.rotateX(90.0F));
                VertexConsumer buffer = buffers.getBuffer(RenderHelper.ICON_OVERLAY);
                RenderHelper.renderIconCropped(ms, buffer, insideUVStart, insideUVStart, insideUVEnd, insideUVEnd, overlayIcon, 16777215, alpha, light);
                ms.popPose();
            }
        }

        int mana = pool == null ? cartMana : pool.getCurrentMana();
        maxMana = pool == null ? -1 : pool.getMaxMana();
        if (maxMana == -1) {
            maxMana = 1000000;
        }

        float manaLevel = (float) mana / (float) maxMana;
        if (manaLevel > 0.0F) {
            ms.pushPose();
            ms.translate(0.0F, Mth.clampedMap(manaLevel, 0.0F, 1.0F, poolBottom, poolTop), 0.0F);
            ms.mulPose(VecHelper.rotateX(90.0F));
            VertexConsumer buffer = buffers.getBuffer(RenderHelper.MANA_POOL_WATER);
            RenderHelper.renderIconCropped(ms, buffer, insideUVStart, insideUVStart, insideUVEnd, insideUVEnd, this.waterSprite, 16777215, 1.0F, light);
            ms.popPose();
        }

        ms.popPose();
        cartMana = -1;
    }
}