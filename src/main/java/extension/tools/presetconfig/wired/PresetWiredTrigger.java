package extension.tools.presetconfig.wired;

import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetWiredTrigger extends PresetWiredBase {

    private int stuff;

    public PresetWiredTrigger(HPacket packet) {
        super(packet);
    }

    public PresetWiredTrigger(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources);
    }

    // deep copy constructor
    public PresetWiredTrigger(PresetWiredTrigger trigger) {
        super(trigger);
    }

    public PresetWiredTrigger(JSONObject object) {
        super(object);
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
        object.put("stuff", stuff);
    }

    @Override
    public void applyWiredConfig(IExtension extension) {
        HPacket packet = new HPacket(
                "UpdateTrigger",
                HMessage.Direction.TOSERVER,
                wiredId
        );
        packet.appendInt(options.size());
        options.forEach(packet::appendInt);
        packet.appendString(stringConfig);
        packet.appendInt(items.size());
        items.forEach(packet::appendInt);

        packet.appendInt(pickedFurniSources.size());
        pickedFurniSources.forEach(packet::appendInt);
        packet.appendInt(pickedUserSources.size());
        pickedUserSources.forEach(packet::appendInt);

        extension.sendToServer(packet);
    }

    @Override
    public PresetWiredTrigger applyWiredConfig(IExtension extension, Map<Integer, Integer> realFurniIdMap) {
        if (realFurniIdMap.containsKey(wiredId)) {
            PresetWiredTrigger presetWiredTrigger = new PresetWiredTrigger(
                    realFurniIdMap.get(wiredId),
                    options,
                    stringConfig,
                    items.stream().filter(realFurniIdMap::containsKey)
                            .map(realFurniIdMap::get).collect(Collectors.toList()),
                    pickedFurniSources,
                    pickedUserSources
            );

            presetWiredTrigger.applyWiredConfig(extension);
            return presetWiredTrigger;
        }
        return null;
    }

}
