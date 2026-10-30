package dev.pug;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class EntityESP extends Module {
    private static final Map<EntityType<?>, Integer> COLORS = Map.of(
            EntityType.ITEM_FRAME, 0xFFFFAA00,
            EntityType.GLOW_ITEM_FRAME, 0xFF55FFAA,
            EntityType.ARMOR_STAND, 0xFFAAAAAA,
            EntityType.CHEST_MINECART, 0xFFFF7700,
            EntityType.HOPPER_MINECART, 0xFFB0B0B0,
            EntityType.VILLAGER, 0xFF55FF55);

    public EntityESP() {
        super("Entity ESP", "Item frames, armor stands, chest carts, villagers", "Base Finding");
    }

    @Override
    public void onRender() {
        if (mc.level == null) return;
        double max = 128 * 128;
        Vec3 cam = Draw.camera();
        for (Entity e : mc.level.entitiesForRendering()) {
            Integer col = COLORS.get(e.getType());
            if (col == null) continue;
            if (e.distanceToSqr(cam) > max) continue;
            Draw.box(e.getBoundingBox(), col);
        }
    }
}
