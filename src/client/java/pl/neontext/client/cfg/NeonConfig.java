package pl.neontext.client.cfg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.anim.Effect;
import pl.neontext.client.anim.Palette;
import pl.neontext.client.holo.Hologram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The whole mod state, persisted as hand-rolled JSON in {@code config/neontext.json}.
 *
 * <p>Serialisation is explicit rather than reflective on purpose: a config file that somebody
 * edited by hand and got slightly wrong must degrade to defaults instead of failing to load, and
 * unknown keys must survive a round trip.
 */
public final class NeonConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // ---------------------------------------------------------------- global

    /** Master switch, also togglable with /neon toggle. */
    public boolean enabled = true;

    /** Also animate text inside screens (tooltips, buttons, inventories). Off keeps menus readable. */
    public boolean animateGui = false;

    /** Global speed multiplier on top of every per-target speed. */
    public float timeScale = 1.0f;

    /** Print a one-line hello into chat the first time the mod loads. */
    public boolean announce = true;

    /** Set once the config has been written at least once, so we do not nag on every launch. */
    public boolean firstRunDone = false;

    // --------------------------------------------------------------- targets

    public final Map<AnimTarget, TargetConfig> targets = new EnumMap<>(AnimTarget.class);

    /** What one animation target holds: an on/off switch plus the style itself. */
    public static final class TargetConfig {
        public boolean enabled = true;
        public AnimStyle style = new AnimStyle();

        public TargetConfig() {
        }

        public TargetConfig(boolean enabled, AnimStyle style) {
            this.enabled = enabled;
            this.style = style;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    // ------------------------------------------------------------ holograms

    public final List<Hologram> holograms = new ArrayList<>();

    private Path file;

    public NeonConfig() {
        targets.put(AnimTarget.NAMEPLATE, new TargetConfig(true, preset("Rainbow Flex")));
        targets.put(AnimTarget.TAB, new TargetConfig(true, preset("Neon Sign")));
        targets.put(AnimTarget.CHAT, new TargetConfig(false, preset("Aurora")));
        targets.put(AnimTarget.HOLOGRAM, new TargetConfig(true, preset("Vaporwave")));
        targets.put(AnimTarget.GUI, new TargetConfig(false, preset("Stealth")));
        targets.put(AnimTarget.OTHER, new TargetConfig(false, new AnimStyle()));
    }

    private static AnimStyle preset(String name) {
        Presets.Preset p = Presets.byName(name);
        AnimStyle s = p == null ? new AnimStyle() : p.create();
        s.presetName = p == null ? "" : p.name();
        return s;
    }

    public TargetConfig target(AnimTarget t) {
        return targets.computeIfAbsent(t, k -> new TargetConfig());
    }

    public AnimStyle style(AnimTarget t) {
        return target(t).style;
    }

    public boolean isActive(AnimTarget t) {
        return enabled && target(t).enabled && target(t).style.animates();
    }

    public Hologram hologram(String id) {
        for (Hologram h : holograms) {
            if (h.id.equalsIgnoreCase(id)) {
                return h;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ I/O

    public static Path defaultFile() {
        return Path.of("config", "neontext.json");
    }

    public static NeonConfig load(Path path) {
        NeonConfig cfg = new NeonConfig();
        cfg.file = path;
        if (path == null || !Files.exists(path)) {
            return cfg;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            cfg.fromJson(root);
        } catch (Exception e) {
            // keep the defaults, but move the broken file aside so the user can look at it
            try {
                Files.move(path, path.resolveSibling(path.getFileName() + ".broken"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // nothing else we can do
            }
            System.err.println("[NeonText] Could not read " + path + ", using defaults: " + e);
        }
        return cfg;
    }

    public void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(toJson()), StandardCharsets.UTF_8);
            firstRunDone = true;
        } catch (IOException e) {
            System.err.println("[NeonText] Could not save " + file + ": " + e);
        }
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("animateGui", animateGui);
        root.addProperty("timeScale", timeScale);
        root.addProperty("announce", announce);
        root.addProperty("firstRunDone", firstRunDone);

        JsonObject t = new JsonObject();
        for (AnimTarget target : AnimTarget.values()) {
            TargetConfig tc = targets.get(target);
            if (tc == null) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("enabled", tc.enabled);
            o.add("style", styleToJson(tc.style));
            t.add(target.name().toLowerCase(java.util.Locale.ROOT), o);
        }
        root.add("targets", t);

        JsonArray hs = new JsonArray();
        for (Hologram h : holograms) {
            hs.add(hologramToJson(h));
        }
        root.add("holograms", hs);
        return root;
    }

    public void fromJson(JsonObject root) {
        enabled = bool(root, "enabled", enabled);
        animateGui = bool(root, "animateGui", animateGui);
        timeScale = num(root, "timeScale", timeScale);
        announce = bool(root, "announce", announce);
        firstRunDone = bool(root, "firstRunDone", firstRunDone);

        if (root.has("targets") && root.get("targets").isJsonObject()) {
            JsonObject t = root.getAsJsonObject("targets");
            for (AnimTarget target : AnimTarget.values()) {
                String key = target.name().toLowerCase(java.util.Locale.ROOT);
                if (!t.has(key) || !t.get(key).isJsonObject()) {
                    continue;
                }
                JsonObject o = t.getAsJsonObject(key);
                TargetConfig tc = target(target);
                tc.enabled = bool(o, "enabled", tc.enabled);
                if (o.has("style") && o.get("style").isJsonObject()) {
                    tc.style = styleFromJson(o.getAsJsonObject("style"));
                }
            }
        }

        holograms.clear();
        if (root.has("holograms") && root.get("holograms").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("holograms")) {
                if (e.isJsonObject()) {
                    holograms.add(hologramFromJson(e.getAsJsonObject()));
                }
            }
        }
    }

    // ------------------------------------------------------------ json bits

    public static JsonObject styleToJson(AnimStyle s) {
        JsonObject o = new JsonObject();
        o.addProperty("preset", s.presetName == null ? "" : s.presetName);
        o.addProperty("effect", s.effect().id());
        o.addProperty("speed", s.speed);
        o.addProperty("amplitude", s.amplitude);
        o.addProperty("spread", s.spread);
        o.addProperty("saturation", s.saturation);
        o.addProperty("brightness", s.brightness);
        o.addProperty("scale", s.scale);
        o.addProperty("shadow", s.shadow);
        o.addProperty("glow", s.glow);
        o.addProperty("onlyMe", s.onlyMe);
        JsonArray c = new JsonArray();
        for (String hex : s.colors == null ? AnimStyle.defaultColors() : s.colors) {
            c.add(hex);
        }
        o.add("colors", c);
        return o;
    }

    public static AnimStyle styleFromJson(JsonObject o) {
        AnimStyle s = new AnimStyle();
        s.presetName = str(o, "preset", "");
        s.setEffect(Effect.fromId(str(o, "effect", s.effect().id())));
        s.speed = num(o, "speed", s.speed);
        s.amplitude = num(o, "amplitude", s.amplitude);
        s.spread = num(o, "spread", s.spread);
        s.saturation = num(o, "saturation", s.saturation);
        s.brightness = num(o, "brightness", s.brightness);
        s.scale = num(o, "scale", s.scale);
        s.shadow = bool(o, "shadow", s.shadow);
        s.glow = bool(o, "glow", s.glow);
        s.onlyMe = bool(o, "onlyMe", s.onlyMe);
        if (o.has("colors") && o.get("colors").isJsonArray()) {
            List<String> colors = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("colors")) {
                colors.add(e.getAsString());
            }
            if (!colors.isEmpty()) {
                s.colors = colors.toArray(new String[0]);
            }
        }
        return s.sanitize();
    }

    public static JsonObject hologramToJson(Hologram h) {
        JsonObject o = new JsonObject();
        o.addProperty("id", h.id);
        o.addProperty("text", h.text);
        o.addProperty("x", h.x);
        o.addProperty("y", h.y);
        o.addProperty("z", h.z);
        o.addProperty("scale", h.scale);
        o.addProperty("shadow", h.shadow);
        o.addProperty("background", Integer.toHexString(h.background));
        o.addProperty("billboard", h.billboard);
        o.addProperty("maxDistance", h.maxDistance);
        o.addProperty("enabled", h.enabled);
        o.add("style", styleToJson(h.style));
        return o;
    }

    public static Hologram hologramFromJson(JsonObject o) {
        Hologram h = new Hologram();
        h.id = str(o, "id", h.id);
        h.text = str(o, "text", h.text);
        h.x = num(o, "x", (float) h.x);
        h.y = num(o, "y", (float) h.y);
        h.z = num(o, "z", (float) h.z);
        h.scale = num(o, "scale", h.scale);
        h.shadow = bool(o, "shadow", h.shadow);
        h.background = (int) Long.parseLong(str(o, "background", "90000000"), 16);
        h.billboard = bool(o, "billboard", h.billboard);
        h.maxDistance = (int) num(o, "maxDistance", h.maxDistance);
        h.enabled = bool(o, "enabled", h.enabled);
        if (o.has("style") && o.get("style").isJsonObject()) {
            h.style = styleFromJson(o.getAsJsonObject("style"));
        }
        return h;
    }

    private static boolean bool(JsonObject o, String k, boolean d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsBoolean() : d;
    }

    private static float num(JsonObject o, String k, float d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsFloat() : d;
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d;
    }

    /** Resets every target back to its default preset, keeping holograms. */
    public void resetStyles() {
        NeonConfig fresh = new NeonConfig();
        targets.putAll(fresh.targets);
        enabled = true;
        animateGui = false;
        timeScale = 1.0f;
    }

    /** Applies one preset to a target. */
    public void applyPreset(AnimTarget target, Presets.Preset preset) {
        AnimStyle s = preset.create();
        s.presetName = preset.name();
        target(target).style = s;
    }

    public Palette paletteOf(AnimTarget target) {
        return style(target).palette();
    }
}
