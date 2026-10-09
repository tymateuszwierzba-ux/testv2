package pl.neontext.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import pl.neontext.client.cfg.NeonConfig;
import pl.neontext.client.core.NeonCommands;
import pl.neontext.client.core.NeonDebug;
import pl.neontext.client.core.NeonRuntime;
import pl.neontext.client.holo.HologramManager;

/**
 * Client entrypoint: loads the config, registers the /neon command tree (the whole UI is commands)
 * and drives the animation clock.
 */
public final class NeonTextClient implements ClientModInitializer {

    public static final String MOD_ID = "neontext";
    public static final String MOD_NAME = "NeonText";
    public static final String VERSION = "1.0.0";

    @Override
    public void onInitializeClient() {
        NeonRuntime.setConfig(NeonConfig.load(NeonConfig.defaultFile()));

        ClientTickEvents.END_CLIENT_TICK.register(NeonTextClient::onClientTick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                NeonCommands.register(dispatcher));

        NeonConfig cfg = NeonRuntime.config();
        if (!cfg.firstRunDone) {
            cfg.firstRunDone = true;
            cfg.save();
        }
        HologramManager.loadFrom(cfg);
    }

    private static void onClientTick(Minecraft mc) {
        NeonRuntime.tick();
        NeonDebug.resetFrame();

        if (mc.player != null && NeonRuntime.config().announce) {
            NeonRuntime.config().announce = false;
            NeonRuntime.save();
            mc.gui.chatListener().handleSystemMessage(Component.literal(
                    "\u00A7b\u00A7lNeonText \u00A77loaded \u00A78\u00BB \u00A77type \u00A7e/neon\u00A77 - "
                            + "click a hologram or chat text to edit it, \u00A7e/neon help\u00A77 for all commands"), false);
        }
    }
}
