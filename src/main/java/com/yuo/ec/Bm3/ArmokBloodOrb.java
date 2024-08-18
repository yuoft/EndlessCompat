package com.yuo.ec.Bm3;

import com.yuo.endless.EndlessTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeItem;
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
        super((new Item.Properties()).maxStackSize(1).group(EndlessTab.endless));
        this.sup = sup;
    }

    @Nullable
    @Override
    public BloodOrb getOrb(ItemStack itemStack) {
        return this.sup.get();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        BloodOrb orb = this.getOrb(stack);
        if (orb == null) {
            return ActionResult.resultFail(stack);
        } else if (world == null) {
            return super.onItemRightClick(world, player, hand);
        } else {
            world.playSound((PlayerEntity)null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
            if (PlayerHelper.isFakePlayer(player)) {
                return super.onItemRightClick(world, player, hand);
            } else if (!stack.hasTag()) {
                return super.onItemRightClick(world, player, hand);
            } else {
                Binding binding = this.getBinding(stack);
                if (binding == null) {
                    return super.onItemRightClick(world, player, hand);
                } else if (world.isRemote) {
                    return super.onItemRightClick(world, player, hand);
                } else {
                    SoulNetwork ownerNetwork = NetworkHelper.getSoulNetwork(binding);
                    if (binding.getOwnerId().equals(player.getGameProfile().getId())) {
                        ownerNetwork.setOrbTier(orb.getTier());
                    }

                    ownerNetwork.add(SoulTicket.item(stack, world, player, 200), orb.getCapacity());
                    ownerNetwork.hurtPlayer(player, 200.0F);
                    return super.onItemRightClick(world, player, hand);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add((new TranslationTextComponent("tooltip.bloodmagic.orb.desc")).mergeStyle(TextFormatting.GRAY));
        BloodOrb orb = this.getOrb(stack);
        if (flag.isAdvanced() && orb != null) {
            tooltip.add((new TranslationTextComponent("tooltip.bloodmagic.orb.owner", stack.getItem().getRegistryName())).mergeStyle(TextFormatting.GRAY));
        }
        if (stack.hasTag()) {
            Binding binding = this.getBinding(stack);
            if (binding != null) {
                tooltip.add((new TranslationTextComponent("tooltip.bloodmagic.currentOwner", binding.getOwnerName())).mergeStyle(TextFormatting.GRAY));
            }
        }
    }

    public ItemStack getContainerItem(ItemStack stack) {
        return stack.copy();
    }

    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }
}
