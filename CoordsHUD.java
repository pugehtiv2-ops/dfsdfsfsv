package dev.pug;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

public class CoordsHUD extends Module {
    public CoordsHUD() {
        super("Coords HUD", "XYZ plus Nether/Overworld conversion", "Visuals");
    }

    @Override
    public void onHud(GuiGraphics g) {
        if (mc.player == null) return;
        Vec3 p = mc.player.position();
        int x = (int) Math.floor(p.x), y = (int) Math.floor(p.y), z = (int) Math.floor(p.z);
        String dim = K.dim(mc);
        g.drawString(mc.font, "XYZ " + x + " " + y + " " + z + "  [" + dim + "]", 6, 84, 0xFFFFFFFF, true);
        if (dim.equals("overworld"))
            g.drawString(mc.font, "Nether " + Math.floorDiv(x, 8) + " " + Math.floorDiv(z, 8), 6, 94, 0xFFFF8855, true);
        else if (dim.equals("nether"))
            g.drawString(mc.font, "Overworld " + (x * 8) + " " + (z * 8), 6, 94, 0xFF55FF55, true);
    }
}
