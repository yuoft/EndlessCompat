package com.yuo.ec.Botania;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuo.endless.Endless;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaternionf;
import vazkii.botania.api.item.TinyPotatoRenderEvent;
import vazkii.botania.client.core.handler.MiscellaneousModels;
import vazkii.botania.client.core.proxy.ClientProxy;
import vazkii.botania.common.handler.ContributorList;
import vazkii.botania.common.helper.VecHelper;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.item.equipment.bauble.FlugelTiaraItem.ClientLogic;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.regex.Pattern;

public class InfinityPotatoRender implements BlockEntityRenderer<InfinityPotatoTile> {
    public static final String DEFAULT = "default";
    public static final String HALLOWEEN = "halloween";
    private static final Pattern ESCAPED = Pattern.compile("[^a-z0-9/._-]");
    private final BlockRenderDispatcher blockRenderDispatcher;
    public InfinityPotatoRender(BlockEntityRendererProvider.Context manager) {
        this.blockRenderDispatcher = manager.getBlockRenderDispatcher();
    }
    private static boolean matches(String name, String match) {
        return name.equals(match) || name.startsWith(match + " ");
    }

    private static String removeFromFront(String name, String match) {
        return name.substring(match.length()).trim();
    }

    public static BakedModel getModelFromDisplayName(Component displayName) {
        StringBuilder nameBuilder = new StringBuilder();
        ItemBlockPotato.isEnchantedName(displayName, nameBuilder);
        return getModel(nameBuilder.toString().toLowerCase(Locale.ROOT));
    }

    private static BakedModel getModel(String name) {
        ModelManager mm = Minecraft.getInstance().getModelManager();
        ResourceLocation location = taterLocation(name);
        BakedModel model = mm.getModel(location);
        if (model == mm.getMissingModel()) {
            if (ClientProxy.dootDoot) {
                return mm.getModel(taterLocation(HALLOWEEN));
            } else {
                return mm.getModel(taterLocation(DEFAULT));
            }
        }
        return model;
    }

    private static ResourceLocation taterLocation(String name) {
        return ResourceLocation.fromNamespaceAndPath(Endless.MOD_ID, "textures/block/infinity_potato");
    }

    private static String normalizeName(String name) {
        return ESCAPED.matcher(name).replaceAll("_");
    }

    @Override
    public void render(@Nonnull InfinityPotatoTile potato, float partialTicks, PoseStack ms, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        ms.pushPose();

        StringBuilder nameBuilder = new StringBuilder();
        boolean enchanted = ItemBlockPotato.isEnchantedName(potato.name, nameBuilder);
        String name = nameBuilder.toString().toLowerCase(Locale.ROOT);
        RenderType layer = Sheets.translucentCullBlockSheet();
//        IBakedModel model = getModel(name);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState blockState = potato.getBlockState();
        BakedModel model = dispatcher.getBlockModel(blockState);

        ms.translate(0.5F, 0F, 0.5F);
        Direction potatoFacing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        float rotY = 0;
        switch (potatoFacing) {
            default:
            case SOUTH:
                rotY = 180F;
                break;
            case NORTH:
                break;
            case EAST:
                rotY = 90F;
                break;
            case WEST:
                rotY = 270F;
                break;
        }
        ms.mulPose(VecHelper.rotateY(rotY));

        float jump = potato.jumpTicks;
        if (jump > 0) {
            jump -= partialTicks;
        }

        float up = (float) Math.abs(Math.sin(jump / 10 * Math.PI)) * 0.2F;
        float rotZ = (float) Math.sin(jump / 10 * Math.PI) * 2;
        float wiggle = (float) Math.sin(jump / 10 * Math.PI) * 0.05F;

        ms.translate(wiggle, up, 0F);
        ms.mulPose(VecHelper.rotateZ(rotZ));

        boolean render = !(name.equals("mami") || name.equals("soaryn") || name.equals("eloraam") && jump != 0);
        if (render) {
            ms.pushPose();
            ms.translate(-0.5F, 0, -0.5F);
            VertexConsumer buffer = buffers.getBuffer(layer);

            renderModel(ms, buffer, light, overlay, model);
            ms.popPose();
        }

        ms.translate(0F, 1.5F, 0F);
        ms.pushPose();
        ms.mulPose(VecHelper.rotateZ(180.0F));
        renderItems(potato, potatoFacing, name, partialTicks, ms, buffers, light, overlay);

        ms.pushPose();
        MinecraftForge.EVENT_BUS.post(new TinyPotatoRenderEvent(potato, potato.name, partialTicks, ms, buffers, light, overlay));
        ms.popPose();
        ms.popPose();

        ms.mulPose(VecHelper.rotateZ(-rotZ));
        ms.mulPose(VecHelper.rotateY(rotY));

        renderName(potato, name, ms, buffers, light);
        ms.popPose();
    }

