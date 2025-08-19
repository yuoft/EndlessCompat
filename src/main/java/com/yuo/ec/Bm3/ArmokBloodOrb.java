package com.yuo.ec.Bm3;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeItem;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.common.item.BloodOrb;
import wayoftime.bloodmagic.common.item.IBindable;
import wayoftime.bloodmagic.common.item.IBloodOrb;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.core.data.SoulNetwork;
import wayoftime.bloodmagic.core.data.SoulTicket;
import wayoftime.bloodmagic.util.helper.NetworkHelper;
import wayoftime.bloodmagic.util.helper.PlayerHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class ArmokBloodOrb extends Item implements IBindable, IBloodOrb, IForgeItem {
    private final Supplier<BloodOrb> sup;

    public ArmokBloodOrb(Supplier<BloodOrb> sup) {
        super((new Item.Properties()).stacksTo(1));
        this.sup = sup;
    }

    @Nullable
    @Override
    public BloodOrb getOrb(ItemStack itemStack) {
        return this.sup.get();
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BloodOrb orb = this.getOrb(stack);
        if (orb == null) {
            return InteractionResultHolder.fail(stack);
        } else {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
            if (PlayerHelper.isFakePlayer(player)) {
                return super.use(world, player, hand);
            } else if (!stack.hasTag()) {
                return super.use(world, player, hand);
            } else {
                Binding binding = this.getBinding(stack);
                if (binding == null) {
                    return super.use(world, player, hand);
                } else if (world.isClientSide) {
                    return super.use(world, player, hand);
                } else {
                    SoulNetwork ownerNetwork = NetworkHelper.getSoulNetwork(binding);
                    if (binding.getOwnerId().equals(player.getGameProfile().getId())) {
                        ownerNetwork.setOrbTier(orb.getTier());
                    }

                    ownerNetwork.add(SoulTicket.item(stack, world, player, 200), orb.getCapacity());
                    ownerNetwork.hurtPlayer(player, 200.0F);
                    return super.use(world, player, hand);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bloodmagic.orb.desc").withStyle(ChatFormatting.GRAY));
        BloodOrb orb = this.getOrb(stack);
        if (flag.isAdvanced() && orb != null) {
            tooltip.add(Component.translatable("tooltip.bloodmagic.orb.owner", new Object[]{ForgeRegistries.ITEMS.getKey(stack.getItem())}).withStyle(ChatFormatting.GRAY));
        }

        super.appendHoverText(stack, world, tooltip, flag);
    }

    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }

    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }
}
