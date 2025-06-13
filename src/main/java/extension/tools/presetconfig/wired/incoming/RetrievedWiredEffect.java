package extension.tools.presetconfig.wired.incoming;

import extension.parsers.HWiredContext;
import extension.tools.presetconfig.wired.PresetWiredEffect;
import gearth.protocol.HPacket;

import java.util.List;

public class RetrievedWiredEffect extends PresetWiredEffect implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredEffect(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, List<Integer> secondItems, int delay, int typeId, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<String> variableIds, HWiredContext wiredContext) {
        super(wiredId, options, stringConfig, items, secondItems, delay, pickedFurniSources, pickedUserSources, variableIds);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredEffect fromPacket(HPacket packet) {
        return RetrievedWired.fromPacket(packet, RetrievedWiredEffect.class);
    }
}
