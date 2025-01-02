package extension.parsers;

public enum VariableInternalType {
    USER_CREATED(0),
    INTERNAL(1),
    SUB(2),
    SMART(3);

    private final int type;

    VariableInternalType(int type) {
        this.type = type;
    }

    public int toInt() {
        return type;
    }

    public static VariableInternalType fromInt(int type) {
        for (VariableInternalType variableType : VariableInternalType.values()) {
            if (variableType.type == type) {
                return variableType;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + type);
    }
}
