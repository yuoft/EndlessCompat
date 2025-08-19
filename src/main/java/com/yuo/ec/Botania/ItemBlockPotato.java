package com.yuo.ec.Botania;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.common.handler.ContributorList;
import vazkii.botania.common.helper.ItemNBTHelper;
import vazkii.botania.common.item.ItemWithBannerPattern;
import vazkii.botania.common.item.block.TinyPotatoBlockItem;
import vazkii.botania.common.lib.BotaniaTags.BannerPatterns;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ItemBlockPotato extends BlockItem implements ItemWithBannerPattern {
    private static final Pattern TYPOS = Pattern.compile("(?!^vazkii$)^v[ao]{1,2}[sz]{0,2}[ak]{1,2}(i){1,2}l{0,2}$", 2);
    private static final List<String> ENCHANTMENT_NAMES = List.of("enchanted", "glowy", "shiny", "gay");
    private static final int NOT_MY_NAME = 17;
    private static final String TAG_TICKS = "notMyNameTicks";

    public ItemBlockPotato(Block block, Item.Properties props) {
        super(block, props);
    }

    public TagKey<BannerPattern> getBannerPattern() {
        return BannerPatterns.PATTERN_ITEM_TINY_POTATO;
    }

    public void inventoryTick(ItemStack stack, Level world, Entity e, int t, boolean idunno) {
        if (!world.isClientSide && e instanceof Player player) {
            if (e.tickCount % 30 == 0 && TYPOS.matcher(stack.getHoverName().getString()).matches()) {
                int ticks = ItemNBTHelper.getInt(stack, "notMyNameTicks", 0);
                if (ticks < 17) {
                    player.sendSystemMessage(Component.translatable("botania.tater.you_came_to_the_wrong_neighborhood." + ticks).withStyle(ChatFormatting.RED));
                    ItemNBTHelper.setInt(stack, "notMyNameTicks", ticks + 1);
                }
            }
        }

    }

    public static boolean isEnchantedName(@NotNull Component name, @Nullable StringBuilder nameBuilder) {
        String trimmed = name.getString().trim();
        String nameString = trimmed.toLowerCase(Locale.ROOT);
        Iterator var4 = ENCHANTMENT_NAMES.iterator();

        String prefix;
        do {
            if (!var4.hasNext()) {
                if (nameBuilder != null) {
                    nameBuilder.append(trimmed);
                }

                return false;
            }

            prefix = (String)var4.next();
        } while(!nameString.equals(prefix) && !nameString.startsWith(prefix + " "));

        if (nameBuilder != null) {
            if (trimmed.length() > prefix.length()) {
                nameBuilder.append(trimmed, prefix.length() + 1, trimmed.length());
            } else {
                nameBuilder.append(trimmed);
            }
        }

        return true;
    }

    public static String removeFromFront(String name, String match) {
        return name.substring(match.length()).trim();
    }

    public boolean isFoil(@NotNull ItemStack stack) {
        return super.isFoil(stack) || isEnchantedName(stack.getHoverName(), (StringBuilder)null);
    }

    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        boolean b;
        if (armorType == this.getEquipmentSlot(stack) && entity instanceof Player player) {
            if (ContributorList.hasFlower(player.getGameProfile().getName().toLowerCase(Locale.ROOT))) {
                b = true;
                return b;
            }
        }

        b = false;
        return b;
    }
}
