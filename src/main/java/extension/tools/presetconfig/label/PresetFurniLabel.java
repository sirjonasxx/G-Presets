package extension.tools.presetconfig.label;

import extension.tools.presetconfig.PresetJsonConfigurable;
import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings of the floor furni "wf_label" (text, color, access & always-open).
 *
 * The state is retrieved through the server packets:
 * - FurnitureLabelsAndRegions        (full room snapshot, sent on room load)
 * - FurnitureLabelsAndRegionsUpdate  (single/multiple item update)
 *
 * and applied through:
 * - SetFurnitureLabelSettings        (color, access & always-open)
 * - SetFurnitureLabel                (text)
 */
public class PresetFurniLabel implements PresetJsonConfigurable {

    private int furniId;

    private String text;
    private int color;      // hex color
    private int access;     // 0 = everybody, 1 = users with rights
    private boolean alwaysOpen;

    public PresetFurniLabel(int furniId, String text, int color, int access, boolean alwaysOpen) {
        this.furniId = furniId;
        this.text = text;
        this.color = color;
        this.access = access;
        this.alwaysOpen = alwaysOpen;
    }

    // deep copy constructor
    public PresetFurniLabel(PresetFurniLabel label) {
        this(label.furniId, label.text, label.color, label.access, label.alwaysOpen);
    }

    public PresetFurniLabel(JSONObject object) {
        furniId = object.getInt("furniId");
        text = object.getString("text");
        color = object.getInt("color");
        access = object.optInt("access", 0);
        alwaysOpen = object.optBoolean("alwaysOpen", false);
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();
        object.put("furniId", furniId);
        object.put("text", text);
        object.put("color", color);
        object.put("access", access);
        object.put("alwaysOpen", alwaysOpen);
        return object;
    }

    /**
     * Reads the entries of a FurnitureLabelsAndRegions or FurnitureLabelsAndRegionsUpdate packet.
     *
     * Layout: {i:count} followed by count * {i:kind}{i:itemId}{s:text}{i:color}{i:access}{b:alwaysOpen},
     * optionally followed by one trailing {i:unknown} (currently always 0, only seen in the Update variant).
     */
    public static List<PresetFurniLabel> readFromPacket(HPacket packet) {
        // this packet can have multiple listeners, always parse it from the start
        packet.resetReadIndex();

        List<PresetFurniLabel> labels = new ArrayList<>();

        int count = packet.readInteger();
        for (int i = 0; i < count; i++) {
            packet.readInteger(); // kind of the entry (unknown purpose, always 0 for labels)
            int itemId = packet.readInteger();
            String text = packet.readString();
            int color = packet.readInteger();
            int access = packet.readInteger();
            boolean alwaysOpen = packet.readBoolean();

            labels.add(new PresetFurniLabel(itemId, text, color, access, alwaysOpen));
        }

        if (packet.getBytesLength() - packet.getReadIndex() >= 4) {
            packet.readInteger(); // trailing unknown
        }

        return labels;
    }

    public int getFurniId() {
        return furniId;
    }

    public void setFurniId(int furniId) {
        this.furniId = furniId;
    }

    public String getText() {
        return text;
    }

    public int getColor() {
        return color;
    }

    public int getAccess() {
        return access;
    }

    public boolean isAlwaysOpen() {
        return alwaysOpen;
    }

    /**
     * Whether the other label holds the same settings (color, access & always-open).
     */
    public boolean hasSameSettingsAs(PresetFurniLabel other) {
        return other != null
                && color == other.color
                && access == other.access
                && alwaysOpen == other.alwaysOpen;
    }

    /**
     * Whether the other label holds the same text.
     */
    public boolean hasSameTextAs(PresetFurniLabel other) {
        return other != null && Objects.equals(text, other.text);
    }
}
