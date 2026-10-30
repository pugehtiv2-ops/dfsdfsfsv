package dev.pug;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MarkerESP extends Module {
    public MarkerESP() {
        super("Marker ESP", "Portals, beacons, beds, anchors, enchant tables", "Base Finding");
    }

    @Override
    public void onEnable() {
        BlockIndex.requeue();
    }

    private static int color(BlockIndex.Kind k) {
        return switch (k) {
            case PORTAL -> 0xFFB84DFF;
            case BEACON -> 0xFF55FFFF;
            case ANCHOR -> 0xFFFF55AA;
            case BED -> 0xFFFF5555;
            default -> 0xFFFFFF55;
        };
    }

    @Override
    public void onRender() {
        double max = 256 * 256;
        Vec3 cam = Draw.camera();
        for (BlockIndex.Idx idx : BlockIndex.INDEX.values()) {
            for (BlockIndex.Kind k : BlockIndex.Kind.values()) {
                if (k == BlockIndex.Kind.LIGHT) continue;
                List<BlockPos> list = idx.m.get(k);
                if (list == null) continue;
                int col = color(k);
                for (BlockPos p : list) {
                    if (p.getCenter().distanceToSqr(cam) > max) continue;
                    Draw.box(new AABB(p), col);
                }
            }
        }
    }
}
