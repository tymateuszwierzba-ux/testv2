package pl.neontext.client.anim;

/**
 * Which piece of the UI a text draw belongs to. The render hooks stamp every text draw with a
 * target so each one can be configured (and toggled) separately.
 */
public enum AnimTarget {

    /** Text above entities / players in the world. */
    NAMEPLATE("Nameplates", "Text over players and entities"),

    /** The TAB player list. */
    TAB("Tab list", "Player list while holding TAB"),

    /** The chat overlay and the chat screen. */
    CHAT("Chat", "All chat messages"),

    /** Client-side holograms placed in the world by this mod. */
    HOLOGRAM("Holograms", "Your own world text"),

    /** Any other GUI/HUD text: tooltips, buttons, screens, boss bars... */
    GUI("GUI & HUD", "Everything else on screen"),

    /** Fallback - never configured directly. */
    OTHER("Other", "Uncategorised text");

    private final String displayName;
    private final String description;

    AnimTarget(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    /** The five targets the GUI exposes as tabs (OTHER is internal). */
    public static AnimTarget[] configurable() {
        return new AnimTarget[]{NAMEPLATE, TAB, CHAT, HOLOGRAM, GUI};
    }
}
