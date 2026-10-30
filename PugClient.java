package dev.pug;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.LevelChunk;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PugClient implements ClientModInitializer {
    public static final String ID = "pugclient";
    public static final Logger LOG = LoggerFactory.getLogger(ID);
    public static final Map<Long, LevelChunk> CHUNKS = new ConcurrentHashMap<>();
    public static final List<Module> MODULES = new ArrayList<>();
    public static MarkerESP MARKERS;
    public static DeepLights LIGHTS;
    public static VillageFilter VILLAGE;

    private static KeyMapping openKey;
    private static boolean wasDown;
    private static Object lastLevel;

    private static void resetAll() {
        BlockIndex.clear();
        for (Module m : MODULES) {
            m.reset();
            if (m.enabled) m.onEnable();
        }
    }

    @Override
    public void onInitializeClient() {
        Draw.init();

        MARKERS = new MarkerESP();
        LIGHTS = new DeepLights();
        VILLAGE = new VillageFilter();

        MODULES.add(new BaseFinder());
        MODULES.add(VILLAGE);
        MODULES.add(new StorageESP());
        MODULES.add(new SpawnerESP());
        MODULES.add(MARKERS);
        MODULES.add(new SignESP());
        MODULES.add(new EntityESP());
        MODULES.add(LIGHTS);
        MODULES.add(new Tracers());
        MODULES.add(new Fullbright());
        MODULES.add(new CoordsHUD());

        Config.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pugclient.gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            CHUNKS.put(K.key(chunk.getPos()), chunk);
            for (Module m : MODULES) if (m.enabled) m.onChunkLoad(level, chunk);
            if (MARKERS.enabled || LIGHTS.enabled) BlockIndex.enqueue(chunk);
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            long k = K.key(chunk.getPos());
            CHUNKS.remove(k);
            BlockIndex.INDEX.remove(k);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            if (client.player != null)
                client.player.displayClientMessage(
                        Component.literal("\u00a7d[Pug] \u00a7fPugClient 1.1.0 loaded. Press RIGHT SHIFT for the menu."), false);
        }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CHUNKS.clear();
            lastLevel = null;
            resetAll();
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            boolean down = false;
            long window = GLFW.glfwGetCurrentContext();
            if (window != 0L)
                down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            boolean open = false;
            while (openKey.consumeClick()) open = true;
            if (down && !wasDown) open = true;
            wasDown = down;
            if (open && mc.screen == null && mc.player != null) mc.setScreen(new ClickGui());

            if (mc.player == null || mc.level == null) return;

            if (mc.level != lastLevel) {
                lastLevel = mc.level;
                CHUNKS.values().removeIf(c -> c.getLevel() != mc.level);
                resetAll();
            }

            for (Module m : MODULES) if (m.enabled) m.onTick(mc);
            if (MARKERS.enabled || LIGHTS.enabled) BlockIndex.tick();
        });

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> {
            if (!Draw.begin(ctx)) return;
            for (Module m : MODULES) if (m.enabled) m.onRender();
            Draw.end();
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ID, "hud"),
                (g, tick) -> {
                    for (Module m : MODULES) if (m.enabled) m.onHud(g);
                });

        LOG.info("PugClient 1.1.0 initialised with {} modules", MODULES.size());
    }
}
