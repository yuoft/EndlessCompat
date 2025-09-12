package com.yuo.ec.Botania;

import com.yuo.ec.ECTileTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.common.block.decor.BotaniaMushroomBlock;
import vazkii.botania.common.block.mana.ManaPoolBlock;
import vazkii.botania.common.item.material.MysticalPetalItem;

import java.util.List;
import java.util.Optional;

public class InfinityManaPool extends ManaPoolBlock {
    public InfinityManaPool(Variant v, Properties builder) {
        super(v, builder);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("endless_compat.info.infinity_pool" ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level world, @NotNull BlockPos pos, Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        ItemStack stack = player.getItemInHand(hand);
        Optional<DyeColor> itemColor = Optional.empty();
        Item var11 = stack.getItem();
        if (var11 instanceof MysticalPetalItem petalItem) {
            itemColor = Optional.of(petalItem.color);
        }

        Block var14 = Block.byItem(stack.getItem());
        if (var14 instanceof BotaniaMushroomBlock mushroomBlock) {
            itemColor = Optional.of(mushroomBlock.color);
        }

        if (itemColor.isPresent() && be instanceof InfinityManaPoolTile pool) {
            if (!itemColor.equals(pool.getColor())) {
                pool.setColor(itemColor);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                return InteractionResult.sidedSuccess(world.isClientSide());
            }
        }

        if (stack.is(Items.CLAY_BALL) && be instanceof InfinityManaPoolTile pool) {
            if (pool.getColor().isPresent()) {
                pool.setColor(Optional.empty());
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                return InteractionResult.sidedSuccess(world.isClientSide());
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity item) {
            InfinityManaPoolTile tile = (InfinityManaPoolTile)world.getBlockEntity(pos);
            tile.collideEntityItem(item);
        }
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        InfinityManaPoolTile pool = (InfinityManaPoolTile)world.getBlockEntity(pos);
        return InfinityManaPoolTile.calculateComparatorLevel(pool.getCurrentMana(), pool.getMaxMana());
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new InfinityManaPoolTile(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ECTileTypes.INFINITY_POOL.get(), level.isClientSide ? InfinityManaPoolTile::clientTick : InfinityManaPoolTile::serverTick);
    }
}
