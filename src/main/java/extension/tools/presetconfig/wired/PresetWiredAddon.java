package extension.tools.presetconfig.wired;

import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.List;

public class PresetWiredAddon extends PresetWiredBase {

    public PresetWiredAddon(HPacket packet) {
        super(packet);
    }

    public PresetWiredAddon(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<Long> variableIds) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources, variableIds);
    }

    // deep copy constructor
    public PresetWiredAddon(PresetWiredAddon addon) {
        super(addon);
    }

    public PresetWiredAddon(JSONObject object) {
        super(object);
    }

    @Override
    protected void readTypeSpecific(HPacket packet) {
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
    }

    @Override
    protected String getPacketName() {
        return "UpdateAddon";
    }

    @Override
    protected void applyTypeSpecificWiredConfig(HPacket packet) {
    }

    @Override
    public PresetWiredAddon clone() {
        return new PresetWiredAddon(this);
    }
}
