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

    protected List<Integer> pickedFurniSources; // set in subclass
    protected List<Integer> pickedUserSources;
    protected List<Long> variableIds;

    public PresetWiredBase(HPacket packet) {
        wiredId = packet.readInteger();
        options = Utils.readIntList(packet);
        stringConfig = packet.readString();
        items = Utils.readIntList(packet);
        readTypeSpecific(packet);
        pickedFurniSources = Utils.readIntList(packet);
        pickedUserSources = Utils.readIntList(packet);
        variableIds = Utils.readLongList(packet);
    }

    // deep copy constructor
    public PresetWiredBase(PresetWiredBase base) {
        this.wiredId = base.getWiredId();
        this.options = new ArrayList<>(base.options);
        this.stringConfig = base.stringConfig;
        this.items = new ArrayList<>(base.items);
        this.pickedFurniSources = new ArrayList<>(base.pickedFurniSources);
        this.pickedUserSources = new ArrayList<>(base.pickedUserSources);
        this.variableIds = new ArrayList<>(base.variableIds);
    }

    public PresetWiredBase(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<Long> variableIds) {
        this.wiredId = wiredId;
        this.options = options;
        this.stringConfig = stringConfig;
        this.items = items;
        this.pickedFurniSources = pickedFurniSources;
        this.pickedUserSources = pickedUserSources;
        this.variableIds = variableIds;
    }

    public PresetWiredBase(JSONObject object) {
        wiredId = object.getInt("wiredId");
        options = object.getJSONArray("options").toList().stream().map(o -> (int)o).collect(Collectors.toList());
        stringConfig = object.getString("config");
        items = object.getJSONArray("items").toList().stream().map(o -> (int)o).collect(Collectors.toList());
        pickedFurniSources = object.has("furniSources") ?
                object.getJSONArray("furniSources").toList().stream().map(o -> (int)o).collect(Collectors.toList()) :
                Collections.emptyList();
        pickedUserSources = object.has("userSources") ?
                object.getJSONArray("userSources").toList().stream().map(o -> (int)o).collect(Collectors.toList()) :
                Collections.emptyList();
        variableIds = object.has("variableIds") ?
                object.getJSONArray("variableIds").toList().stream().map(o -> {
                    if(o instanceof Integer) {
                        return new Long((int)o);
                    }
                    return (long)o;
                }).collect(Collectors.toList()) :
                Collections.emptyList();
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();
        object.put("wiredId", wiredId);
        object.put("options", options);
        object.put("config", stringConfig);
        object.put("items", items);
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
        variableIds.forEach(packet::appendLong);

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

    public <T extends PresetWiredBase> T applyWiredConfig(IExtension extension, Map<Integer, Integer> realFurniIdMap, Map<Long, Long> realVariableIdMap) {
        if (realFurniIdMap.containsKey(wiredId)) {
            PresetWiredBase presetWiredBase = this.clone();
            presetWiredBase.wiredId = realFurniIdMap.get(wiredId);
            presetWiredBase.items = items.stream()
                    .filter(realFurniIdMap::containsKey)
                    .map(realFurniIdMap::get)
                    .collect(Collectors.toList());
            presetWiredBase.variableIds = variableIds.stream()
                    .map(id -> id < 0 ? id : realVariableIdMap.getOrDefault(id, this instanceof PresetWiredVariable && this.variableIds.size() == 1 ? this.variableIds.get(0) : 0L))
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

    public List<Integer> getPickedFurniSources() {
        return pickedFurniSources;
    }

    public List<Integer> getPickedUserSources() {
        return pickedUserSources;
    }

    public List<Long> getVariableIds() {
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

    public void setVariableIds(List<Long> variableIds) {
        this.variableIds = variableIds;
    }

    @Override
    protected abstract PresetWiredBase clone();
}
