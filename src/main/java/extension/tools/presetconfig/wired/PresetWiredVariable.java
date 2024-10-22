package extension.tools.presetconfig.wired;

import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.List;

public class PresetWiredVariable extends PresetWiredBase {
    protected Long variableId;

    public PresetWiredVariable(HPacket packet) {
        super(packet);
    }

    public PresetWiredVariable(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<Long> variableIds) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources, variableIds);
    }

    public PresetWiredVariable(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<Long> variableIds, Long variableId) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources, variableIds);
        this.variableId = variableId;
    }

    // deep copy constructor
    public PresetWiredVariable(PresetWiredVariable variable) {
        super(variable);
        this.variableId = variable.variableId;
    }

    public PresetWiredVariable(JSONObject object) {
        super(object);
        this.variableId = object.has("variableId") && !object.isNull("variableId") ? object.getLong("variableId") : null;
    }

    @Override
    protected void readTypeSpecific(HPacket packet) {
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
        object.put("variableId", variableId);
    }

    @Override
    protected String getPacketName() {
        return "UpdateVariable";
    }

    @Override
    protected void applyTypeSpecificWiredConfig(HPacket packet) {
    }

    @Override
    public PresetWiredVariable clone() {
        return new PresetWiredVariable(this);
    }

    public Long getVariableId() {
        return variableId;
    }

    public void setVariableId(Long variableId) {
        this.variableId = variableId;
    }

}
