package dev.privatesign;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** config/private_sign.json (UTF-8). Spaces at the start/end of a line are kept as typed. */
public final class SignConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static SignConfig instance;

    /** The 4 sign lines. Leading/trailing spaces are indents and are preserved. */
    public List<String> lines = new ArrayList<>(List.of("Частная ", "территория", " Сунгирь", ""));
    /** Also write the same text on the back side. */
    public boolean backSide = false;
    /** Do not open the text editor after placing (the preset text is final unless the sign is edited later). */
    public boolean skipEditor = true;
    /** Wax the sign so nobody can change the text afterwards (honeycomb protection). */
    public boolean wax = false;

    public static synchronized SignConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("private_sign.json");
    }

    private static SignConfig load() {
        SignConfig c = null;
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                c = GSON.fromJson(r, SignConfig.class);
            } catch (Exception e) {
                PrivateSign.LOG.warn("Could not read config, using defaults", e);
            }
        }
        if (c == null) c = new SignConfig();
        while (c.lines.size() < 4) c.lines.add("");
        c.save();
        return c;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            PrivateSign.LOG.warn("Could not write config", e);
        }
    }
}
