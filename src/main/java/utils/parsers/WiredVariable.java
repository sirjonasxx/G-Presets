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


    public long getVariableId() {
        return variableId;
    }

    public String getVariableName() {
        return variableName;
    }

    public int getAvailabilityType() {
        return availabilityType;
    }

    public int getVariableType() {
        return variableType;
    }

    public boolean isAlwaysAvailable() {
        return alwaysAvailable;
    }

    public boolean isCanCreateAndDelete() {
        return canCreateAndDelete;
    }

    public boolean isHasValue() {
        return hasValue;
    }

    public boolean isCanWriteValue() {
        return canWriteValue;
    }

    public boolean isCanInterceptChanges() {
        return canInterceptChanges;
    }

    public boolean isInvisible() {
        return isInvisible;
    }

    public boolean isCanReadCreationTime() {
        return canReadCreationTime;
    }

    public boolean isCanReadLastUpdateTime() {
        return canReadLastUpdateTime;
    }

    public HashMap<Integer, String> getTextConnector() {
        return textConnector;
    }
}
