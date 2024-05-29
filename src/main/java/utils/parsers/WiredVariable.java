package utils.parsers;

import gearth.protocol.HPacket;

import java.util.HashMap;

public class WiredVariable {
    private long variableId;
    private String variableName;
    private int availabilityType;
    private int variableType;
    private boolean alwaysAvailable;
    private boolean canCreateAndDelete;
    private boolean hasValue;
    private boolean canWriteValue;
    private boolean canInterceptChanges;
    private boolean isInvisible;
    private boolean canReadCreationTime;
    private boolean canReadLastUpdateTime;
    private HashMap<Integer, String> textConnector;

    public WiredVariable(HPacket packet) {
        variableId = packet.readLong();
        variableName = packet.readString();
        availabilityType = packet.readInteger();
        variableType = packet.readInteger();
        alwaysAvailable = packet.readBoolean();
        canCreateAndDelete = packet.readBoolean();
        hasValue = packet.readBoolean();
        canWriteValue = packet.readBoolean();
        canInterceptChanges = packet.readBoolean();
        isInvisible = packet.readBoolean();
        canReadCreationTime = packet.readBoolean();
        canReadLastUpdateTime = packet.readBoolean();
        boolean hasTextConnector = packet.readBoolean();
        if (hasTextConnector) {
            int count = packet.readInteger();
            textConnector = new HashMap<>();
            for (int i = 0; i < count; i++) {
                textConnector.put(packet.readInteger(), packet.readString());
            }
        }
    }
}
