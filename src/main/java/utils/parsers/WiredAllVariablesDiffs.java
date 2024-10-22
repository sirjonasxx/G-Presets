package utils.parsers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WiredAllVariablesDiffs {
    private int allVariablesHash = 0;
    private boolean lastChunk = false;
    private final List<Long> removedVariables = new ArrayList<>();
    private final Map<Integer, WiredVariable> addedVariables = new HashMap<>();

    public WiredAllVariablesDiffs(HPacket packet) {
        allVariablesHash = packet.readInteger();
        lastChunk = packet.readBoolean();
        int removedCount = packet.readInteger();
        for (int i = 0; i < removedCount; i++) {
            removedVariables.add(packet.readLong());
        }

        int addedOrUpdatedCount = packet.readInteger();
        for (int i = 0; i < addedOrUpdatedCount; i++) {
            int hash = packet.readInteger();
            addedVariables.put(hash, new WiredVariable(packet));
        }
    }

    public int getAllVariablesHash() {
        return allVariablesHash;
    }

    public boolean isLastChunk() {
        return lastChunk;
    }

    public List<Long> getRemovedVariables() {
        return removedVariables;
    }

    public Map<Integer, WiredVariable> getAddedVariables() {
        return addedVariables;
    }
}
