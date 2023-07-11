package extension.tools.presetconfig.wired;

import extension.tools.presetconfig.PresetJsonConfigurable;
import gearth.extensions.IExtension;
import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class PresetWiredBase implements PresetJsonConfigurable {

    protected int wiredId;
    protected List<Integer> options;
    protected String stringConfig;
    protected List<Integer> items;

    protected List<Integer> pickedFurniSources; // set in subclass
    protected List<Integer> pickedUserSources;

    public PresetWiredBase(HPacket packet) {
        wiredId = packet.readInteger();

        int optionsCount = packet.readInteger();
        options = new ArrayList<>();
        for (int i = 0; i < optionsCount; i++) {
            options.add(packet.readInteger());
        }

        stringConfig = packet.readString();

        int itemsCount = packet.readInteger();
        items = new ArrayList<>();
        for (int i = 0; i < itemsCount; i++) {
            items.add(packet.readInteger());
        }
    }

    // deep copy constructor
    public PresetWiredBase(PresetWiredBase base) {
        this.wiredId = base.getWiredId();
        this.options = new ArrayList<>(base.options);
        this.stringConfig = base.stringConfig;
        this.items = new ArrayList<>(base.items);
        this.pickedFurniSources = new ArrayList<>(base.pickedFurniSources);
        this.pickedUserSources = new ArrayList<>(base.pickedUserSources);
    }

    public PresetWiredBase(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        this.wiredId = wiredId;
        this.options = options;
        this.stringConfig = stringConfig;
        this.items = items;
        this.pickedFurniSources = pickedFurniSources;
        this.pickedUserSources = pickedUserSources;
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

        appendJsonFields(object);
        return object;
    }

    protected abstract void appendJsonFields(JSONObject object);

    public abstract void applyWiredConfig(IExtension extension);

    public abstract PresetWiredBase applyWiredConfig(IExtension extension, Map<Integer, Integer> realFurniIdMap);

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

    public List<Integer> getPickedFurniSources() {
        return pickedFurniSources;
    }

    public void setPickedFurniSources(List<Integer> pickedFurniSources) {
        this.pickedFurniSources = pickedFurniSources;
    }

    public List<Integer> getPickedUserSources() {
        return pickedUserSources;
    }

    public void setPickedUserSources(List<Integer> pickedUserSources) {
        this.pickedUserSources = pickedUserSources;
    }
}
