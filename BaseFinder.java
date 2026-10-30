package dev.pug;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BaseFinder extends Module {
    public record Base(ChunkPos cp, BlockPos pos, int score, int storage, int art) {}
    public record Cluster(BlockPos center, int score, int chunks, int storage) {}

    public static final Map<Long, Base> BASES = new ConcurrentHashMap<>();
    private static final Set<Long> BELLS = ConcurrentHashMap.newKeySet();
    private static final Set<Block> ART = Set.of(
            Blocks.GLASS, Blocks.GLASS_PANE, Blocks.TORCH, Blocks.WALL_TORCH, Blocks.LANTERN,
            Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
            Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.REDSTONE_WIRE, Blocks.REPEATER,
            Blocks.COMPARATOR, Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON,
            Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER, Blocks.NOTE_BLOCK,
            Blocks.END_ROD, Blocks.SEA_LANTERN, Blocks.ENCHANTING_TABLE, Blocks.ANVIL,
            Blocks.BREWING_STAND, Blocks.SMITHING_TABLE, Blocks.COBBLESTONE,
            Blocks.BEACON, Blocks.NETHER_PORTAL, Blocks.RESPAWN_ANCHOR, Blocks.LODESTONE,
            Blocks.SOUL_TORCH, Blocks.SOUL_LANTERN, Blocks.SCAFFOLDING, Blocks.LADDER);
    private static final String[] ARROWS = {
            "\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};

    private static int threshold = 12;
    private static int ver, cachedVer = -1;
    private static List<Cluster> cache = List.of();

    private final ArrayDeque<LevelChunk> queue = new ArrayDeque<>();

    public BaseFinder() {
        super("Base Finder", "Scores chunks for player-built bases", "Base Finding");
    }

    public static int getThreshold() { return threshold; }
    public static void setThreshold(int t) { threshold = Math.max(3, Math.min(60, t)); }

    private static boolean artificial(BlockState s) {
        return ART.contains(s.getBlock())
                || s.is(BlockTags.PLANKS) || s.is(BlockTags.BEDS)
                || s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS)
                || s.is(BlockTags.WOOL);
    }

    private static int tier(int score) {
        return score >= 40 ? 0xFFFF3B30 : score >= 24 ? 0xFFFFA500 : 0xFFD21CFF;
    }

    private static void removeBase(long key) {
        if (BASES.remove(key) != null) ver++;
    }

    private static boolean nearBell(int cx, int cz, int r) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                if (BELLS.contains(K.key(cx + dx, cz + dz))) return true;
        return false;
    }

    private static void clearAround(int cx, int cz, int r) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) removeBase(K.key(cx + dx, cz + dz));
    }

    public static List<Cluster> clusters() {
        if (cachedVer == ver) return cache;
        Map<Long, Base> snap = new HashMap<>(BASES);
        Set<Long> seen = new HashSet<>();
        List<Cluster> out = new ArrayList<>();
        for (Map.Entry<Long, Base> e : snap.entrySet()) {
            if (!seen.add(e.getKey())) continue;
            ArrayDeque<Base> stack = new ArrayDeque<>();
            stack.add(e.getValue());
            int score = 0, n = 0, st = 0;
            double sx = 0, sy = 0, sz = 0;
            while (!stack.isEmpty()) {
                Base b = stack.poll();
                score += b.score();
                n++;
                st += b.storage();
                sx += b.pos().getX() * (double) b.score();
                sy += b.pos().getY() * (double) b.score();
                sz += b.pos().getZ() * (double) b.score();
                int cx = b.cp().getMinBlockX() >> 4, cz = b.cp().getMinBlockZ() >> 4;
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        long k = K.key(cx + dx, cz + dz);
                        Base nb = snap.get(k);
                        if (nb != null && seen.add(k)) stack.add(nb);
                    }
            }
            BlockPos c = new BlockPos((int) (sx / score), (int) (sy / score), (int) (sz / score));
            out.add(new Cluster(c, score, n, st));
        }
        cache = out;
        cachedVer = ver;
        return out;
    }

    @Override
    public void onEnable() {
        queue.addAll(PugClient.CHUNKS.values());
    }

    @Override
    public void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        queue.add(chunk);
    }

    @Override
    public void reset() {
        queue.clear();
        BASES.clear();
        BELLS.clear();
        ver++;
    }

    @Override
    public void onRightClick() {
        threshold = threshold >= 30 ? 6 : threshold + 3;
        queue.clear();
        BASES.clear();
        ver++;
        queue.addAll(PugClient.CHUNKS.values());
        Config.save();
    }

    @Override
    public String info() {
        return clusters().size() + " found / min " + threshold;
    }

    @Override
    public void onTick(Minecraft mc) {
        for (int i = 0; i < 3 && !queue.isEmpty(); i++) scan(queue.poll());
    }

    private void scan(LevelChunk c) {
        ChunkPos cp = c.getPos();
        int cx = cp.getMinBlockX() >> 4, cz = cp.getMinBlockZ() >> 4;
        long key = K.key(cx, cz);

        int storage = 0;
        long sx = 0, sy = 0, sz = 0;
        boolean bell = false;
        for (BlockEntity be : c.getBlockEntities().values()) {
            if (be instanceof BellBlockEntity) bell = true;
            if (StorageESP.colorOf(be) == 0) continue;
            storage++;
            sx += be.getBlockPos().getX();
            sy += be.getBlockPos().getY();
            sz += be.getBlockPos().getZ();
        }
        if (bell) {
            BELLS.add(key);
            clearAround(cx, cz, 4);
        }
        if (PugClient.VILLAGE != null && PugClient.VILLAGE.enabled && nearBell(cx, cz, 4)) {
            removeBase(key);
            return;
        }

        int art = 0;
        long ay = 0;
        LevelChunkSection[] secs = c.getSections();
        int bottom = c.getMinY();
        for (int i = 0; i < secs.length; i++) {
            LevelChunkSection sec = secs[i];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(BaseFinder::artificial)) continue;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++)
                        if (artificial(sec.getBlockState(x, y, z))) {
                            art++;
                            ay += bottom + i * 16 + y;
                        }
        }

        int score = storage * 3 + art / 6;
        if (score < threshold) {
            removeBase(key);
            return;
        }

        BlockPos pos = storage > 0
                ? new BlockPos((int) (sx / storage), (int) (sy / storage), (int) (sz / storage))
                : new BlockPos(cp.getMiddleBlockX(), (int) (ay / Math.max(art, 1)), cp.getMiddleBlockZ());

        boolean fresh = true;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (BASES.containsKey(K.key(cx + dx, cz + dz))) fresh = false;

        BASES.put(key, new Base(cp, pos, score, storage, art));
        ver++;
        if (fresh) announce(pos, score, storage);
    }

    private void announce(BlockPos p, int score, int storage) {
        String line = "BASE [" + K.dim(mc) + "] x=" + p.getX() + " y=" + p.getY() + " z=" + p.getZ()
                + " score=" + score + " storage=" + storage;
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal("\u00a7d[Pug] \u00a7f" + line), false);
        try {
            Path f = FabricLoader.getInstance().getGameDir().resolve("pugclient_bases.txt");
            Files.writeString(f, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void onRender() {
        for (Base b : BASES.values()) {
            double y = b.pos().getY();
            Draw.box(new AABB(b.cp().getMinBlockX(), y - 8, b.cp().getMinBlockZ(),
                    b.cp().getMaxBlockX() + 1, y + 8, b.cp().getMaxBlockZ() + 1), tier(b.score()));
        }
        for (Cluster c : clusters()) {
            double x = c.center().getX() + 0.5, z = c.center().getZ() + 0.5, y = c.center().getY();
            Draw.line(new Vec3(x, y, z), new Vec3(x, 320, z), (0x99 << 24) | (tier(c.score()) & 0xFFFFFF));
        }
    }

    private String arrow(Vec3 me, BlockPos t) {
        if (mc.player == null) return "";
        double dx = t.getX() + 0.5 - me.x, dz = t.getZ() + 0.5 - me.z;
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = Mth.wrapDegrees(target - mc.player.getYRot());
        int k = Math.floorMod((int) Math.round(rel / 45.0), 8);
        return ARROWS[k];
    }

    @Override
    public void onHud(GuiGraphics g) {
        if (mc.player == null) return;
        List<Cluster> list = new ArrayList<>(clusters());
        Vec3 me = mc.player.position();
        list.sort(Comparator.comparingDouble(c -> c.center().getCenter().distanceToSqr(me)));
        g.drawString(mc.font, "\u00a7dPugClient \u00a77bases: " + list.size()
                + " (" + BASES.size() + " chunks)", 6, 6, 0xFFFFFFFF, true);
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            Cluster c = list.get(i);
            int d = (int) Math.sqrt(c.center().getCenter().distanceToSqr(me));
            String t = arrow(me, c.center()) + " " + c.center().getX() + " " + c.center().getY() + " "
                    + c.center().getZ() + "  " + d + "m  s" + c.score() + " c" + c.chunks();
            g.drawString(mc.font, t, 6, 18 + i * 10, tier(c.score()), true);
        }
    }
}
