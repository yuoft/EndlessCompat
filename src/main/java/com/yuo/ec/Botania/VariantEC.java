package com.yuo.ec.Botania;

public enum VariantEC {
    MANA(160, 1000, 2162464, 65280, 60, 4.0F, 1.0F),
    REDSTONE(160, 1000, 16719904, 16711680, 60, 4.0F, 1.0F),
    ELVEN(240, 1000, 16729540, 16711854, 80, 4.0F, 1.25F),
    GAIA(640, 6400, 2162464, 65280, 120, 20.0F, 2.0F),
    INFINITY(214748364, 2147483647, 2162464, 65280, 120, 20.0F, 2.0F);

    public final int burstMana;
    public final int manaCapacity;
    public final int color;
    public final int hudColor;
    public final int preLossTicks;
    public final float lossPerTick;
    public final float motionModifier;
    //脉冲魔力量  魔力缓存 颜色 hud颜色 魔力流失前时间  ？？ 脉冲速度
    VariantEC(int bm, int mc, int c, int hc, int plt, float lpt, float mm) {
        this.burstMana = bm;
        this.manaCapacity = mc;
        this.color = c;
        this.hudColor = hc;
        this.preLossTicks = plt;
        this.lossPerTick = lpt;
        this.motionModifier = mm;
    }
}