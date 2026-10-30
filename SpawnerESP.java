package dev.pug;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SpawnerESP extends Module {
    public SpawnerESP() {
        super("Spawner ESP", "Mob spawners through walls", "Base Finding");
    }

    @Override
    public void onRender() {
        double max = 384 * 384;
        Vec3 cam = Draw.camera();
        for (LevelChunk c : PugClient.CHUNKS.values()) {
            for (BlockEntity be : c.getBlockEntities().values()) {
                if (!(be instanceof SpawnerBlockEntity)) continue;
                if (be.getBlockPos().getCenter().distanceToSqr(cam) > max) continue;
                Draw.box(new AABB(be.getBlockPos()), 0xFF00FF66);
            }
        }
    }
}
