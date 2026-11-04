package com.vladdisasters.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** Сохраняет, построен ли бункер и где он. */
public class BunkerData extends SavedData {
    private static final String NAME = "vladdisasters_bunker";
    public boolean generated;
    @Nullable
    public BlockPos pos;

    public static BunkerData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(BunkerData::new, BunkerData::load, null), NAME);
    }

    public static BunkerData load(CompoundTag tag, HolderLookup.Provider provider) {
        BunkerData d = new BunkerData();
        d.generated = tag.getBoolean("Generated");
        if (tag.contains("X")) d.pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("Generated", generated);
        if (pos != null) {
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
        }
        return tag;
    }
}
