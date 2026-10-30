package dev.pug;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StorageESP extends Module {
    public StorageESP() {
        super("Storage ESP", "Chests, barrels, shulkers, hoppers through walls", "Base Finding");
    }

    public static int colorOf(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return 0xFFFF3B30;
        if (be instanceof ChestBlockEntity) return 0xFFFFA500;
        if (be instanceof BarrelBlockEntity) return 0xFFB5651D;
        if (be instanceof ShulkerBoxBlockEntity) return 0xFFFF55FF;
        if (be instanceof EnderChestBlockEntity) return 0xFF8A2BE2;
        if (be instanceof HopperBlockEntity) return 0xFFB0B0B0;
        if (be instanceof DispenserBlockEntity) return 0xFFDDDDDD;
        if (be instanceof AbstractFurnaceBlockEntity) return 0xFF7FD3FF;
        return 0;
    }

    @Override
    public void onRender() {
        double max = 256 * 256;
        Vec3 cam = Draw.camera();
        for (LevelChunk c : PugClient.CHUNKS.values()) {
            for (BlockEntity be : c.getBlockEntities().values()) {
                int col = colorOf(be);
                if (col == 0) continue;
                if (be.getBlockPos().getCenter().distanceToSqr(cam) > max) continue;
                Draw.box(new AABB(be.getBlockPos()), col);
            }
        }
    }
}
