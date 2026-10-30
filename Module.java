package dev.pug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public abstract class Module {
    public final String name, desc, cat;
    public boolean enabled;
    protected final Minecraft mc = Minecraft.getInstance();

    protected Module(String name, String desc, String cat) {
        this.name = name;
        this.desc = desc;
        this.cat = cat;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
        Config.save();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(Minecraft mc) {}
    public void onRender() {}
    public void onChunkLoad(ClientLevel level, LevelChunk chunk) {}
    public void onHud(GuiGraphics g) {}
    public void onRightClick() {}
    public void reset() {}
    public String info() { return ""; }
}
