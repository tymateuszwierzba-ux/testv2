package pl.neontext.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import pl.neontext.client.cfg.NeonConfig;
import pl.neontext.client.core.NeonCommands;
import pl.neontext.client.core.NeonRuntime;
import pl.neontext.client.gui.NeonScreen;
import pl.neontext.client.holo.HologramManager;

/**
 * Client entrypoint: loads the config, registers the GUI keybind and the /neon command, and drives
 * the animation clock.
 */
public final class NeonTextClient implements ClientModInitializer {

    public static final String MOD_ID = "neontext";
    public static final String MOD_NAME = "NeonText";
    public static final String VERSION = "1.0.0";

    /** Opens the NeonText GUI. Rebindable in Options -> Controls. */
    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        NeonRuntime.setConfig(NeonConfig.load(NeonConfig.defaultFile()));

        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.neontext.open",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_K,
                KeyMapping.Category.MISC));

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

        while (openGuiKey != null && openGuiKey.consumeClick()) {
            // 26.x: screens live on the Gui instance, Minecraft.setScreen no longer exists
            mc.gui.setScreen(new NeonScreen(null));
        }

        if (mc.player != null && NeonRuntime.config().announce) {
            NeonRuntime.config().announce = false;
            NeonRuntime.save();
            mc.gui.chatListener().handleSystemMessage(Component.literal(
                    "\u00A7b\u00A7lNeonText \u00A77loaded \u00A78\u00BB \u00A77press \u00A7eK\u00A77 or type \u00A7e/neon\u00A77"), false);
        }
    }

    public static KeyMapping openGuiKey() {
        return openGuiKey;
    }
}
