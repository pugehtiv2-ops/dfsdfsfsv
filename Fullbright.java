package dev.pug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "Client-side night vision for caves and tunnels", "Visuals");
    }

    @Override
    public void onTick(Minecraft mc) {
        mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 4000, 0, false, false, false));
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }
}