    private void renderName(InfinityPotatoTile potato, String name, PoseStack ms, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        HitResult pos = mc.hitResult;
        if (Minecraft.renderNames() && !name.isEmpty() && pos != null && pos.getType() == Type.BLOCK && potato.getBlockPos().equals(((BlockHitResult)pos).getBlockPos())) {
            ms.pushPose();
            ms.translate(0.0F, -0.6F, 0.0F);
            ms.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            float f1 = 0.02666667F;
            ms.scale(-f1, -f1, f1);
            int halfWidth = mc.font.width(potato.name.getString()) / 2;
            float opacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            int opacityRGB = (int)(opacity * 255.0F) << 24;
            mc.font.drawInBatch(potato.name, (float)(-halfWidth), 0.0F, 553648127, false, ms.last().pose(), buffers, DisplayMode.SEE_THROUGH, opacityRGB, light);
            mc.font.drawInBatch(potato.name, (float)(-halfWidth), 0.0F, -1, false, ms.last().pose(), buffers, DisplayMode.NORMAL, 0, light);
            if (name.equals("pahimar") || name.equals("soaryn")) {
                ms.translate(0.0F, 14.0F, 0.0F);
                String str = name.equals("pahimar") ? "[WIP]" : "(soon)";
                halfWidth = mc.font.width(str) / 2;
                mc.font.drawInBatch(str, (float)(-halfWidth), 0.0F, 553648127, false, ms.last().pose(), buffers, DisplayMode.SEE_THROUGH, opacityRGB, light);
                mc.font.drawInBatch(str, (float)(-halfWidth), 0.0F, -1, false, ms.last().pose(), buffers, DisplayMode.SEE_THROUGH, 0, light);
            }

            ms.popPose();
        }
    }

