package extension.tools.presetconfig.recolorable;

import org.json.JSONObject;

public class PresetRecolorable {
    private int furniId;

    private String red;
    private String green;
    private String blue;

    public PresetRecolorable(int furniId, String red, String green, String blue) {
        this.furniId = furniId;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public PresetRecolorable(JSONObject object) {
        furniId = object.getInt("furniId");

        red = object.getString("red");
        green = object.getString("green");
        blue = object.getString("blue");
    }

    public String getRed() {
        return red;
    }

    public String getGreen() {
        return green;
    }

    public String getBlue() {
        return blue;
    }

    public int getFurniId() {
        return furniId;
    }

    public void setFurniId(int furniId) {
        this.furniId = furniId;
    }

    public String toColorString() {
        return red + "," + green + "," + blue;
    }
}
