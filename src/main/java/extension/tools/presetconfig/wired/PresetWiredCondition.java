package extension.tools.presetconfig.wired;

import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.List;

public class PresetWiredCondition extends PresetWiredBase {

    private int quantifier;

    public PresetWiredCondition(HPacket packet) {
        super(packet);
    }

    public PresetWiredCondition(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int quantifier, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources);
        this.quantifier = quantifier;
    }

    // deep copy constructor
    public PresetWiredCondition(PresetWiredCondition condition) {
        super(condition);
        this.quantifier = condition.quantifier;
    }

    public PresetWiredCondition(JSONObject object) {
        super(object);
        this.quantifier = object.optInt("quantifier");
    }

    @Override
    protected void readTypeSpecific(HPacket packet) {
        quantifier = packet.readInteger();
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
        object.put("quantifier", quantifier);
    }

    @Override
    protected String getPacketName() {
        return "UpdateCondition";
    }

    @Override
    protected void applyTypeSpecificWiredConfig(HPacket packet) {
        packet.appendInt(quantifier);
    }

    @Override
    public PresetWiredCondition clone() {
        return new PresetWiredCondition(this);
    }

}
