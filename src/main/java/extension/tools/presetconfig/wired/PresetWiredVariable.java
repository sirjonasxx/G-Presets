package extension.tools.presetconfig.wired;

import extension.parsers.HSharedVariable;
import extension.parsers.HWiredContext;
import extension.parsers.HWiredVariable;
import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Beny.
 */
public class PresetWiredVariable extends PresetWiredBase {

    public String variableId;

    public PresetWiredVariable(HPacket packet) {
        super(packet);
    }

    public PresetWiredVariable(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> secondItems, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<String> variableIds, HWiredContext wiredContext) {
        super(wiredId, options, stringConfig, items, secondItems, pickedFurniSources, pickedUserSources, variableIds);
        this.variableId = "";

        if(wiredContext != null) {
            if(wiredContext.roomVariablesList != null && !wiredContext.roomVariablesList.variables.isEmpty()) {
                for(HWiredVariable var : wiredContext.roomVariablesList.variables.values()) {
                    if(var.name.equals(stringConfig)) {
                        this.variableId = var.id;
                    }
                }
            }
            else if(wiredContext.furniVariableInfo != null && wiredContext.furniVariableInfo.variable.name.equals(stringConfig)) {
                this.variableId = wiredContext.furniVariableInfo.variable.id;
            }
            else if(wiredContext.userVariableInfo != null && wiredContext.userVariableInfo.variable.name.equals(stringConfig)) {
                this.variableId = wiredContext.userVariableInfo.variable.id;
            }
            else if(wiredContext.globalVariableInfo != null && wiredContext.globalVariableInfo.variable.name.equals(stringConfig)) {
                this.variableId = wiredContext.globalVariableInfo.variable.id;
            }
            else if(wiredContext.referenceVariablesList != null && !wiredContext.referenceVariablesList.sharedVariables.isEmpty()) {
                for(HSharedVariable var : wiredContext.referenceVariablesList.sharedVariables) {
                    if(var.wiredVariable.name.equals(stringConfig)) {
                        this.variableId = var.wiredVariable.id;
                    }
                }
            }
        }
    }

    // deep copy constructor
    public PresetWiredVariable(PresetWiredVariable variable) {
        super(variable);
        this.variableId = variable.variableId;
    }

    public PresetWiredVariable(JSONObject object) {
        super(object);

        try {
            this.variableId = object.optString("variableId");
        }
        catch(Exception e) {
            try {
                this.variableId = object.optString("variableId");
            }
            catch(Exception ex) {
                this.variableId = (String)object.get("variableId");
            }
        }
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
}
