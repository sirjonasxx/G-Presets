package extension.tools.presetconfig.wired;

import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.List;

public class PresetWiredTrigger extends PresetWiredBase {

    public PresetWiredTrigger(HPacket packet) {
        super(packet);
    }

    public PresetWiredTrigger(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> secondItems, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<String> variableIds) {
        super(wiredId, options, stringConfig, items, secondItems, pickedFurniSources, pickedUserSources, variableIds);
    }

    // deep copy constructor
    public PresetWiredTrigger(PresetWiredTrigger trigger) {
        super(trigger);
    }

    public PresetWiredTrigger(JSONObject object) {
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
        return "UpdateTrigger";
    }

    @Override
    protected void applyTypeSpecificWiredConfig(HPacket packet) {
    }

    @Override
    public PresetWiredTrigger clone() {
        return new PresetWiredTrigger(this);
    }

}
