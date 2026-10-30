package dev.pug;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pugclient.properties");
    }

    private static String k(Module m) {
        return "module." + m.name.replace(' ', '_');
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            for (Module m : PugClient.MODULES)
                if (m instanceof BaseFinder || m instanceof CoordsHUD || m instanceof VillageFilter) {
                    m.enabled = true;
                    m.onEnable();
                }
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        for (Module m : PugClient.MODULES)
            if (Boolean.parseBoolean(p.getProperty(k(m), "false"))) {
                m.enabled = true;
                m.onEnable();
            }
        try {
            BaseFinder.setThreshold(Integer.parseInt(p.getProperty("threshold", "12")));
        } catch (NumberFormatException ignored) {
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : PugClient.MODULES) p.setProperty(k(m), String.valueOf(m.enabled));
        p.setProperty("threshold", String.valueOf(BaseFinder.getThreshold()));
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "PugClient");
        } catch (IOException ignored) {
        }
    }
}
