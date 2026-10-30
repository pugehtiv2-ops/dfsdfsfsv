package dev.pug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Standard vanilla/Fabric screen: Button widgets laid out in init(), super.render() draws background + widgets. */
public class ClickGui extends Screen {
    private static final String[] CATS = {"Base Finding", "Visuals"};
    private static int cat, page;

    private int pages = 1, navX, footerY;

    protected ClickGui() {
        super(Component.literal("PugClient"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<Module> list() {
        List<Module> l = new ArrayList<>();
        for (Module m : PugClient.MODULES) if (m.cat.equals(CATS[cat])) l.add(m);
        return l;
    }

    private static int count(String c) {
        int n = 0;
        for (Module m : PugClient.MODULES) if (m.cat.equals(c)) n++;
        return n;
    }

    private static Component label(Module m) {
        return Component.literal((m.enabled ? "\u00a7d" : "\u00a77") + m.name
                + "\u00a7f: " + (m.enabled ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private static Component thresholdLabel() {
        return Component.literal("Score threshold: " + BaseFinder.getThreshold());
    }

    @Override
    protected void init() {
        final int gap = 6;
        int cols = width >= 360 ? 2 : 1;
        int bw = Math.min(200, (width - 24 - gap * (cols - 1)) / cols);
        int gridW = bw * cols + gap * (cols - 1);
        int left = (width - gridW) / 2;

        // category tabs
        int tabW = Math.min(110, (width - 24 - gap) / CATS.length);
        int tabsW = tabW * CATS.length + gap * (CATS.length - 1);
        int tabX = (width - tabsW) / 2;
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            String t = (i == cat ? "\u00a7d" : "\u00a77") + CATS[i] + " (" + count(CATS[i]) + ")";
            addRenderableWidget(Button.builder(Component.literal(t), btn -> {
                cat = idx;
                page = 0;
                rebuildWidgets();
            }).bounds(tabX + i * (tabW + gap), 24, tabW, 20).build());
        }

        // module toggles
        List<Module> mods = list();
        int top = 52;
        int bottom = height - 30;
        int rows = Math.max(1, (bottom - top + 4) / 24);
        int perPage = rows * cols;
        pages = Math.max(1, (mods.size() + perPage - 1) / perPage);
        if (page >= pages) page = 0;
        int from = page * perPage, to = Math.min(mods.size(), from + perPage);
        for (int j = from; j < to; j++) {
            Module m = mods.get(j);
            int k = j - from;
            int x = left + (k % cols) * (bw + gap);
            int y = top + (k / cols) * 24;
            Button b = Button.builder(label(m), btn -> {
                m.toggle();
                btn.setMessage(label(m));
            }).bounds(x, y, bw, 20).build();
            b.setTooltip(Tooltip.create(Component.literal(m.desc)));
            addRenderableWidget(b);
        }

        // footer: threshold, page nav, done
        footerY = height - 26;
        if (cat == 0) {
            BaseFinder bf = null;
            for (Module m : PugClient.MODULES) if (m instanceof BaseFinder b0) bf = b0;
            if (bf != null) {
                final BaseFinder finder = bf;
                Button t = Button.builder(thresholdLabel(), btn -> {
                    finder.onRightClick();
                    btn.setMessage(thresholdLabel());
                }).bounds(left, footerY, 120, 20).build();
                t.setTooltip(Tooltip.create(Component.literal("Higher = fewer, bigger bases. Click to cycle.")));
                addRenderableWidget(t);
            }
        }
        if (pages > 1) {
            navX = left + gridW / 2 - 35;
            addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(navX, footerY, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(navX + 50, footerY, 20, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Done"),
                btn -> Minecraft.getInstance().setScreen(null)).bounds(left + gridW - 60, footerY, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        String title = "\u00a7dPUG\u00a7fCLIENT \u00a77v1.1.0";
        g.drawString(this.font, title, (width - this.font.width(title)) / 2, 10, 0xFFFFFFFF, true);
        if (pages > 1) {
            String p = (page + 1) + "/" + pages;
            g.drawString(this.font, p, navX + 20 + (30 - this.font.width(p)) / 2, footerY + 6, 0xFFB8A8C8, false);
        }
    }
}
