package dev.pug;

import net.minecraft.world.phys.Vec3;

public class Tracers extends Module {
    public Tracers() {
        super("Base Tracers", "Lines from crosshair to every found base", "Base Finding");
    }

    @Override
    public void onRender() {
        for (BaseFinder.Cluster c : BaseFinder.clusters())
            Draw.tracer(Vec3.atCenterOf(c.center()), 0xFFD21CFF);
    }
}
