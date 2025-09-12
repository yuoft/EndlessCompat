package com.yuo.ec.Botania;

import com.yuo.ec.ECTileTypes;
import com.yuo.endless.Items.Tool.InfinityDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import vazkii.botania.api.block_entity.GeneratingFlowerBlockEntity;
import vazkii.botania.api.block_entity.RadiusDescriptor;
import vazkii.botania.api.block_entity.RadiusDescriptor.Rectangle;

import java.util.List;

public class SoarLeanderFlowerTile extends GeneratingFlowerBlockEntity {
    private static final int RANGE = 8;

    public SoarLeanderFlowerTile(BlockPos pos, BlockState state) {
        super(ECTileTypes.SOAR_LEANDER.get(), pos, state);
    }

    @Override
    public void tickFlower() {
        super.tickFlower();
        if (level == null || level.isClientSide) return;
        if (getMana() >= getMaxMana() || !isValidBinding()) return;
        //产能
        long gameTime = level.getGameTime();
        if (gameTime % 5 == 0){
            BlockPos pos = getBlockPos();
            List<LivingEntity> entitiesOfClass = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.offset(-7, -2, -7), pos.offset(7, 7, 7)), LivingEntity::isAlive);
            for (LivingEntity living : entitiesOfClass) {
                if (living instanceof Player) continue;
                boolean hurt = living.hurt(level.damageSources().fellOutOfWorld(), living.getMaxHealth());
                if (hurt && !living.isAlive()) {
                    addMana((int) Math.floor(living.getMaxHealth() * 100));
                    this.sync();
                }
            }
        }

    }

    @Override
    public int getColor() {
        return 0x3CA7FF;
    }

    //最大魔力
    @Override
    public int getMaxMana() {
        return 327670;
    }

    //作用范围
    @Override
    public RadiusDescriptor getRadius() {
        return Rectangle.square(getEffectivePos(), RANGE);
    }

    //是否是被动花
    @Override
    public boolean isOvergrowthAffected() {
        return false;
    }
}
