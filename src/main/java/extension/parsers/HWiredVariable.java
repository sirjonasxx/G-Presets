package extension.parsers;

import gearth.protocol.HPacket;
import java.util.HashMap;

public class HWiredVariable {
    public final String id;
    public final VariableInternalType variableInternalType;
    public final String name;
    public final int availabilityType;
    public final int variableType;
    public final boolean alwaysAvailable;
    public final boolean canCreateAndDelete;
    public final boolean hasValue;
    public final boolean canWriteValue;
    public final boolean canInterceptChanges;
    public final boolean isInvisible;
    public final boolean canReadCreationTime;
    public final boolean canReadLastUpdateTime;
    public HashMap<Integer, String> textConnector;

    public HWiredVariable(HPacket packet) {
        this.id = packet.readString();
        this.variableInternalType = VariableInternalType.fromInt(packet.readInteger());
        this.name = packet.readString();
        this.availabilityType = packet.readInteger();
        this.variableType = packet.readInteger();
        this.alwaysAvailable = packet.readBoolean();
        this.canCreateAndDelete = packet.readBoolean();
        this.hasValue = packet.readBoolean();
        this.canWriteValue = packet.readBoolean();
        this.canInterceptChanges = packet.readBoolean();
        this.isInvisible = packet.readBoolean();
        this.canReadCreationTime = packet.readBoolean();
        this.canReadLastUpdateTime = packet.readBoolean();

        if (packet.readBoolean()) {
            this.textConnector = new HashMap<>();
            int textConnectorCount = packet.readInteger();
            for (int i = 0; i < textConnectorCount; i++) {
                this.textConnector.put(packet.readInteger(), packet.readString());
            }
        }
    }
}
