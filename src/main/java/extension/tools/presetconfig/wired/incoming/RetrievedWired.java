package extension.tools.presetconfig.wired.incoming;

import extension.parsers.HWiredContext;
import gearth.protocol.HPacket;
import utils.Utils;

import java.util.List;

public interface RetrievedWired {

    class DefinitionSpecifics {

        public int quantifier;
        public int delay;
        public boolean filter;
        public boolean invert;
        public int quantifierType;
        public boolean isInvert;
    }

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

        // readDefinitionSpecifics
        DefinitionSpecifics specifics = new DefinitionSpecifics();

        if (cls == RetrievedWiredEffect.class) {
            specifics.delay = packet.readInteger();
        }
        else if (cls == RetrievedWiredCondition.class) {
            specifics.quantifier = packet.readInteger();
        }
        else if (cls == RetrievedWiredSelector.class) {
            specifics.filter = packet.readBoolean();
            specifics.invert = packet.readBoolean();
        }

        boolean advancedMode = packet.readBoolean();

        // InputSourcesConf
        readAllowedSources(packet);
        readAllowedSources(packet);
        readDefaultSources(packet);
        readDefaultSources(packet);

        boolean allowWallFurni = packet.readBoolean();

        // readTypeSpecifics
        if (cls == RetrievedWiredCondition.class) {
            specifics.quantifierType = packet.readInteger();
            specifics.isInvert = packet.readBoolean();
        }

        HWiredContext wiredContext = null;

        if(packet.isEOF() == 0) {
            // WiredContext
            wiredContext = new HWiredContext(packet);
        }

        if (cls == RetrievedWiredAddon.class) {
            return cls.cast(new RetrievedWiredAddon(wiredId, options, configString, items, typeId, furniSources, userSources, variableIds, wiredContext));
        } else if (cls == RetrievedWiredTrigger.class) {
            return cls.cast(new RetrievedWiredTrigger(wiredId, options, configString, items, typeId, furniSources, userSources, variableIds, wiredContext));
        } else if (cls == RetrievedWiredEffect.class) {
            return cls.cast(new RetrievedWiredEffect(wiredId, options, configString, items, specifics.delay, typeId, furniSources, userSources, variableIds, wiredContext));
        } else if (cls == RetrievedWiredCondition.class) {
            return cls.cast(new RetrievedWiredCondition(wiredId, options, configString, items, typeId, specifics.quantifier, furniSources, userSources, variableIds, wiredContext));
        } else if (cls == RetrievedWiredSelector.class) {
            return cls.cast(new RetrievedWiredSelector(wiredId, options, configString, items, typeId, specifics.filter, specifics.invert, furniSources, userSources, variableIds, wiredContext));
        } else if (cls == RetrievedWiredVariable.class) {
            return cls.cast(new RetrievedWiredVariable(wiredId, options, configString, items, typeId, furniSources, userSources, variableIds, wiredContext));
        }

        return null;
    }

    static void readAllowedSources(HPacket packet) {
        int count = packet.readInteger();
        for(int i = 0; i < count; i++) {
            Utils.readIntList(packet);
        }
    }
    static void readDefaultSources(HPacket packet) {
        Utils.readIntList(packet);
    }

}
