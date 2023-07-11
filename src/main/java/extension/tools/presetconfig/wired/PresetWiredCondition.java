package extension.tools.presetconfig.wired;

import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import org.json.JSONObject;
import utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetWiredCondition extends PresetWiredBase {

    private int quantifier;

    public PresetWiredCondition(HPacket packet) {
        super(packet);
        quantifier = packet.readInteger();
        pickedFurniSources = Utils.readIntList(packet);
        pickedUserSources = Utils.readIntList(packet);
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
        this.quantifier = object.has("quantifier") ? object.getInt("quantifier") : 0;
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
        object.put("quantifier", quantifier);
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

        packet.appendInt(quantifier);
        packet.appendInt(pickedFurniSources.size());
        pickedFurniSources.forEach(packet::appendInt);
        packet.appendInt(pickedUserSources.size());
        pickedUserSources.forEach(packet::appendInt);

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
                            .map(realFurniIdMap::get).collect(Collectors.toList()),
                    quantifier,
                    pickedFurniSources,
                    pickedUserSources
            );

            presetWiredCondition.applyWiredConfig(extension);
            return presetWiredCondition;
        }
        return null;
    }

}
