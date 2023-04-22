package extension.tools.presetconfig.wired;

import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetWiredEffect extends PresetWiredBase {

    private int delay;
    private int stuff;

    public PresetWiredEffect(HPacket packet) {
        super(packet);
        delay = packet.readInteger();
    }

    public PresetWiredEffect(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int delay) {
        super(wiredId, options, stringConfig, items);
        this.delay = delay;
    }

    // deep copy constructor
    public PresetWiredEffect(PresetWiredEffect effect) {
        super(effect);
        this.delay = effect.delay;
    }

    public PresetWiredEffect(JSONObject object) {
        super(object);
        this.delay = object.getInt("delay");
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
        object.put("delay", delay);
    }

    @Override
    public void applyWiredConfig(IExtension extension) {
        HPacket packet = new HPacket(
                "UpdateAction",
                HMessage.Direction.TOSERVER,
                wiredId
        );
        packet.appendInt(options.size());
        options.forEach(packet::appendInt);
        packet.appendString(stringConfig);
        packet.appendInt(items.size());
        items.forEach(packet::appendInt);
        packet.appendInt(delay);

        packet.appendInt(0); // todo
        packet.appendInt(0);

        extension.sendToServer(packet);
    }

    @Override
    public PresetWiredEffect applyWiredConfig(IExtension extension, Map<Integer, Integer> realFurniIdMap) {
        if (realFurniIdMap.containsKey(wiredId)) {
            PresetWiredEffect presetWiredEffect = new PresetWiredEffect(
                    realFurniIdMap.get(wiredId),
                    options,
                    stringConfig,
                    items.stream().filter(realFurniIdMap::containsKey)
                            .map(realFurniIdMap::get).collect(Collectors.toList()),
                    delay
            );
            presetWiredEffect.applyWiredConfig(extension);
            return presetWiredEffect;
        }
        return null;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

}
