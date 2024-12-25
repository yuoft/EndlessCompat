package com.yuo.ec.Botania;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import vazkii.botania.client.core.handler.ClientTickHandler;
import vazkii.botania.client.core.handler.MiscellaneousIcons;
import vazkii.botania.common.core.helper.ColorHelper;

import javax.annotation.Nonnull;
import java.util.Random;

public class InfinityTileSpreaderRender extends TileEntityRenderer<InfinityTileSpreader> {
    public InfinityTileSpreaderRender(TileEntityRendererDispatcher manager) {
        super(manager);
    }

    public void render(@Nonnull InfinityTileSpreader spreader, float partialTicks, MatrixStack ms, IRenderTypeBuffer buffers, int light, int overlay) {
        ms.push();
        ms.translate(0.5, 0.5, 0.5);
        Quaternion transform = Vector3f.YP.rotationDegrees(spreader.rotationX + 90.0F);
        transform.multiply(Vector3f.XP.rotationDegrees(spreader.rotationY));
        ms.rotate(transform);
        ms.translate(-0.5, -0.5, -0.5);
        double time = (double) ((float) ClientTickHandler.ticksInGame + partialTicks);
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        if (spreader.getVariant() == VariantEC.INFINITY) {
            int color = MathHelper.hsvToRGB((float) ((time * 5.0 + (double) (new Random((long) spreader.getPos().hashCode())).nextInt(10000)) % 360.0) / 360.0F, 0.4F, 0.9F);
            r = (float) (color >> 16 & 255) / 255.0F;
            g = (float) (color >> 8 & 255) / 255.0F;
            b = (float) (color & 255) / 255.0F;
        }

        IVertexBuilder buffer = buffers.getBuffer(RenderTypeLookup.func_239220_a_(spreader.getBlockState(), false));
        IBakedModel bakedModel = Minecraft.getInstance().getBlockRendererDispatcher().getModelForState(spreader.getBlockState());
        Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(ms.getLast(), buffer, spreader.getBlockState(), bakedModel, r, g, b, light, overlay);
        ms.push();
        ms.translate(0.5, 0.5, 0.5);
        ms.rotate(Vector3f.YP.rotationDegrees((float) time % 360.0F));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(0.0, (double) ((float) Math.sin(time / 20.0) * 0.05F), 0.0);
        IBakedModel cube = this.getInsideModel(spreader);
        Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(ms.getLast(), buffer, spreader.getBlockState(), cube, 1.0F, 1.0F, 1.0F, light, overlay);
        ms.pop();
        ms.translate(0.5, 1.5, 0.5);
        ItemStack stack = spreader.getItemHandler().getStackInSlot(0);
        if (!stack.isEmpty()) {
            ms.push();
            ms.translate(0.0, -1.0, -0.4675000011920929);
            ms.rotate(Vector3f.ZP.rotationDegrees(180.0F));
            ms.rotate(Vector3f.XP.rotationDegrees(180.0F));
            ms.scale(1.0F, 1.0F, 1.0F);
            Minecraft.getInstance().getItemRenderer().renderItem(stack, TransformType.NONE, light, overlay, ms, buffers);
            ms.pop();
        }
//        spreader.paddingColor = DyeColor.GREEN;
        if (spreader.paddingColor != null) {
            BlockState carpet = ((Block) ColorHelper.CARPET_MAP.apply(spreader.paddingColor)).getDefaultState();
            IBakedModel model = Minecraft.getInstance().getBlockRendererDispatcher().getModelForState(carpet);
            buffer = buffers.getBuffer(RenderTypeLookup.func_239220_a_(carpet, false));
            float f = 0.0625F;
            ms.push();
            ms.rotate(Vector3f.XP.rotationDegrees(90.0F));
            ms.translate(-0.5, (double) (0.5F - f), 0.5);
            Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(ms.getLast(), buffer, carpet, model, 1.0F, 1.0F, 1.0F, light, overlay);
            ms.pop();
            ms.push();
            ms.rotate(Vector3f.ZP.rotationDegrees(-90.0F));
            ms.translate(0.5, 0.5, (double) (-0.5F - f));
            Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(ms.getLast(), buffer, carpet, model, 1.0F, 1.0F, 1.0F, light, overlay);
            ms.pop();
            ms.push();
            ms.rotate(Vector3f.ZP.rotationDegrees(90.0F));
            ms.translate(-1.5, 0.5, (double) (-0.5F - f));
            Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(ms.getLast(), buffer, carpet, model, 1.0F, 1.0F, 1.0F, light, overlay);
            ms.pop();
            ms.push();
            ms.translate(-0.5, -0.5, (double) (-0.5F - f));
            Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(ms.getLast(), buffer, carpet, model, 1.0F, 1.0F, 1.0F, light, overlay);
            ms.pop();
            ms.push();
            ms.rotate(Vector3f.YP.rotationDegrees(180.0F));
            ms.translate(-0.5, (double) (-1.5F - f), (double) (-0.5F + f));
            Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(ms.getLast(), buffer, carpet, model, 1.0F, 1.0F, 1.0F, light, overlay);
            ms.pop();
        }

        ms.pop();
    }

    private IBakedModel getInsideModel(InfinityTileSpreader tile) {
        switch (tile.getVariant()) {
            case GAIA:
            case INFINITY:
                return MiscellaneousIcons.INSTANCE.gaiaSpreaderInside;
            case REDSTONE:
                return MiscellaneousIcons.INSTANCE.redstoneSpreaderInside;
            case ELVEN:
                return MiscellaneousIcons.INSTANCE.elvenSpreaderInside;
            case MANA:
            default:
                return MiscellaneousIcons.INSTANCE.manaSpreaderInside;
        }
    }
}