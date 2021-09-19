package extension.tools;

public enum StackTileSetting {

    Large("tile_stackmagic2", 2),
    XL("tile_stackmagic4x4", 4),
    XXL("tile_stackmagic6x6", 6),
    XXXL("tile_stackmagic8x8", 8);

    private String className;
    private int dimension;

    StackTileSetting(String className, int dimension) {
        this.className = className;
        this.dimension = dimension;
    }

    public String getClassName() {
        return className;
    }

    public int getDimension() {
        return dimension;
    }

    public static StackTileSetting fromString(String s) {
        if (s.equals("2x2")) return Large;
        else if (s.equals("4x4")) return XL;
        else if (s.equals("6x6")) return XXL;
        else if (s.equals("8x8")) return XXXL;
        return null;
    }

    @Override
    public String toString() {
        return String.format("%dx%d", dimension, dimension);
    }
}
