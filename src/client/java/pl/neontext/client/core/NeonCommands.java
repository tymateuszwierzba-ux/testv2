package pl.neontext.client.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.anim.Effect;
import pl.neontext.client.cfg.NeonConfig;
import pl.neontext.client.cfg.Presets;
import pl.neontext.client.gui.NeonScreen;
import pl.neontext.client.holo.Hologram;
import pl.neontext.client.holo.HologramManager;

import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Client-side {@code /neon} command tree. Everything here runs on the client only - the server never
 * sees it and never has to allow it.
 */
public final class NeonCommands {

    private static final SuggestionProvider<FabricClientCommandSource> TARGETS = (ctx, builder) -> {
        for (AnimTarget t : AnimTarget.configurable()) {
            builder.suggest(t.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    };

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

    private NeonCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(literal("neon")
                .executes(ctx -> {
                    openGui(ctx, null);
                    return 1;
                })
                .then(literal("gui").executes(ctx -> {
                    openGui(ctx, null);
                    return 1;
                }))
                .then(literal("toggle").executes(NeonCommands::toggle))
                .then(literal("reload").executes(NeonCommands::reload))
                .then(literal("reset").executes(NeonCommands::reset))
                .then(literal("effect")
                        .then(argument("target", StringArgumentType.word()).suggests(TARGETS)
                                .then(argument("effect", StringArgumentType.word()).suggests(EFFECTS)
                                        .executes(NeonCommands::setEffect))))
                .then(literal("preset")
                        .then(argument("target", StringArgumentType.word()).suggests(TARGETS)
                                .then(argument("preset", StringArgumentType.greedyString()).suggests(PRESETS)
                                        .executes(NeonCommands::applyPreset))))
                .then(literal("speed")
                        .then(argument("target", StringArgumentType.word()).suggests(TARGETS)
                                .then(argument("value", FloatArgumentType.floatArg(0.0f, 10.0f))
                                        .executes(NeonCommands::setSpeed))))
                .then(literal("hologram")
                        .then(literal("list").executes(NeonCommands::listHolograms))
                        .then(literal("add")
                                .then(argument("id", StringArgumentType.word())
                                        .then(argument("x", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                        .then(argument("y", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                                .then(argument("z", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                                        .then(argument("text", StringArgumentType.greedyString())
                                                                .executes(NeonCommands::addHologram))))))))
                        .then(literal("here")
                                .then(argument("id", StringArgumentType.word())
                                        .then(argument("text", StringArgumentType.greedyString())
                                                .executes(NeonCommands::addHologramHere))))
                        .then(literal("remove")
                                .then(argument("id", StringArgumentType.word()).suggests(HOLOS)
                                        .executes(NeonCommands::removeHologram)))
                        .then(literal("text")
                                .then(argument("id", StringArgumentType.word()).suggests(HOLOS)
                                        .then(argument("text", StringArgumentType.greedyString())
                                                .executes(NeonCommands::setHologramText))))
                        .then(literal("move")
                                .then(argument("id", StringArgumentType.word()).suggests(HOLOS)
                                        .then(argument("x", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                        .then(argument("y", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                                .then(argument("z", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                                        .executes(NeonCommands::moveHologram)))))))
                        .then(literal("scale")
                                .then(argument("id", StringArgumentType.word()).suggests(HOLOS)
                                        .then(argument("value", FloatArgumentType.floatArg(0.2f, 8.0f))
                                                .executes(NeonCommands::scaleHologram))))
                        .then(literal("edit")
                                .then(argument("id", StringArgumentType.word()).suggests(HOLOS)
                                        .executes(NeonCommands::editHologram)))));
    }

    // ------------------------------------------------------------------ gui

    private static int openGui(CommandContext<FabricClientCommandSource> ctx, String tab) {
        ctx.getSource().getClient().execute(() ->
                ctx.getSource().getClient().gui.setScreen(new NeonScreen(tab)));
        return 1;
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        NeonConfigToggle();
        boolean on = NeonRuntime.config().enabled;
        info(ctx, (on ? "\u00A7aenabled" : "\u00A7cdisabled") + "\u00A77 animations");
        return 1;
    }

    private static void NeonConfigToggle() {
        NeonRuntime.config().enabled = !NeonRuntime.config().enabled;
        NeonRuntime.save();
    }

    private static int reload(CommandContext<FabricClientCommandSource> ctx) {
        java.nio.file.Path path = NeonConfig.defaultFile();
        NeonRuntime.setConfig(NeonConfig.load(path));
        HologramManager.loadFrom(NeonRuntime.config());
        info(ctx, "\u00A7aconfig reloaded");
        return 1;
    }

    private static int reset(CommandContext<FabricClientCommandSource> ctx) {
        java.util.List<Hologram> kept = new java.util.ArrayList<>(NeonRuntime.config().holograms);
        NeonRuntime.config().resetStyles();
        NeonRuntime.config().holograms.clear();
        NeonRuntime.config().holograms.addAll(kept);
        NeonRuntime.save();
        info(ctx, "\u00A7aall animation styles reset to defaults");
        return 1;
    }

    // --------------------------------------------------------------- styles

    private static int setEffect(CommandContext<FabricClientCommandSource> ctx) {
        AnimTarget target = target(ctx);
        if (target == null) {
            return 0;
        }
        Effect effect = Effect.fromId(StringArgumentType.getString(ctx, "effect"));
        AnimStyle style = NeonRuntime.config().style(target);
        style.setEffect(effect);
        style.presetName = "";
        NeonRuntime.config().target(target).enabled = true;
        NeonRuntime.save();
        info(ctx, "\u00A77" + label(target) + " \u00A78-> \u00A7b" + effect.displayName());
        return 1;
    }

    private static int applyPreset(CommandContext<FabricClientCommandSource> ctx) {
        AnimTarget target = target(ctx);
        if (target == null) {
            return 0;
        }
        String wanted = StringArgumentType.getString(ctx, "preset").replace('_', ' ');
        Presets.Preset preset = Presets.byName(wanted);
        if (preset == null) {
            error(ctx, "\u00A7cno preset called '" + wanted + "'");
            return 0;
        }
        NeonRuntime.config().applyPreset(target, preset);
        NeonRuntime.config().target(target).enabled = true;
        NeonRuntime.save();
        info(ctx, "\u00A77" + label(target) + " \u00A78-> \u00A7d" + preset.name());
        return 1;
    }

    private static int setSpeed(CommandContext<FabricClientCommandSource> ctx) {
        AnimTarget target = target(ctx);
        if (target == null) {
            return 0;
        }
        float value = FloatArgumentType.getFloat(ctx, "value");
        NeonRuntime.config().style(target).speed = value;
        NeonRuntime.save();
        info(ctx, "\u00A77" + label(target) + " speed \u00A78-> \u00A7e" + value);
        return 1;
    }

    // ------------------------------------------------------------ holograms

    private static int listHolograms(CommandContext<FabricClientCommandSource> ctx) {
        if (NeonRuntime.config().holograms.isEmpty()) {
            info(ctx, "\u00A77no holograms yet - try \u00A7e/neon hologram here <id> <text>\u00A77");
            return 1;
        }
        info(ctx, "\u00A7b\u00A7lHolograms \u00A78(" + NeonRuntime.config().holograms.size() + ")");
        for (Hologram h : NeonRuntime.config().holograms) {
            info(ctx, " \u00A7e" + h.id + " \u00A78[" + fmt(h.x) + ", " + fmt(h.y) + ", " + fmt(h.z)
                    + "] \u00A77\"" + h.text + "\"" + (h.enabled ? "" : " \u00A7c(off)"));
        }
        return 1;
    }

    private static int addHologram(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double y = DoubleArgumentType.getDouble(ctx, "y");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        String text = StringArgumentType.getString(ctx, "text");
        return createHologram(ctx, id, text, x, y, z);
    }

    private static int addHologramHere(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String text = StringArgumentType.getString(ctx, "text");
        var p = ctx.getSource().getPlayer();
        if (p == null) {
            error(ctx, "\u00A7cyou need to be in a world");
            return 0;
        }
        return createHologram(ctx, id, text, p.getX(), p.getY() + 2.2, p.getZ());
    }

    private static int createHologram(CommandContext<FabricClientCommandSource> ctx, String id, String text,
                                      double x, double y, double z) {
        if (NeonRuntime.config().hologram(id) != null) {
            error(ctx, "\u00A7ca hologram called '" + id + "' already exists");
            return 0;
        }
        Hologram h = new Hologram(id, text, x, y, z);
        h.style = NeonRuntime.config().style(AnimTarget.HOLOGRAM).copy();
        NeonRuntime.config().holograms.add(h);
        NeonRuntime.save();
        HologramManager.loadFrom(NeonRuntime.config());
        info(ctx, "\u00A7acreated hologram \u00A7e" + id + " \u00A77at " + fmt(x) + ", " + fmt(y) + ", " + fmt(z));
        return 1;
    }

    private static int removeHologram(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        Hologram h = NeonRuntime.config().hologram(id);
        if (h == null) {
            error(ctx, "\u00A7cno hologram called '" + id + "'");
            return 0;
        }
        HologramManager.remove(h);
        NeonRuntime.config().holograms.remove(h);
        NeonRuntime.save();
        info(ctx, "\u00A7cremoved hologram \u00A7e" + id);
        return 1;
    }

    private static int setHologramText(CommandContext<FabricClientCommandSource> ctx) {
        Hologram h = holo(ctx);
        if (h == null) {
            return 0;
        }
        h.text = StringArgumentType.getString(ctx, "text");
        NeonRuntime.save();
        info(ctx, "\u00A7a" + h.id + " \u00A77text set");
        return 1;
    }

    private static int moveHologram(CommandContext<FabricClientCommandSource> ctx) {
        Hologram h = holo(ctx);
        if (h == null) {
            return 0;
        }
        h.x = DoubleArgumentType.getDouble(ctx, "x");
        h.y = DoubleArgumentType.getDouble(ctx, "y");
        h.z = DoubleArgumentType.getDouble(ctx, "z");
        NeonRuntime.save();
        info(ctx, "\u00A7a" + h.id + " \u00A77moved to " + fmt(h.x) + ", " + fmt(h.y) + ", " + fmt(h.z));
        return 1;
    }

    private static int scaleHologram(CommandContext<FabricClientCommandSource> ctx) {
        Hologram h = holo(ctx);
        if (h == null) {
            return 0;
        }
        h.scale = FloatArgumentType.getFloat(ctx, "value");
        NeonRuntime.save();
        info(ctx, "\u00A7a" + h.id + " \u00A77scale \u00A78-> \u00A7e" + h.scale);
        return 1;
    }

    private static int editHologram(CommandContext<FabricClientCommandSource> ctx) {
        Hologram h = holo(ctx);
        if (h == null) {
            return 0;
        }
        ctx.getSource().getClient().execute(() ->
                ctx.getSource().getClient().gui.setScreen(new NeonScreen("holograms", h.id)));
        return 1;
    }

    // ---------------------------------------------------------------- utils

    private static AnimTarget target(CommandContext<FabricClientCommandSource> ctx) {
        String raw = StringArgumentType.getString(ctx, "target").toUpperCase(Locale.ROOT);
        try {
            AnimTarget t = AnimTarget.valueOf(raw);
            if (t == AnimTarget.OTHER) {
                error(ctx, "\u00A7cthat target cannot be configured");
                return null;
            }
            return t;
        } catch (IllegalArgumentException e) {
            error(ctx, "\u00A7cunknown target '" + raw + "' \u00A77- use nameplate, tab, chat, hologram or gui");
            return null;
        }
    }

    private static Hologram holo(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        Hologram h = NeonRuntime.config().hologram(id);
        if (h == null) {
            error(ctx, "\u00A7cno hologram called '" + id + "'");
        }
        return h;
    }

    private static String label(AnimTarget t) {
        return t.displayName();
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static void info(CommandContext<FabricClientCommandSource> ctx, String text) {
        ctx.getSource().sendFeedback(prefix().append(Component.literal(text)));
    }

    private static void error(CommandContext<FabricClientCommandSource> ctx, String text) {
        ctx.getSource().sendError(prefix().append(Component.literal(text)));
    }

    private static Component prefix() {
        return Component.literal("\u00A7b\u00A7lNeon \u00A78\u00BB \u00A7r");
    }
}