    private void renderItems(InfinityPotatoTile potato, Direction facing, String name, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        ms.pushPose();
        ms.mulPose(VecHelper.rotateZ(180.0F));
        ms.translate(0.0F, -1.0F, 0.0F);
        float s = 0.2857143F;
        ms.scale(s, s, s);

        for (int i = 0; i < potato.inventorySize(); i++) {
            ItemStack stack = potato.getItemHandler().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            ms.pushPose();
            Direction side = Direction.values()[i];
            if (side.getAxis() != Axis.Y) {
                float sideAngle = side.toYRot() - facing.toYRot();
                side = Direction.fromYRot(sideAngle);
            }

            boolean block = stack.getItem() instanceof BlockItem;
            boolean mySon = stack.getItem() instanceof ItemBlockPotato;

            switch (side) {
                case UP:
                    if (mySon) {
                        ms.translate(0F, 2.0F, 1.0F);
                    } else if (block) {
                        ms.translate(0F, 0.6F, 0.5F);
                    }
                    ms.translate(0F, 0.6F, -0.4F);
                    break;
                case DOWN:
                    ms.translate(0, -2.3F, -1.35F);
                    if (mySon) {
                        ms.translate(0, 1.5F, 0.8F);
                    } else if (block) {
                        ms.translate(0, 1, 0.6F);
                    }
                    break;
                case NORTH:
                    ms.translate(0, -1.2F, 0.48F);
                    if (mySon) {
                        ms.translate(0, 2.6, 0.68F);
                    } else if (block) {
                        ms.translate(0, 1, 0.6F);
                    }
                    break;
                case SOUTH:
                    ms.translate(0, -1.2F, -1.35F);
                    if (mySon) {
                        ms.translate(0, 3.4F, 1.2F);
                    } else if (block) {
                        ms.translate(0, 1.0F, 0.5F);
                    }
                    break;
                case EAST:
                    if (mySon) {
                        ms.translate(-0.8F, 1.6F, -0.2F);
                    } else if (block) {
                        ms.translate(-0.2F, 0.8F, -0.4F);
                    } else {
                        ms.mulPose(VecHelper.rotateY(-90.0F));
                    }
                    ms.translate(-0.8F, -1.0F, 0.48F);
                    break;
                case WEST:
                    if (mySon) {
                        ms.translate(2.5F, 1.6F, 1.5F);
                    } else if (block) {
                        ms.translate(1.92F, 0.8F, 1.3F);
                    } else {
                        ms.mulPose(VecHelper.rotateY(-90.0F));
                    }
                    ms.translate(-0.8F, -1.0F, -1.34F);
                    break;
            }

            if (mySon) {
                ms.scale(1.1F, 1.1F, 1.1F);
            } else if (block) {
                ms.scale(0.5F, 0.5F, 0.5F);
            }
            if (block && side == Direction.NORTH) {
                ms.mulPose(VecHelper.rotateY(180.0F));
            }
            renderItem(ms, buffers, potato.getLevel(), light, overlay, stack);
            ms.popPose();
        }
        ms.popPose();

        ms.pushPose();
        if (!name.isEmpty()) {
            ContributorList.firstStart();

            float scale = 1F / 4F;
            ms.translate(0F, 1F, 0F);
            ms.scale(scale, scale, scale);
            ItemStack icon;
            switch (name) {
                case "phi":
                case "vazkii":
                    ms.pushPose();
                    ms.translate(-0.15, 0.1, 0.4);
                    ms.mulPose(VecHelper.rotateY(90.0F));
                    ms.mulPose((new Quaternionf()).rotateAxis(VecHelper.toRadians(20.0F), 1.0F, 0.0F, 1.0F));
                    renderModel(ms, buffers, light, overlay, MiscellaneousModels.INSTANCE.phiFlowerModel);
                    ms.popPose();

                    if (name.equals("vazkii")) {
                        ms.scale(1.25F, 1.25F, 1.25F);
                        ms.mulPose(VecHelper.rotateX(180.0F));
                        ms.mulPose(VecHelper.rotateY(-90.0F));
                        ms.translate(0.2, -1.25, 0);
                        renderModel(ms, buffers, light, overlay, MiscellaneousModels.INSTANCE.nerfBatModel);
                    }
                    break;
                case "haighyorkie":
                    ms.scale(1.25F, 1.25F, 1.25F);
                    ms.mulPose(VecHelper.rotateZ(180.0F));
                    ms.mulPose(VecHelper.rotateY(-90.0F));
                    ms.translate(-0.5F, -1.2F, -0.075F);
                    renderModel(ms, buffers, light, overlay, MiscellaneousModels.INSTANCE.goldfishModel);
                    break;
                case "martysgames":
                case "marty":
                    ms.scale(0.7F, 0.7F, 0.7F);
                    ms.mulPose(VecHelper.rotateZ(180.0F));
                    ms.translate(-0.3F, -2.7F, -1.2F);
                    ms.mulPose(VecHelper.rotateZ(15.0F));
                    renderItem(ms, buffers, potato.getLevel(), light, overlay, new ItemStack(BotaniaItems.infiniteFruit, 1).setHoverName(Component.literal("das boot")));
                    break;
                case "jibril":
                    ms.scale(1.5F, 1.5F, 1.5F);
                    ms.translate(0.0F, 0.8F, 0.0F);
                    ClientLogic.renderHalo((HumanoidModel)null, (LivingEntity)null, ms, buffers, partialTicks);
                    break;
                case "kingdaddydmac":
                    ms.scale(0.5F, 0.5F, 0.5F);
                    ms.mulPose(VecHelper.rotateZ(180.0F));
                    ms.mulPose(VecHelper.rotateY(90.0F));
                    ms.pushPose();
                    ms.translate(0.0F, -2.5F, 0.65F);
                    icon = new ItemStack(BotaniaItems.manaRing);
                    this.renderItem(ms, buffers, potato.getLevel(), light, overlay, icon);
                    ms.translate(0.0F, 0.0F, -4.0F);
                    this.renderItem(ms, buffers, potato.getLevel(), light, overlay, icon);
                    ms.popPose();
                    ms.translate(1.5, -4.0, -2.5);
                    this.renderBlock(ms, buffers, light, overlay, Blocks.CAKE);
                    break;
                default:
                    icon = ContributorList.getFlower(name);
                    if (!icon.isEmpty()) {
                        ms.mulPose(VecHelper.rotateX(180.0F));
                        ms.mulPose(VecHelper.rotateY(180.0F));
                        ms.translate(0.0, -0.78, -0.5);
                        Minecraft.getInstance().getItemRenderer().renderStatic(icon, ItemDisplayContext.HEAD, light, overlay, ms, buffers, potato.getLevel(), 0);
                    }
                    break;
            }
        }
        ms.popPose();
    }

    private void renderModel(PoseStack ms, MultiBufferSource buffers, int light, int overlay, BakedModel model) {
        this.renderModel(ms, buffers.getBuffer(Sheets.translucentCullBlockSheet()), light, overlay, model);
    }

    private void renderModel(PoseStack ms, VertexConsumer buffer, int light, int overlay, BakedModel model) {
        this.blockRenderDispatcher.getModelRenderer().renderModel(ms.last(), buffer, (BlockState)null, model, 1.0F, 1.0F, 1.0F, light, overlay);
    }

    private void renderItem(PoseStack ms, MultiBufferSource buffers, @org.jetbrains.annotations.Nullable Level level, int light, int overlay, ItemStack stack) {
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.HEAD, light, overlay, ms, buffers, level, 0);
    }

    private void renderBlock(PoseStack ms, MultiBufferSource buffers, int light, int overlay, Block block) {
        this.blockRenderDispatcher.renderSingleBlock(block.defaultBlockState(), ms, buffers, light, overlay);
    }
}
