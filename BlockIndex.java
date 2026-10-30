package dev.pug;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Per-chunk index of marker blocks (portals, beacons, beds, anchors, deep lights). */
public final class BlockIndex {
    public enum Kind { PORTAL, BEACON, ANCHOR, BED, MISC, LIGHT }

    public static final class Idx {
        public final EnumMap<Kind, List<BlockPos>> m = new EnumMap<>(Kind.class);
    }

    public static final Map<Long, Idx> INDEX = new ConcurrentHashMap<>();
    public static final int LIGHT_Y = 40;
    private static final int CAP = 24;
    private static final ArrayDeque<LevelChunk> QUEUE = new ArrayDeque<>();
    private static final Set<Block> LIGHTS = Set.of(
            Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
            Blocks.LANTERN, Blocks.SOUL_LANTERN);

    private BlockIndex() {}

    private static boolean candidate(BlockState s) {
        Block b = s.getBlock();
        return b == Blocks.NETHER_PORTAL || b == Blocks.BEACON || b == Blocks.RESPAWN_ANCHOR
                || b == Blocks.ENCHANTING_TABLE || b == Blocks.LODESTONE || b == Blocks.END_PORTAL_FRAME
                || s.is(BlockTags.BEDS) || LIGHTS.contains(b);
    }

    private static Kind kind(BlockState s, int y) {
        Block b = s.getBlock();
        if (b == Blocks.NETHER_PORTAL) return Kind.PORTAL;
        if (b == Blocks.BEACON) return Kind.BEACON;
        if (b == Blocks.RESPAWN_ANCHOR) return Kind.ANCHOR;
        if (s.is(BlockTags.BEDS)) return Kind.BED;
        if (b == Blocks.ENCHANTING_TABLE || b == Blocks.LODESTONE || b == Blocks.END_PORTAL_FRAME) return Kind.MISC;
        if (y < LIGHT_Y && LIGHTS.contains(b)) return Kind.LIGHT;
        return null;
    }

    public static void enqueue(LevelChunk c) {
        QUEUE.add(c);
    }

    public static void requeue() {
        QUEUE.clear();
        QUEUE.addAll(PugClient.CHUNKS.values());
    }

    public static void clear() {
        QUEUE.clear();
        INDEX.clear();
    }

    public static void tick() {
        for (int i = 0; i < 3 && !QUEUE.isEmpty(); i++) {
            LevelChunk c = QUEUE.poll();
            if (c != null) scan(c);
        }
    }

    private static void scan(LevelChunk c) {
        long key = K.key(c.getPos());
        if (PugClient.CHUNKS.get(key) != c) return;
        Idx idx = new Idx();
        LevelChunkSection[] secs = c.getSections();
        int bottom = c.getMinY();
        int bx = c.getPos().getMinBlockX(), bz = c.getPos().getMinBlockZ();
        for (int i = 0; i < secs.length; i++) {
            LevelChunkSection sec = secs[i];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(BlockIndex::candidate)) continue;
            for (int y = 0; y < 16; y++) {
                int wy = bottom + i * 16 + y;
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        Kind k = kind(sec.getBlockState(x, y, z), wy);
                        if (k == null) continue;
                        List<BlockPos> list = idx.m.computeIfAbsent(k, kk -> new ArrayList<>());
                        if (list.size() < CAP) list.add(new BlockPos(bx + x, wy, bz + z));
                    }
            }
        }
        INDEX.put(key, idx);
    }
}
