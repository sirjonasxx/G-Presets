package extension.tools.presetconfig.wired;

import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetWiredCondition extends PresetWiredBase {

    private int stuff;

    public PresetWiredCondition(HPacket packet) {
        super(packet);
    }

    public PresetWiredCondition(int wiredId, List<Integer> options, String stringConfig, List<Integer> items) {
        super(wiredId, options, stringConfig, items);
    }

    // deep copy constructor
    public PresetWiredCondition(PresetWiredCondition condition) {
        super(condition);
    }

    public PresetWiredCondition(JSONObject object) {
        super(object);
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
    }

    @Override
    public void applyWiredConfig(IExtension extension) {
        HPacket packet = new HPacket(
                "UpdateCondition",
                HMessage.Direction.TOSERVER,
                wiredId
        );
        packet.appendInt(options.size());
        options.forEach(packet::appendInt);
        packet.appendString(stringConfig);
        packet.appendInt(items.size());
        items.forEach(packet::appendInt);

        packet.appendInt(0); // todo
        packet.appendInt(0);
        packet.appendInt(0);

        extension.sendToServer(packet);
    }

    @Override
    public PresetWiredCondition applyWiredConfig(IExtension extension, Map<Integer, Integer> realFurniIdMap) {
        if (realFurniIdMap.containsKey(wiredId)) {
            PresetWiredCondition presetWiredCondition = new PresetWiredCondition(
                    realFurniIdMap.get(wiredId),
                    options,
                    stringConfig,
                    items.stream().filter(realFurniIdMap::containsKey)
                            .map(realFurniIdMap::get).collect(Collectors.toList())
            );

            presetWiredCondition.applyWiredConfig(extension);
            return presetWiredCondition;
        }
        return null;
    }

}
