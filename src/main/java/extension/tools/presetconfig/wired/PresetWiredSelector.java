package extension.tools.presetconfig.wired;

import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.List;

public class PresetWiredSelector extends PresetWiredBase {

    private boolean filter;
    private boolean inverse;

    public PresetWiredSelector(HPacket packet) {
        super(packet);
    }

    public PresetWiredSelector(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, boolean filter, boolean inverse, List<Integer> furniSources, List<Integer> userSources) {
        super(wiredId, options, stringConfig, items, furniSources, userSources);
        this.filter = filter;
        this.inverse = inverse;
    }

    // deep copy constructor
    public PresetWiredSelector(PresetWiredSelector selector) {
        super(selector);
        this.filter = selector.filter;
        this.inverse = selector.inverse;
    }

    public PresetWiredSelector(JSONObject object) {
        super(object);
        this.filter = object.optBoolean("filter");
        this.inverse = object.optBoolean("inverse");
    }

    @Override
    protected void readTypeSpecific(HPacket packet) {
        this.filter = packet.readBoolean();
        this.inverse = packet.readBoolean();
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
        object.put("filter", filter);
        object.put("inverse", inverse);
    }

    @Override
    protected String getPacketName() {
        return "UpdateSelector";
    }

    @Override
    protected void applyTypeSpecificWiredConfig(HPacket packet) {
        packet.appendBoolean(filter);
        packet.appendBoolean(inverse);
    }

    @Override
    public PresetWiredSelector clone() {
        return new PresetWiredSelector(this);
    }

}
