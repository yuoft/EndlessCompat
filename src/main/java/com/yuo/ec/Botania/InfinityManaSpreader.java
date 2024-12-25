package com.yuo.ec.Botania;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.botania.api.mana.ILens;
import vazkii.botania.api.wand.IWandHUD;
import vazkii.botania.api.wand.IWandable;
import vazkii.botania.api.wand.IWireframeAABBProvider;
import vazkii.botania.common.block.BlockModWaterloggable;
import vazkii.botania.common.core.helper.ColorHelper;
import vazkii.botania.common.item.ModItems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

//无尽魔力发射器
public class InfinityManaSpreader extends BlockModWaterloggable implements ITileEntityProvider, IWandable, IWandHUD, IWireframeAABBProvider {
    public final VariantEC variantEC;
    private static final VoxelShape RENDER_SHAPE = makeCuboidShape(1.0, 1.0, 1.0, 15.0, 15.0, 15.0);

    public InfinityManaSpreader(VariantEC v, Properties builder) {
        super(builder);
        this.variantEC = v;
    }

    @Nonnull
    @Override
    public VoxelShape getRenderShape(BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos) {
        return RENDER_SHAPE;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new InfinityTileSpreader();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction orientation = placer == null ? Direction.WEST : Direction.getFacingDirections(placer)[0].getOpposite();
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof InfinityTileSpreader) {
            InfinityTileSpreader spreader = (InfinityTileSpreader) tile;
            switch (orientation) {
                case DOWN:
                    spreader.rotationY = -90.0F;
                    break;
                case UP:
                    spreader.rotationY = 90.0F;
                    break;
                case NORTH:
                    spreader.rotationX = 270.0F;
                    break;
                case SOUTH:
                    spreader.rotationX = 90.0F;
                case WEST:
                default:
                    break;
                case EAST:
                    spreader.rotationX = 180.0F;
            }
        }
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof InfinityTileSpreader)) {
            return ActionResultType.PASS;
        } else {
            InfinityTileSpreader spreader = (InfinityTileSpreader)tile;
            ItemStack lens = spreader.getItemHandler().getStackInSlot(0);
            ItemStack heldItem = player.getHeldItem(hand);
            boolean isHeldItemLens = !heldItem.isEmpty() && heldItem.getItem() instanceof ILens;
            boolean wool = !heldItem.isEmpty() && ColorHelper.isWool(Block.getBlockFromItem(heldItem.getItem()));
            if (!heldItem.isEmpty() && heldItem.getItem() == ModItems.twigWand) {
                return ActionResultType.PASS;
            } else if (lens.isEmpty() && isHeldItemLens) {
                if (!player.abilities.isCreativeMode) {
                    player.setHeldItem(hand, ItemStack.EMPTY);
                }

                spreader.getItemHandler().setInventorySlotContents(0, heldItem.copy());
                return ActionResultType.SUCCESS;
            } else if (!lens.isEmpty() && !wool) {
                player.inventory.placeItemBackInInventory(player.world, lens);
                spreader.getItemHandler().setInventorySlotContents(0, ItemStack.EMPTY);
                return ActionResultType.SUCCESS;
            } else if (wool && spreader.paddingColor == null) {
                Block block = Block.getBlockFromItem(heldItem.getItem());
                spreader.paddingColor = ColorHelper.getWoolColor(block);
                if (!player.abilities.isCreativeMode) {
                    heldItem.shrink(1);
                    if (heldItem.isEmpty()) {
                        player.setHeldItem(hand, ItemStack.EMPTY);
                    }
                }

                world.playSound(player, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return ActionResultType.SUCCESS;
            } else if (!wool && (!heldItem.isEmpty() || spreader.paddingColor == null || !lens.isEmpty())) {
                return ActionResultType.PASS;
            } else {
                ItemStack pad = new ItemStack(ColorHelper.WOOL_MAP.apply(spreader.paddingColor));
                player.inventory.placeItemBackInInventory(player.world, pad);
                spreader.paddingColor = null;
                spreader.markDirty();
                world.playSound(player, pos, SoundEvents.BLOCK_WOOL_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return ActionResultType.SUCCESS;
            }
        }
    }

    @Override
    public void onReplaced(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof InfinityTileSpreader)) {
                return;
            }

            InfinityTileSpreader inv = (InfinityTileSpreader)tile;
            if (inv.paddingColor != null) {
                ItemStack padding = new ItemStack(ColorHelper.WOOL_MAP.apply(inv.paddingColor));
                world.addEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), padding));
            }

            InventoryHelper.dropInventoryItems(world, pos, inv.getItemHandler());
            super.onReplaced(state, world, pos, newState, isMoving);
        }

    }

    @Override
    public boolean onUsedByWand(PlayerEntity player, ItemStack stack, World world, BlockPos pos, Direction side) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof InfinityTileSpreader) {
            ((InfinityTileSpreader) tile).onWanded(player);
        }
        return true;
    }

    @Nonnull
    @Override
    public TileEntity createNewTileEntity(@Nonnull IBlockReader world) {
        return new InfinityTileSpreader();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void renderHUD(MatrixStack ms, Minecraft mc, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof InfinityTileSpreader) {
            ((InfinityTileSpreader) tile).renderHUD(ms, mc);
        }
    }

    @Override
    public List<AxisAlignedBB> getWireframeAABB(World world, BlockPos blockPos) {
        return ImmutableList.of((new AxisAlignedBB(blockPos)).shrink(0.0625));
    }
}
