package pl.neontext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared (environment-agnostic) constants for NeonText.
 * <p>
 * NeonText is a 100% client-side mod, but keeping the mod id / logger here means the
 * client package never has to repeat string literals.
 */
public final class NeonText {

    public static final String MOD_ID = "neontext";
    public static final String MOD_NAME = "NeonText";
    public static final String MOD_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    /** Default key used to open the GUI: K */
    public static final String DEFAULT_KEY = "key.keyboard.k";

    private NeonText() {
    }
}
