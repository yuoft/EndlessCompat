package com.yuo.ec.Botania;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuo.endless.Endless;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import vazkii.botania.api.state.BotaniaStateProperties;
import vazkii.botania.client.core.handler.ClientTickHandler;
import vazkii.botania.client.core.handler.MiscellaneousModels;
import vazkii.botania.common.block.block_entity.mana.ManaSpreaderBlockEntity;
import vazkii.botania.common.helper.ColorHelper;
import vazkii.botania.common.helper.VecHelper;

import javax.annotation.Nonnull;
import java.util.Random;

public class InfinityTileSpreaderRender implements BlockEntityRenderer<InfinityTileSpreader> {
    public InfinityTileSpreaderRender(BlockEntityRendererProvider.Context manager) {
    }

    public void render(@Nonnull InfinityTileSpreader spreader, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        Quaternionf transform = VecHelper.rotateY(spreader.rotationX + 90.0F);
        transform.mul(VecHelper.rotateX(spreader.rotationY));
        ms.mulPose(transform);
        ms.translate(-0.5, -0.5, -0.5);
        double time = (double) ((float) ClientTickHandler.ticksInGame + partialTicks);
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        if (spreader.getVariant() == VariantEC.INFINITY) {
            int color = Mth.hsvToRgb((float) ((time * 5.0 + (double) (new Random((long) spreader.getBlockPos().hashCode())).nextInt(10000)) % 360.0) / 360.0F, 0.4F, 0.9F);
            r = (float) (color >> 16 & 255) / 255.0F;
            g = (float) (color >> 8 & 255) / 255.0F;
            b = (float) (color & 255) / 255.0F;
        }

        VertexConsumer buffer = buffers.getBuffer(ItemBlockRenderTypes.getRenderType(spreader.getBlockState(), false));
        BakedModel spreaderModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(spreader.getBlockState());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer, spreader.getBlockState(), spreaderModel, r, g, b, light, overlay);
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(VecHelper.rotateY((float)time % 360.0F));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(0.0F, (float)Math.sin(time / 20.0) * 0.05F, 0.0F);
        BakedModel core = this.getCoreModel(spreader);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer, spreader.getBlockState(), core, 1.0F, 1.0F, 1.0F, light, overlay);
        ms.popPose();
        ItemStack stack = spreader.getItemHandler().getItem(0);
        if (!stack.isEmpty()) {
            ms.pushPose();
            ms.translate(0.5F, 0.5F, 0.094F);
            ms.mulPose(VecHelper.rotateZ(180.0F));
            ms.mulPose(VecHelper.rotateX(180.0F));
            ms.scale(0.997F, 0.997F, 1.0F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, overlay, ms, buffers, spreader.getLevel(), 0);
            ms.popPose();
        }
        BakedModel scaffolding;
        if (spreader.paddingColor != null) {
            ms.pushPose();
            ms.translate(0.5F, 0.5F, 0.5F);
            ms.mulPose(VecHelper.rotateX(-90.0F));
            ms.mulPose(VecHelper.rotateY(180.0F));
            ms.translate(-0.5F, -0.5F, -0.5F);
            scaffolding = this.getPaddingModel(spreader.paddingColor);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer, spreader.getBlockState(), scaffolding, r, g, b, light, overlay);
            ms.popPose();
        }

        ms.popPose();
        if (spreader.getBlockState().getValue(BotaniaStateProperties.HAS_SCAFFOLDING)) {
            scaffolding = this.getScaffoldingModel(spreader);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer, spreader.getBlockState(), scaffolding, r, g, b, light, overlay);
        }
    }

    private BakedModel getCoreModel(InfinityTileSpreader tile) {
        BakedModel var10000;
        switch (tile.getVariant()) {
            case GAIA, INFINITY -> var10000 = MiscellaneousModels.INSTANCE.gaiaSpreaderCore;
            case REDSTONE -> var10000 = MiscellaneousModels.INSTANCE.redstoneSpreaderCore;
            case ELVEN -> var10000 = MiscellaneousModels.INSTANCE.elvenSpreaderCore;
            case MANA -> var10000 = MiscellaneousModels.INSTANCE.manaSpreaderCore;
            default -> throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    private BakedModel getPaddingModel(DyeColor color) {
        return MiscellaneousModels.INSTANCE.spreaderPaddings.get(color);
    }

    private BakedModel getScaffoldingModel(InfinityTileSpreader tile) {
        return switch (tile.getVariant()) {
            case GAIA, INFINITY -> MiscellaneousModels.INSTANCE.gaiaSpreaderScaffolding;
            case REDSTONE, MANA -> MiscellaneousModels.INSTANCE.manaSpreaderScaffolding;
            case ELVEN -> MiscellaneousModels.INSTANCE.elvenSpreaderScaffolding;
            default -> throw new IncompatibleClassChangeError();
        };
    }
}