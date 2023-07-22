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

        List<Integer> furniSources = Utils.readIntList(packet);
        List<Integer> userSources = Utils.readIntList(packet);

        if (cls == RetrievedWiredAddon.class) {
            return cls.cast(new RetrievedWiredAddon(wiredId, options, configString, items, typeId, furniSources, userSources));
        } else if (cls == RetrievedWiredTrigger.class) {
            return cls.cast(new RetrievedWiredTrigger(wiredId, options, configString, items, typeId, furniSources, userSources));
        } else if (cls == RetrievedWiredEffect.class) {
            int delay = packet.readInteger();
            return cls.cast(new RetrievedWiredEffect(wiredId, options, configString, items, delay, typeId, furniSources, userSources));
        } else if (cls == RetrievedWiredCondition.class) {
            packet.readInteger();
            int quantifier = packet.readInteger();
            return cls.cast(new RetrievedWiredCondition(wiredId, options, configString, items, typeId, quantifier, furniSources, userSources));
        } else if (cls == RetrievedWiredSelector.class) {
            packet.readInteger();
            boolean filter = packet.readBoolean();
            boolean invert = packet.readBoolean();
            return cls.cast(new RetrievedWiredSelector(wiredId, options, configString, items, typeId, filter, invert, furniSources, userSources));
        }

        return null;
    }

}
