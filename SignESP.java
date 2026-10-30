package dev.pug;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SignESP extends Module {
    public SignESP() {
        super("Sign ESP", "Signs and hanging signs through walls", "Base Finding");
    }

    @Override
    public void onRender() {
        double max = 192 * 192;
        Vec3 cam = Draw.camera();
        for (LevelChunk c : PugClient.CHUNKS.values()) {
            for (BlockEntity be : c.getBlockEntities().values()) {
                if (!(be instanceof SignBlockEntity)) continue;
                if (be.getBlockPos().getCenter().distanceToSqr(cam) > max) continue;
                Draw.box(new AABB(be.getBlockPos()), 0xFFFFFF55);
            }
        }
    }
}
