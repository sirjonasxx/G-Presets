package extension.tools.presetconfig.wired;

import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import org.json.JSONObject;
import utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetWiredAddon extends PresetWiredBase {

    private int stuff;

    public PresetWiredAddon(HPacket packet) {
        super(packet);
        pickedFurniSources = Utils.readIntList(packet);
        pickedUserSources = Utils.readIntList(packet);
    }

    public PresetWiredAddon(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources);
    }

    // deep copy constructor
    public PresetWiredAddon(PresetWiredAddon addon) {
        super(addon);
    }

    public PresetWiredAddon(JSONObject object) {
        super(object);
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
    }

    @Override
    public void applyWiredConfig(IExtension extension) {
        HPacket packet = new HPacket(
                "UpdateAddon",
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
    public PresetWiredAddon applyWiredConfig(IExtension extension, Map<Integer, Integer> realFurniIdMap) {
        if (realFurniIdMap.containsKey(wiredId)) {
            PresetWiredAddon presetWiredAddon = new PresetWiredAddon(
                    realFurniIdMap.get(wiredId),
                    options,
                    stringConfig,
                    items.stream().filter(realFurniIdMap::containsKey)
                            .map(realFurniIdMap::get).collect(Collectors.toList()),
                    pickedFurniSources,
                    pickedUserSources
            );

            presetWiredAddon.applyWiredConfig(extension);
            return presetWiredAddon;
        }
        return null;
    }
}
