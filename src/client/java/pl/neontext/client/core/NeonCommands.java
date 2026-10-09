package pl.neontext.client.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.anim.Effect;
import pl.neontext.client.anim.Palette;
import pl.neontext.client.cfg.NeonConfig;
import pl.neontext.client.cfg.Presets;
import pl.neontext.client.holo.Hologram;
import pl.neontext.client.holo.HologramManager;

import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Client-side {@code /neon} command tree - the whole UI of the mod. Select a thing by clicking it
 * (hologram / chat text) or with {@code /neon select}, then edit it with the style commands.
 */
public final class NeonCommands {

    private static final SuggestionProvider<FabricClientCommandSource> EFFECTS = (ctx, builder) -> {
        for (Effect e : Effect.values()) {
            builder.suggest(e.id());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> PRESETS = (ctx, builder) -> {
        for (Presets.Preset p : Presets.ALL) {
            builder.suggest(p.name().replace(' ', '_'));
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> HOLOS = (ctx, builder) -> {
        for (Hologram h : NeonRuntime.config().holograms) {
            builder.suggest(h.id);
        }
        return builder.buildFuture();
    };

    /** What {@code /neon select} accepts: targets, "none" and every hologram id. */
    private static final SuggestionProvider<FabricClientCommandSource> WHO = (ctx, builder) -> {
        for (AnimTarget t : AnimTarget.configurable()) {
            builder.suggest(t.name().toLowerCase(Locale.ROOT));
        }
        builder.suggest("none");
        for (Hologram h : NeonRuntime.config().holograms) {
            builder.suggest(h.id);
        }
        return builder.buildFuture();
    };

    private NeonCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(literal("neon")
                .executes(ctx -> {
                    status(ctx.getSource());
                    return 1;
                })
                .then(literal("help").executes(ctx -> {
                    help(ctx.getSource());
                    return 1;
                }))
                .then(literal("toggle").executes(ctx -> {
                    NeonRuntime.config().enabled = !NeonRuntime.config().enabled;
                    NeonRuntime.save();
                    ok(ctx.getSource(), "Animations are now " + onOff(NeonRuntime.config().enabled));
                    return 1;
                }))
                .then(literal("on").executes(ctx -> setSelectionEnabled(ctx.getSource(), true)))
                .then(literal("off").executes(ctx -> setSelectionEnabled(ctx.getSource(), false)))
                .then(literal("select")
                        .executes(ctx -> selectUnderCrosshair(ctx.getSource()))
                        .then(argument("who", StringArgumentType.word()).suggests(WHO).executes(ctx -> {
                            return select(ctx.getSource(), StringArgumentType.getString(ctx, "who"));
                        })))
                .then(literal("next").executes(ctx -> {
                    if (!requireSelection(ctx.getSource())) {
                        return 0;
                    }
                    NeonClick.cycleSelection();
                    return 1;
                }))
                .then(literal("effect")
                        .then(argument("effect", StringArgumentType.word()).suggests(EFFECTS).executes(ctx -> {
                            Effect e = Effect.fromId(StringArgumentType.getString(ctx, "effect"));
                            if (e == null) {
                                error(ctx.getSource(), "Unknown effect. Try /neon effect tab");
                                return 0;
                            }
                            AnimStyle s = selectedStyle(ctx.getSource());
                            if (s == null) {
                                return 0;
                            }
                            s.setEffect(e);
                            s.sanitize();
                            finishStyleEdit(ctx.getSource(), "effect: §b" + e.id());
                            return 1;
                        })))
                .then(literal("preset")
                        .then(argument("preset", StringArgumentType.greedyString()).suggests(PRESETS).executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "preset").replace('_', ' ');
                            Presets.Preset p = Presets.byName(name);
                            if (p == null) {
                                error(ctx.getSource(), "Unknown preset. Try /neon preset tab");
                                return 0;
                            }
                            AnimStyle applied = p.create();
                            if (applyStyle(ctx.getSource(), applied)) {
                                finishStyleEdit(ctx.getSource(), "preset: §b" + p.name());
                                return 1;
                            }
                            return 0;
                        })))
                .then(literal("speed").then(argument("value", FloatArgumentType.floatArg(0.0f, 10.0f))
                        .executes(ctx -> setFloat(ctx.getSource(), "speed",
                                FloatArgumentType.getFloat(ctx, "value")))))
                .then(literal("amplitude").then(argument("value", FloatArgumentType.floatArg(0.0f, 5.0f))
                        .executes(ctx -> setFloat(ctx.getSource(), "amplitude",
                                FloatArgumentType.getFloat(ctx, "value")))))
                .then(literal("spread").then(argument("value", FloatArgumentType.floatArg(0.05f, 8.0f))
                        .executes(ctx -> setFloat(ctx.getSource(), "spread",
                                FloatArgumentType.getFloat(ctx, "value")))))
                .then(literal("size").then(argument("value", FloatArgumentType.floatArg(0.25f, 4.0f))
                        .executes(ctx -> setFloat(ctx.getSource(), "size",
                                FloatArgumentType.getFloat(ctx, "value")))))
                .then(literal("saturation").then(argument("value", FloatArgumentType.floatArg(0.0f, 3.0f))
                        .executes(ctx -> setFloat(ctx.getSource(), "saturation",
                                FloatArgumentType.getFloat(ctx, "value")))))
                .then(literal("brightness").then(argument("value", FloatArgumentType.floatArg(0.0f, 3.0f))
                        .executes(ctx -> setFloat(ctx.getSource(), "brightness",
                                FloatArgumentType.getFloat(ctx, "value")))))
                .then(literal("shadow")
                        .then(literal("on").executes(ctx -> setFlag(ctx.getSource(), "shadow", true)))
                        .then(literal("off").executes(ctx -> setFlag(ctx.getSource(), "shadow", false))))
                .then(literal("glow")
                        .then(literal("on").executes(ctx -> setFlag(ctx.getSource(), "glow", true)))
                        .then(literal("off").executes(ctx -> setFlag(ctx.getSource(), "glow", false))))
                .then(literal("onlyme")
                        .then(literal("on").executes(ctx -> setFlag(ctx.getSource(), "onlyme", true)))
                        .then(literal("off").executes(ctx -> setFlag(ctx.getSource(), "onlyme", false))))
                .then(literal("palette")
                        .then(argument("colors", StringArgumentType.greedyString()).executes(ctx -> {
                            AnimStyle s = selectedStyle(ctx.getSource());
                            if (s == null) {
                                return 0;
                            }
                            String arg = StringArgumentType.getString(ctx, "colors").trim();
                            Palette p = parsePalette(arg);
                            if (p == null) {
                                error(ctx.getSource(), "Use: /neon palette rainbow  or  /neon palette #FF0000 #00FF00 #0000FF");
                                return 0;
                            }
                            s.setPalette(p);
                            finishStyleEdit(ctx.getSource(), "palette updated (" + p.colors().length + " colours)");
                            return 1;
                        })))
                .then(literal("reset").executes(ctx -> {
                    if (NeonRuntime.selectedHolo != null) {
                        Hologram h = selectedHolo(ctx.getSource());
                        if (h == null) {
                            return 0;
                        }
                        h.style = new AnimStyle();
                        NeonRuntime.save();
                        ok(ctx.getSource(), "Style of §b" + h.id + "§r reset to defaults");
                        return 1;
                    }
                    if (NeonRuntime.selectedTarget != null) {
                        NeonRuntime.config().target(NeonRuntime.selectedTarget).style = new AnimStyle();
                        NeonRuntime.save();
                        ok(ctx.getSource(), "Style of §b" + NeonClick.targetLabel(NeonRuntime.selectedTarget)
                                + "§r reset to defaults");
                        return 1;
                    }
                    error(ctx.getSource(), "Nothing selected - /neon select nameplate|tab|chat|...");
                    return 0;
                }))
                .then(literal("reload").executes(ctx -> {
                    NeonRuntime.setConfig(pl.neontext.client.cfg.NeonConfig.load(
                            pl.neontext.client.cfg.NeonConfig.defaultFile()));
                    ok(ctx.getSource(), "Config reloaded from disk");
                    return 1;
                }))
                .then(literal("debug").executes(ctx -> {
                    debug(ctx.getSource());
                    return 1;
                }))
                .then(literal("creators").executes(ctx -> {
                    creators(ctx.getSource());
                    return 1;
                }))
                .then(literal("click")
                        .then(literal("on").executes(ctx -> {
                            NeonRuntime.clickSelect = true;
                            ok(ctx.getSource(), "Click-to-select is §aon");
                            return 1;
                        }))
                        .then(literal("off").executes(ctx -> {
                            NeonRuntime.clickSelect = false;
                            ok(ctx.getSource(), "Click-to-select is §coff");
                            return 1;
                        })))
                .then(literal("holo")
                        .then(literal("list").executes(ctx -> {
                            listHolograms(ctx.getSource());
                            return 1;
                        }))
                        .then(literal("add")
                                .then(argument("id", StringArgumentType.word()).suggests(HOLOS)
                                        .then(argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                                            return addHologram(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                                                    StringArgumentType.getString(ctx, "text"), false);
                                        }))))
                        .then(literal("here")
                                .then(argument("id", StringArgumentType.word()).suggests(HOLOS)
                                        .then(argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                                            return addHologram(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                                                    StringArgumentType.getString(ctx, "text"), true);
                                        }))))
                        .then(literal("remove")
                                .then(argument("id", StringArgumentType.word()).suggests(HOLOS).executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    Hologram h = NeonRuntime.config().hologram(id);
                                    if (h == null) {
                                        error(ctx.getSource(), "No hologram called §c" + id);
                                        return 0;
                                    }
                                    HologramManager.remove(h);
                                    if (id.equals(NeonRuntime.selectedHolo)) {
                                        NeonRuntime.selectNone();
                                    }
                                    NeonRuntime.save();
                                    ok(ctx.getSource(), "Removed hologram §b" + id);
                                    return 1;
                                })))
                        .then(literal("text")
                                .then(argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                                    Hologram h = selectedHolo(ctx.getSource());
                                    if (h == null) {
                                        return 0;
                                    }
                                    h.text = StringArgumentType.getString(ctx, "text");
                                    NeonRuntime.save();
                                    ok(ctx.getSource(), "Text of §b" + h.id + "§r updated");
                                    return 1;
                                })))
                        .then(literal("move")
                                .then(argument("x", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                        .then(argument("y", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                                .then(argument("z", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                                        .executes(ctx -> {
                                                            Hologram h = selectedHolo(ctx.getSource());
                                                            if (h == null) {
                                                                return 0;
                                                            }
                                                            h.x = DoubleArgumentType.getDouble(ctx, "x");
                                                            h.y = DoubleArgumentType.getDouble(ctx, "y");
                                                            h.z = DoubleArgumentType.getDouble(ctx, "z");
                                                            NeonRuntime.save();
                                                            ok(ctx.getSource(), "Moved §b" + h.id + "§r to "
                                                                    + fmt(h.x) + " " + fmt(h.y) + " " + fmt(h.z));
                                                            return 1;
                                                        })))))
                        .then(literal("scale")
                                .then(argument("value", FloatArgumentType.floatArg(0.05f, 10.0f)).executes(ctx -> {
                                    Hologram h = selectedHolo(ctx.getSource());
                                    if (h == null) {
                                        return 0;
                                    }
                                    h.scale = FloatArgumentType.getFloat(ctx, "value");
                                    NeonRuntime.save();
                                    ok(ctx.getSource(), "Scale of §b" + h.id + "§r = §b" + fmt(h.scale));
                                    return 1;
                                })))
                        .then(literal("distance")
                                .then(argument("value", IntegerArgumentType.integer(1, 256)).executes(ctx -> {
                                    Hologram h = selectedHolo(ctx.getSource());
                                    if (h == null) {
                                        return 0;
                                    }
                                    h.maxDistance = IntegerArgumentType.getInteger(ctx, "value");
                                    NeonRuntime.save();
                                    ok(ctx.getSource(), "View distance of §b" + h.id + "§r = §b" + h.maxDistance);
                                    return 1;
                                })))));
    }

    // ------------------------------------------------------------------ selection

    private static int selectUnderCrosshair(FabricClientCommandSource src) {
        Hologram h = NeonClick.pickUnderCrosshair();
        if (h == null) {
            error(src, "No hologram under the crosshair - use /neon select <name>");
            return 0;
        }
        NeonRuntime.selectHolo(h.id);
        ok(src, "Selected hologram §b" + h.id + "§r - /neon next to swap its style");
        return 1;
    }

    private static int select(FabricClientCommandSource src, String who) {
        String w = who.toLowerCase(Locale.ROOT);
        if (w.equals("none")) {
            NeonRuntime.selectNone();
            ok(src, "Selection cleared");
            return 1;
        }
        for (AnimTarget t : AnimTarget.configurable()) {
            if (t.name().toLowerCase(Locale.ROOT).equals(w)) {
                NeonRuntime.select(t);
                ok(src, "Selected §b" + NeonClick.targetLabel(t) + "§r - /neon next to swap its style");
                return 1;
            }
        }
        Hologram h = NeonRuntime.config().hologram(who);
        if (h != null) {
            NeonRuntime.selectHolo(h.id);
            ok(src, "Selected hologram §b" + h.id + "§r - /neon next to swap its style");
            return 1;
        }
        error(src, "Unknown target §c" + who + "§r - try nameplate, tab, chat, holograms, gui or a hologram id");
        return 0;
    }

    private static boolean requireSelection(FabricClientCommandSource src) {
        if (NeonRuntime.hasSelection()) {
            return true;
        }
        error(src, "Nothing selected - click a hologram/chat text or use /neon select nameplate|tab|chat|...");
        return false;
    }

    /** @return the style of the current selection, or null (with an error sent) when there is none. */
    private static AnimStyle selectedStyle(FabricClientCommandSource src) {
        if (!requireSelection(src)) {
            return null;
        }
        if (NeonRuntime.selectedHolo != null) {
            Hologram h = selectedHolo(src);
            return h == null ? null : h.style;
        }
        return NeonRuntime.config().target(NeonRuntime.selectedTarget).style;
    }

    private static Hologram selectedHolo(FabricClientCommandSource src) {
        Hologram h = NeonRuntime.config().hologram(NeonRuntime.selectedHolo);
        if (h == null) {
            error(src, "The selected hologram no longer exists");
            NeonRuntime.selectNone();
        }
        return h;
    }

    /** Applies a fully built style to the current selection. */
    private static boolean applyStyle(FabricClientCommandSource src, AnimStyle style) {
        if (!requireSelection(src)) {
            return false;
        }
        if (NeonRuntime.selectedHolo != null) {
            Hologram h = selectedHolo(src);
            if (h == null) {
                return false;
            }
            h.style = style;
            h.enabled = true;
        } else {
            NeonRuntime.config().target(NeonRuntime.selectedTarget).style = style;
            NeonRuntime.config().target(NeonRuntime.selectedTarget).enabled = true;
            if (NeonRuntime.selectedTarget == AnimTarget.GUI) {
                NeonRuntime.config().animateGui = true;
            }
        }
        return true;
    }

    /** Small edit helper: keep the current style, enable it, save, report. */
    private static void finishStyleEdit(FabricClientCommandSource src, String what) {
        AnimStyle s = selectedStyle(src);
        if (s == null) {
            return;
        }
        // enabling on edit is the "convert it for me" behaviour
        if (NeonRuntime.selectedHolo != null) {
            selectedHolo(src).enabled = true;
        } else if (NeonRuntime.selectedTarget != null) {
            NeonRuntime.config().target(NeonRuntime.selectedTarget).enabled = true;
        }
        NeonRuntime.save();
        ok(src, selectionLabel() + " §r-> " + what);
    }

    private static String selectionLabel() {
        return NeonRuntime.selectedHolo != null ? "§bHologram " + NeonRuntime.selectedHolo
                : NeonRuntime.selectedTarget != null ? "§b" + NeonClick.targetLabel(NeonRuntime.selectedTarget)
                : "§7(nothing selected)";
    }

    private static int setSelectionEnabled(FabricClientCommandSource src, boolean value) {
        if (!requireSelection(src)) {
            return 0;
        }
        if (NeonRuntime.selectedHolo != null) {
            Hologram h = selectedHolo(src);
            if (h == null) {
                return 0;
            }
            h.enabled = value;
        } else {
            NeonRuntime.config().target(NeonRuntime.selectedTarget).enabled = value;
        }
        NeonRuntime.save();
        ok(src, selectionLabel() + " §r-> " + onOff(value));
        return 1;
    }

    private static int setFloat(FabricClientCommandSource src, String field, float value) {
        AnimStyle s = selectedStyle(src);
        if (s == null) {
            return 0;
        }
        switch (field) {
            case "speed" -> s.speed = value;
            case "amplitude" -> s.amplitude = value;
            case "spread" -> s.spread = value;
            case "size" -> s.scale = value;
            case "saturation" -> s.saturation = value;
            case "brightness" -> s.brightness = value;
        }
        s.sanitize();
        finishStyleEdit(src, field + " = §b" + fmt(value));
        return 1;
    }

    private static int setFlag(FabricClientCommandSource src, String field, boolean value) {
        AnimStyle s = selectedStyle(src);
        if (s == null) {
            return 0;
        }
        switch (field) {
            case "shadow" -> s.shadow = value;
            case "glow" -> s.glow = value;
            case "onlyme" -> s.onlyMe = value;
        }
        finishStyleEdit(src, field + " = " + onOff(value));
        return 1;
    }

    private static Palette parsePalette(String arg) {
        if (arg.equalsIgnoreCase("rainbow")) {
            return Palette.rainbow();
        }
        String[] parts = arg.split("\\s+");
        java.util.List<String> hex = new java.util.ArrayList<>();
        for (String p : parts) {
            String v = p.startsWith("#") ? p : "#" + p;
            if (!v.matches("#[0-9a-fA-F]{6}")) {
                return null;
            }
            hex.add(v);
        }
        return hex.isEmpty() ? null : Palette.of(hex.toArray(new String[0]));
    }

    // ------------------------------------------------------------------ info screens

    private static void status(FabricClientCommandSource src) {
        NeonConfig cfg = NeonRuntime.config();
        chat(src, "§b§l[NeonText] §8» §fanimations " + onOff(cfg.enabled)
                + " §8| §7click-to-select " + onOff(NeonRuntime.clickSelect));
        chat(src, "§7Selection: " + selectionLabel());
        if (NeonRuntime.hasSelection()) {
            Hologram h = NeonRuntime.selectedHolo != null
                    ? cfg.hologram(NeonRuntime.selectedHolo) : null;
            AnimStyle s = h != null ? h.style
                    : NeonRuntime.selectedTarget != null ? cfg.target(NeonRuntime.selectedTarget).style : null;
            if (s != null) {
                chat(src, "§7Style: §f" + s.effect().id() + " §8| §7speed §f" + fmt(s.speed)
                        + " §8| §7size §f" + fmt(s.scale) + " §8| §7glow " + onOff(s.glow));
            }
        }
        chat(src, "§7Targets: §f" + targetSummary(cfg));
        chat(src, "§8Type §f/neon help§8 for all commands.");
    }

    private static String targetSummary(pl.neontext.client.cfg.NeonConfig cfg) {
        StringBuilder sb = new StringBuilder();
        for (AnimTarget t : AnimTarget.configurable()) {
            boolean on = cfg.target(t).enabled && cfg.target(t).style.animates();
            if (t == AnimTarget.GUI) {
                on = cfg.animateGui && cfg.target(t).style.animates();
            }
            if (sb.length() > 0) {
                sb.append("§8, ");
            }
            sb.append(on ? "§a" : "§8").append(NeonClick.targetLabel(t));
        }
        return sb.toString();
    }

    private static void help(FabricClientCommandSource src) {
        chat(src, "§b§l[NeonText] §8» §fClick a hologram or chat text to select it and swap its style.");
        chat(src, "§b/neon select §8<§fnameplate|tab|chat|holograms|gui|holo-id§8> §7- select by command");
        chat(src, "§b/neon next §7- swap to the next preset §8| §b/neon effect §8<§feffect§8> §8| §b/neon preset §8<§fpreset§8>");
        chat(src, "§b/neon speed|amplitude|spread|size|saturation|brightness §8<§fvalue§8>");
        chat(src, "§b/neon shadow|glow|onlyme §8<§fon|off§8> §8| §b/neon palette §8<§frainbow | #hex...§8>");
        chat(src, "§b/neon on|off|reset|toggle §7- edit the selection §8| §b/neon click §8<§fon|off§8>");
        chat(src, "§b/neon holo add|here|remove|list|text|move|scale|distance §7- world holograms");
        chat(src, "§8Other: /neon status · /neon reload · /neon debug · /neon creators");
    }

    private static void creators(FabricClientCommandSource src) {
        chat(src, "§b§l[NeonText] §8» §fCreated by:");
        chat(src, "§b§lTymoteusz §8» §fOwner & Developer §8- §b/tymoteusz_dev");
        chat(src, "§b§lOskar §8- §fHelper §8- §bhttps://github.com/Oskarko121");
    }

    private static void debug(FabricClientCommandSource src) {
        chat(src, "§b§l[NeonText debug]");
        chat(src, "§7master: " + onOff(NeonRuntime.masterEnabled())
                + " §8| §7time: §f" + NeonRuntime.time() + "ms"
                + " §8| §7animateGui: " + onOff(NeonRuntime.config().animateGui));
        chat(src, "§7selection: " + selectionLabel() + " §8| §7click: " + onOff(NeonRuntime.clickSelect));
        chat(src, "§7last tick: §fglyphs §f" + NeonDebug.glyphWrapsFrame
                + " §8| §frecolors §f" + NeonDebug.recolorsFrame
                + " §8| §fchat stamps §f" + NeonDebug.chatStampsFrame
                + " §8| §fholo stamps §f" + NeonDebug.holoStampsFrame);
        chat(src, "§7totals: §ffont wraps §f" + NeonDebug.fontWraps
                + " §8| §fgui wraps §f" + NeonDebug.guiWraps
                + " §8| §fgui skipped §f" + NeonDebug.guiSkipped);
        chat(src, "§7last text: §f" + NeonDebug.lastText + " §8(§7" + NeonDebug.lastResolved + "§8)");
    }

    private static void listHolograms(FabricClientCommandSource src) {
        var all = NeonRuntime.config().holograms;
        if (all.isEmpty()) {
            chat(src, "§8[NeonText] §7No holograms. Create one: §f/neon holo here myid My text");
            return;
        }
        chat(src, "§b§l[NeonText] §8» §fHolograms §8(" + all.size() + "):");
        for (Hologram h : all) {
            String mark = h.id.equals(NeonRuntime.selectedHolo) ? "§b▶ " : "§8- ";
            chat(src, mark + "§f" + h.id + " §8» §7" + h.text + " §8| "
                    + (h.enabled ? "§aon" : "§coff") + " §8| §7" + h.style.effect().id());
        }
    }

    private static int addHologram(FabricClientCommandSource src, String id, String text, boolean atPlayer) {
        if (NeonRuntime.config().hologram(id) != null) {
            error(src, "Hologram §c" + id + "§r already exists");
            return 0;
        }
        Hologram h = new Hologram(id, text, 0, 0, 0);
        if (atPlayer) {
            var player = src.getPlayer();
            if (player == null) {
                error(src, "Join a world first");
                return 0;
            }
            h.x = player.getX();
            h.y = player.getY() + 2.0;
            h.z = player.getZ();
        }
        HologramManager.add(h);
        NeonRuntime.selectHolo(h.id);
        NeonRuntime.save();
        ok(src, "Added hologram §b" + id + (atPlayer ? "§r at your position" : "§r at 0 0 0")
                + " §8- /neon holo move to place it");
        return 1;
    }

    // ------------------------------------------------------------------ output helpers

    private static String onOff(boolean value) {
        return value ? "§aon" : "§coff";
    }

    private static String fmt(double value) {
        String s = String.format(Locale.ROOT, "%.2f", value);
        if (s.endsWith(".00")) {
            s = s.substring(0, s.length() - 3);
        }
        return s;
    }

    private static void ok(FabricClientCommandSource src, String message) {
        chat(src, "§b[Neon] §7" + message);
    }

    private static void error(FabricClientCommandSource src, String message) {
        chat(src, "§c[Neon] §7" + message);
    }

    private static void chat(FabricClientCommandSource src, String message) {
        src.sendFeedback(Component.literal(message));
    }
}
