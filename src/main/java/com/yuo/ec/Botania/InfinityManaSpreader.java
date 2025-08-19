package com.yuo.ec.Botania;

import com.yuo.ec.ECTileTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.mana.BasicLensItem;
import vazkii.botania.api.state.BotaniaStateProperties;
import vazkii.botania.common.block.BotaniaWaterloggedBlock;
import vazkii.botania.common.block.block_entity.mana.ManaSpreaderBlockEntity;
import vazkii.botania.common.block.mana.ManaSpreaderBlock;
import vazkii.botania.common.handler.BotaniaSounds;
import vazkii.botania.common.helper.ColorHelper;
import vazkii.botania.common.item.WandOfTheForestItem;

//无尽魔力发射器
public class InfinityManaSpreader extends BotaniaWaterloggedBlock implements EntityBlock {
    public final VariantEC variantEC;
    private static final VoxelShape SHAPE = box(2.0, 2.0, 2.0, 14.0, 14.0, 14.0);
    private static final VoxelShape SHAPE_PADDING = box(1.0, 1.0, 1.0, 15.0, 15.0, 15.0);
    private static final VoxelShape SHAPE_SCAFFOLDING = box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public InfinityManaSpreader(VariantEC v, Properties builder) {
        super(builder);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue(BotaniaStateProperties.HAS_SCAFFOLDING, false));
        this.variantEC = v;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{BotaniaStateProperties.HAS_SCAFFOLDING});
    }

    public @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        if ((Boolean)blockState.getValue(BotaniaStateProperties.HAS_SCAFFOLDING)) {
            return SHAPE_SCAFFOLDING;
        } else {
            BlockEntity be = blockGetter.getBlockEntity(blockPos);
            VoxelShape var10000;
            if (be instanceof InfinityTileSpreader) {
                InfinityTileSpreader spreader = (InfinityTileSpreader)be;
                if (spreader.paddingColor != null) {
                    var10000 = SHAPE_PADDING;
                    return var10000;
                }
            }

            var10000 = SHAPE;
            return var10000;
        }
    }

    public @NotNull VoxelShape getOcclusionShape(BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        return SHAPE;
    }

    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public RenderShape getRenderShape(BlockState p_60550_) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable LivingEntity placer, ItemStack stack) {
        Direction orientation = placer == null ? Direction.WEST : Direction.orderedByNearest(placer)[0].getOpposite();
        BlockEntity tile = world.getBlockEntity(pos);
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
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof ManaSpreaderBlockEntity spreader) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (heldItem.getItem() instanceof WandOfTheForestItem) {
                return InteractionResult.PASS;
            } else {
                boolean mainHandEmpty = player.getMainHandItem().isEmpty();
                ItemStack lens = spreader.getItemHandler().getItem(0);
                boolean playerHasLens = heldItem.getItem() instanceof BasicLensItem;
                boolean lensIsSame = playerHasLens && ItemStack.isSameItemSameTags(heldItem, lens);
                ItemStack wool = spreader.paddingColor != null ? new ItemStack((ItemLike)ColorHelper.WOOL_MAP.apply(spreader.paddingColor)) : ItemStack.EMPTY;
                boolean playerHasWool = ColorHelper.isWool(Block.byItem(heldItem.getItem()));
                boolean woolIsSame = playerHasWool && ItemStack.isSameItemSameTags(heldItem, wool);
                boolean playerHasScaffolding = !heldItem.isEmpty() && heldItem.is(Items.SCAFFOLDING);
                boolean shouldInsert = playerHasLens && !lensIsSame || playerHasWool && !woolIsSame || playerHasScaffolding && !(Boolean)state.getValue(BotaniaStateProperties.HAS_SCAFFOLDING);
                ItemStack scaffolding;
                if (shouldInsert) {
                    if (playerHasLens) {
                        scaffolding = heldItem.split(1);
                        if (!lens.isEmpty()) {
                            player.getInventory().placeItemBackInInventory(lens);
                        }

                        spreader.getItemHandler().setItem(0, scaffolding);
                        world.playSound(player, pos, BotaniaSounds.spreaderAddLens, SoundSource.BLOCKS, 1.0F, 1.0F);
                    } else if (playerHasWool) {
                        Block woolBlock = Block.byItem(heldItem.getItem());
                        heldItem.shrink(1);
                        if (spreader.paddingColor != null) {
                            ItemStack spreaderWool = new ItemStack((ItemLike)ColorHelper.WOOL_MAP.apply(spreader.paddingColor));
                            player.getInventory().placeItemBackInInventory(spreaderWool);
                        }

                        spreader.paddingColor = ColorHelper.getWoolColor(woolBlock);
                        spreader.setChanged();
                        world.playSound(player, pos, BotaniaSounds.spreaderCover, SoundSource.BLOCKS, 1.0F, 1.0F);
                    } else {
                        world.setBlockAndUpdate(pos, (BlockState)state.setValue(BotaniaStateProperties.HAS_SCAFFOLDING, true));
                        world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
                        if (!player.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }

                        world.playSound(player, pos, BotaniaSounds.spreaderScaffold, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }

                    return InteractionResult.sidedSuccess(world.isClientSide());
                } else if ((Boolean)state.getValue(BotaniaStateProperties.HAS_SCAFFOLDING) && player.isSecondaryUseActive()) {
                    if (!player.getAbilities().instabuild) {
                        scaffolding = new ItemStack(Items.SCAFFOLDING);
                        player.getInventory().placeItemBackInInventory(scaffolding);
                    }

                    world.setBlockAndUpdate(pos, (BlockState)state.setValue(BotaniaStateProperties.HAS_SCAFFOLDING, false));
                    world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
                    world.playSound(player, pos, BotaniaSounds.spreaderUnScaffold, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return InteractionResult.sidedSuccess(world.isClientSide());
                } else if (!lens.isEmpty() && (mainHandEmpty || lensIsSame)) {
                    player.getInventory().placeItemBackInInventory(lens);
                    spreader.getItemHandler().setItem(0, ItemStack.EMPTY);
                    world.playSound(player, pos, BotaniaSounds.spreaderRemoveLens, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return InteractionResult.sidedSuccess(world.isClientSide());
                } else if (spreader.paddingColor == null || !mainHandEmpty && !woolIsSame) {
                    return InteractionResult.PASS;
                } else {
                    player.getInventory().placeItemBackInInventory(wool);
                    spreader.paddingColor = null;
                    spreader.setChanged();
                    world.playSound(player, pos, BotaniaSounds.spreaderUncover, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return InteractionResult.sidedSuccess(world.isClientSide());
                }
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (!(tile instanceof InfinityTileSpreader)) {
                return;
            }

            InfinityTileSpreader spreader = (InfinityTileSpreader)tile;
            ItemStack scaffolding;
            if (spreader.paddingColor != null) {
                scaffolding = new ItemStack((ItemLike)ColorHelper.WOOL_MAP.apply(spreader.paddingColor));
                Containers.dropItemStack(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), scaffolding);
            }

            if ((Boolean)state.getValue(BotaniaStateProperties.HAS_SCAFFOLDING)) {
                scaffolding = new ItemStack(Items.SCAFFOLDING);
                Containers.dropItemStack(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), scaffolding);
            }

            Containers.dropContents(world, pos, spreader.getItemHandler());
            super.onRemove(state, world, pos, newState, isMoving);
        }

    }

    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new InfinityTileSpreader(blockPos, blockState);
    }

    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ECTileTypes.INFINITY_SPREADER.get(), InfinityTileSpreader::commonTick);
    }

}
