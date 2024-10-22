package extension.tools.presetconfig.wired.incoming;

import gearth.protocol.HPacket;
import utils.Utils;

import java.util.List;

public interface RetrievedWired {

    int getTypeId();

    static <T extends RetrievedWired> T fromPacket(HPacket packet, Class<T> cls) {
        packet.readInteger(); // selection limit

        List<Integer> items = Utils.readIntList(packet);

        int typeId = packet.readInteger(); // typeid
        int wiredId = packet.readInteger(); // furni id

        String configString = packet.readString();
        List<Integer> options = Utils.readIntList(packet);

        List<Long> variableIds = Utils.readLongList(packet);

        List<Integer> furniSources = Utils.readIntList(packet);
        List<Integer> userSources = Utils.readIntList(packet);

        packet.readInteger(); // code

        if (cls == RetrievedWiredAddon.class) {
            return cls.cast(new RetrievedWiredAddon(wiredId, options, configString, items, typeId, furniSources, userSources, variableIds));
        } else if (cls == RetrievedWiredTrigger.class) {
            return cls.cast(new RetrievedWiredTrigger(wiredId, options, configString, items, typeId, furniSources, userSources, variableIds));
        } else if (cls == RetrievedWiredEffect.class) {
            int delay = packet.readInteger();
            return cls.cast(new RetrievedWiredEffect(wiredId, options, configString, items, delay, typeId, furniSources, userSources, variableIds));
        } else if (cls == RetrievedWiredCondition.class) {
            int quantifier = packet.readInteger();
            return cls.cast(new RetrievedWiredCondition(wiredId, options, configString, items, typeId, quantifier, furniSources, userSources, variableIds));
        } else if (cls == RetrievedWiredSelector.class) {
            boolean filter = packet.readBoolean();
            boolean invert = packet.readBoolean();
            return cls.cast(new RetrievedWiredSelector(wiredId, options, configString, items, typeId, filter, invert, furniSources, userSources, variableIds));
        } else if (cls == RetrievedWiredVariable.class) {
            return cls.cast(new RetrievedWiredVariable(wiredId, options, configString, items, typeId, furniSources, userSources, variableIds));
        }

        return null;
    }

}
