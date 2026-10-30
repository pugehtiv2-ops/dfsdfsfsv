package dev.pug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class K {
    private K() {}

    public static long key(ChunkPos cp) {
        return ChunkPos.asLong(cp.getMinBlockX() >> 4, cp.getMinBlockZ() >> 4);
    }

    public static long key(int cx, int cz) {
        return ChunkPos.asLong(cx, cz);
    }

    public static String dim(Minecraft mc) {
        if (mc.level == null) return "?";
        var d = mc.level.dimension();
        if (Level.NETHER.equals(d)) return "nether";
        if (Level.END.equals(d)) return "end";
        if (Level.OVERWORLD.equals(d)) return "overworld";
        return "other";
    }
}
