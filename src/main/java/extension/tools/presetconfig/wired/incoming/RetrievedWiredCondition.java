package extension.tools.presetconfig.wired.incoming;

import extension.parsers.HWiredContext;
import extension.tools.presetconfig.wired.PresetWiredCondition;
import gearth.protocol.HPacket;

import java.util.List;

public class RetrievedWiredCondition extends PresetWiredCondition implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredCondition(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int typeId, int quantifier, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<String> variableIds, HWiredContext wiredContext) {
        super(wiredId, options, stringConfig, items, quantifier, pickedFurniSources, pickedUserSources, variableIds);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredCondition fromPacket(HPacket packet) {
        return RetrievedWired.fromPacket(packet, RetrievedWiredCondition.class);
    }
}
