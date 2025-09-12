package com.yuo.ec.Botania;

import com.yuo.ec.ECTileTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.block_entity.GeneratingFlowerBlockEntity;
import vazkii.botania.api.block_entity.RadiusDescriptor;
import vazkii.botania.api.block_entity.RadiusDescriptor.Rectangle;

public class AsgardFlowerTile extends GeneratingFlowerBlockEntity {
    private static final int RANGE = 8;

    public AsgardFlowerTile(BlockPos pos, BlockState state) {
        super(ECTileTypes.ASGARD_FLOWER.get(), pos, state);
    }

    @Override
    public void tickFlower() {
        super.tickFlower();
        if (level == null || level.isClientSide || !isValidBinding() || getMana() >= getMaxMana()) return;

        double particleChance = 1F - (double) getMana() / (double) getMaxMana() / 3.5F;
        int color = getColor();
        float red = (color >> 16 & 0xFF) / 255F;
        float green = (color >> 8 & 0xFF) / 255F;
        float blue = (color & 0xFF) / 255F;

        if (Math.random() > particleChance) {
            Vec3 offset = level.getBlockState(getBlockPos()).getOffset(level, getBlockPos());
            double x = getBlockPos().getX() + offset.x;
            double y = getBlockPos().getY() + offset.y;
            double z = getBlockPos().getZ() + offset.z;
            BotaniaAPI.instance().sparkleFX(getLevel(), x + 0.3 + Math.random() * 0.5, y + 0.5 + Math.random() * 0.5, z + 0.3 + Math.random() * 0.5, red, green, blue, (float) Math.random(), 5);
        }

        //产能
        long gameTime = level.getGameTime();
        if (gameTime % 2 == 0){
            addMana(getGenMana());
            this.sync();
        }

        //阻止周围产能花枯萎
//        for(int dx = -RANGE; dx <= RANGE; dx++)
//            for(int dz = -RANGE; dz <= RANGE; dz++){
//                BlockPos pos = getEffectivePos().offset(dx, 0, dz);
//                BlockEntity tile = level.getBlockEntity(pos);
//                if(tile instanceof GeneratingFlowerBlockEntity flower){
//                    if(flower.isRemoved()){
//                        flower.passiveDecayTicks = 0;
//                    }
//                }
//            }
    }

    public int getGenMana() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getColor() {
        return 0x5EF2FF;
    }

    //最大魔力
    @Override
    public int getMaxMana() {
        return Integer.MAX_VALUE;
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
