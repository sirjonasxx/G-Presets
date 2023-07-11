package extension.tools.presetconfig.wired;

import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import org.json.JSONObject;
import utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetWiredSelector extends PresetWiredBase {

    private boolean filter;
    private boolean invert;

    public PresetWiredSelector(HPacket packet) {
        super(packet);
        filter = packet.readBoolean();
        invert = packet.readBoolean();
        pickedFurniSources = Utils.readIntList(packet);
        pickedUserSources = Utils.readIntList(packet);
    }

    public PresetWiredSelector(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, boolean filter, boolean invert, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources);
        this.filter = filter;
        this.invert = invert;
    }

    // deep copy constructor
    public PresetWiredSelector(PresetWiredSelector selector) {
        super(selector);
        this.filter = selector.filter;
        this.invert = selector.invert;
    }

    public PresetWiredSelector(JSONObject object) {
        super(object);
        this.filter = object.getBoolean("filter");
        this.invert = object.getBoolean("invert");
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
        object.put("filter", filter);
        object.put("invert", invert);
    }

    @Override
    public void applyWiredConfig(IExtension extension) {
        HPacket packet = new HPacket(
                "UpdateSelector",
                HMessage.Direction.TOSERVER,
                wiredId
        );
        packet.appendInt(options.size());
        options.forEach(packet::appendInt);
        packet.appendString(stringConfig);
        packet.appendInt(items.size());
        items.forEach(packet::appendInt);

        packet.appendBoolean(filter);
        packet.appendBoolean(invert);
        packet.appendInt(pickedFurniSources.size());
        pickedFurniSources.forEach(packet::appendInt);
        packet.appendInt(pickedUserSources.size());
        pickedUserSources.forEach(packet::appendInt);

        extension.sendToServer(packet);
    }

    @Override
    public PresetWiredSelector applyWiredConfig(IExtension extension, Map<Integer, Integer> realFurniIdMap) {
        if (realFurniIdMap.containsKey(wiredId)) {
            PresetWiredSelector presetWiredSelector = new PresetWiredSelector(
                    realFurniIdMap.get(wiredId),
                    options,
                    stringConfig,
                    items.stream().filter(realFurniIdMap::containsKey)
                            .map(realFurniIdMap::get).collect(Collectors.toList()),
                    filter,
                    invert,
                    pickedFurniSources,
                    pickedUserSources
            );

            presetWiredSelector.applyWiredConfig(extension);
            return presetWiredSelector;
        }
        return null;
    }

}
