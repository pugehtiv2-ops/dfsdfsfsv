package dev.pug;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DeepLights extends Module {
    public DeepLights() {
        super("Deep Lights", "Torches and lanterns below y=" + BlockIndex.LIGHT_Y, "Base Finding");
    }

    @Override
    public void onEnable() {
        BlockIndex.requeue();
    }

    @Override
    public void onRender() {
        double max = 192 * 192;
        Vec3 cam = Draw.camera();
        for (BlockIndex.Idx idx : BlockIndex.INDEX.values()) {
            List<BlockPos> list = idx.m.get(BlockIndex.Kind.LIGHT);
            if (list == null) continue;
            for (BlockPos p : list) {
                if (p.getCenter().distanceToSqr(cam) > max) continue;
                Draw.box(new AABB(p), 0xFFFFD34D);
            }
        }
    }
}
