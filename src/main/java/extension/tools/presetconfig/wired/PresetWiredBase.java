package extension.tools.presetconfig.wired;

import extension.tools.presetconfig.PresetJsonConfigurable;
import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import org.json.JSONObject;
import utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class PresetWiredBase implements PresetJsonConfigurable, Cloneable {

    protected int wiredId;
    protected List<Integer> options;
    protected String stringConfig;
    protected List<Integer> items;
    protected List<Integer> items2;

    protected List<Integer> pickedFurniSources; // set in subclass
    protected List<Integer> pickedUserSources;
    protected List<String> variableIds;

    public PresetWiredBase(HPacket packet) {
        wiredId = packet.readInteger();
        options = Utils.readIntList(packet);
        stringConfig = packet.readString();
        items = Utils.readIntList(packet);
        readTypeSpecific(packet);
        pickedFurniSources = Utils.readIntList(packet);
        pickedUserSources = Utils.readIntList(packet);
        variableIds = Utils.readStringList(packet);
        items2 = Utils.readIntList(packet);
    }

    // deep copy constructor
    public PresetWiredBase(PresetWiredBase base) {
        this.wiredId = base.getWiredId();
        this.options = new ArrayList<>(base.options);
        this.stringConfig = base.stringConfig;
        this.items = new ArrayList<>(base.items);
        this.items2 = new ArrayList<>(base.items2);
        this.pickedFurniSources = new ArrayList<>(base.pickedFurniSources);
        this.pickedUserSources = new ArrayList<>(base.pickedUserSources);
        this.variableIds = new ArrayList<>(base.variableIds);
    }

    public PresetWiredBase(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> items2, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<String> variableIds) {
        this.wiredId = wiredId;
        this.options = options;
        this.stringConfig = stringConfig;
        this.items = items;
        this.items2 = items2;
        this.pickedFurniSources = pickedFurniSources;
        this.pickedUserSources = pickedUserSources;
        this.variableIds = variableIds;
    }

    public PresetWiredBase(JSONObject object) {
        wiredId = object.getInt("wiredId");
        options = object.getJSONArray("options").toList().stream().map(o -> (int)o).collect(Collectors.toList());
        stringConfig = object.getString("config");
        items = object.getJSONArray("items").toList().stream().map(o -> (int)o).collect(Collectors.toList());
        items2 = object.has("secondItems") ?
                object.getJSONArray("secondItems").toList().stream().map(o -> (int)o).collect(Collectors.toList()) :
                Collections.emptyList();
        pickedFurniSources = object.has("furniSources") ?
                object.getJSONArray("furniSources").toList().stream().map(o -> (int)o).collect(Collectors.toList()) :
                Collections.emptyList();
        pickedUserSources = object.has("userSources") ?
                object.getJSONArray("userSources").toList().stream().map(o -> (int)o).collect(Collectors.toList()) :
                Collections.emptyList();
        variableIds = object.has("variableIds") ?
                object.getJSONArray("variableIds").toList().stream().map(o -> (String)o).collect(Collectors.toList()) :
                Collections.emptyList();
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();
        object.put("wiredId", wiredId);
        object.put("options", options);
        object.put("config", stringConfig);
        object.put("items", items);
        object.put("secondItems", items);
        object.put("furniSources", pickedFurniSources);
        object.put("userSources", pickedUserSources);
        object.put("variableIds", variableIds);

        appendJsonFields(object);
        return object;
    }

    protected abstract void readTypeSpecific(HPacket packet);

    protected abstract void appendJsonFields(JSONObject object);

    protected abstract String getPacketName();

    public void applyWiredConfig(IExtension extension) {
        HPacket packet = new HPacket(
                getPacketName(),
                HMessage.Direction.TOSERVER,
                wiredId
        );
        packet.appendInt(options.size());
        options.forEach(packet::appendInt);
        packet.appendString(stringConfig);
        packet.appendInt(items.size());
        items.forEach(packet::appendInt);

        applyTypeSpecificWiredConfig(packet);

        packet.appendInt(pickedFurniSources.size());
        pickedFurniSources.forEach(packet::appendInt);
        packet.appendInt(pickedUserSources.size());
        pickedUserSources.forEach(packet::appendInt);

        packet.appendInt(variableIds.size());
        variableIds.forEach(packet::appendString);

        packet.appendInt(items2.size());
        items2.forEach(packet::appendInt);

        if (this instanceof PresetWiredTrigger) {
            new PresetWiredTrigger(packet);
        } else if (this instanceof PresetWiredSelector) {
            new PresetWiredSelector(packet);
        } else if (this instanceof PresetWiredEffect) {
            new PresetWiredEffect(packet);
        } else if (this instanceof PresetWiredAddon) {
            new PresetWiredAddon(packet);
        } else if (this instanceof PresetWiredCondition) {
            new PresetWiredCondition(packet);
        } else if (this instanceof PresetWiredVariable) {
            new PresetWiredVariable(packet);
        }

        extension.sendToServer(packet);
    }

    protected abstract void applyTypeSpecificWiredConfig(HPacket packet);

    public <T extends PresetWiredBase> T applyWiredConfig(IExtension extension, Map<Integer, Integer> realFurniIdMap, Map<String, String> realVariableIdMap) {
        if (realFurniIdMap.containsKey(wiredId)) {
            PresetWiredBase presetWiredBase = this.clone();
            presetWiredBase.wiredId = realFurniIdMap.get(wiredId);
            presetWiredBase.items = items.stream()
                    .filter(realFurniIdMap::containsKey)
                    .map(realFurniIdMap::get)
                    .collect(Collectors.toList());
            presetWiredBase.items2 = items2.stream()
                    .filter(realFurniIdMap::containsKey)
                    .map(realFurniIdMap::get)
                    .collect(Collectors.toList());

            String _defaultId = "0";
            // Variables from another room
            if(this instanceof PresetWiredVariable && this.variableIds.size() == 1) {
                _defaultId = this.variableIds.get(0);
                realVariableIdMap.put(_defaultId, _defaultId);
            }

            String defaultId = _defaultId;

            presetWiredBase.variableIds = variableIds.stream()
                    .map(id -> id.equals("0") || id.startsWith("-") ? id : realVariableIdMap.getOrDefault(id, defaultId))
                    .collect(Collectors.toList());

            presetWiredBase.applyWiredConfig(extension);
            return (T) presetWiredBase;
        }
        return null;
    }

    public int getWiredId() {
        return wiredId;
    }

    public List<Integer> getOptions() {
        return options;
    }

    public String getStringConfig() {
        return stringConfig;
    }

    public List<Integer> getItems() {
        return items;
    }

    public List<Integer> getSecondItems() {
        return items2;
    }

    public List<Integer> getPickedFurniSources() {
        return pickedFurniSources;
    }

    public List<Integer> getPickedUserSources() {
        return pickedUserSources;
    }

    public List<String> getVariableIds() {
        return variableIds;
    }

    public void setWiredId(int wiredId) {
        this.wiredId = wiredId;
    }

    public void setOptions(List<Integer> options) {
        this.options = options;
    }

    public void setStringConfig(String stringConfig) {
        this.stringConfig = stringConfig;
    }

    public void setItems(List<Integer> items) {
        this.items = items;
    }

    public void setPickedFurniSources(List<Integer> pickedFurniSources) {
        this.pickedFurniSources = pickedFurniSources;
    }

    public void setPickedUserSources(List<Integer> pickedUserSources) {
        this.pickedUserSources = pickedUserSources;
    }

    public void setVariableIds(List<String> variableIds) {
        this.variableIds = variableIds;
    }

    @Override
    protected abstract PresetWiredBase clone();
}
