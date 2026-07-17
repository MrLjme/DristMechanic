package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> RAIDABLE_CROPS = TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "raidable_crops")
        );
    }
}