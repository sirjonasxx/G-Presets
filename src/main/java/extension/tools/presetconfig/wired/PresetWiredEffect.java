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
        stuff = packet.readInteger();
    }

    public PresetWiredEffect(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int delay, int stuff) {
        super(wiredId, options, stringConfig, items);
        this.delay = delay;
        this.stuff = stuff;
    }

    // deep copy constructor
    public PresetWiredEffect(PresetWiredEffect effect) {
        super(effect);
        this.delay = effect.delay;
        this.stuff = effect.stuff;
    }

    public PresetWiredEffect(JSONObject object) {
        super(object);
        this.delay = object.getInt("delay");
        this.stuff = object.getInt("stuff");
    }

    @Override
    protected void appendJsonFields(JSONObject object) {
        object.put("delay", delay);
        object.put("stuff", stuff);
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
        packet.appendInt(stuff);

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
                    delay,
                    stuff
            );
            presetWiredEffect.applyWiredConfig(extension);
            return presetWiredEffect;
        }
        return null;
    }

    public int getDelay() {
        return delay;
    }

    public int getStuff() {
        return stuff;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setStuff(int stuff) {
        this.stuff = stuff;
    }

}
