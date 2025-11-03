package com.yuo.ec;

import net.minecraft.resources.ResourceLocation;

public class RlUtil {
    public static ResourceLocation fa(String path){
        return new ResourceLocation(EndlessCompat.MOD_ID, path);
    }

    public static ResourceLocation fa(String namespace, String path){
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String path){
        return ResourceLocation.parse(path);
    }
}
