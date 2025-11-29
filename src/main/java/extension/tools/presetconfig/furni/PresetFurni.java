package extension.tools.presetconfig.furni;

import extension.tools.presetconfig.PresetJsonConfigurable;
import gearth.extensions.parsers.HPoint;
import org.json.JSONObject;

public class PresetFurni implements PresetJsonConfigurable {

    private int furniId;
    private String className;
    private HPoint location; // relative to selection, both x & y & z matter
    private int rotation;
    private String
            state; // only if category = legacystuffdata (0), only apply if UseFurniture packet
    // works

    private String furniName =
            null; // uniquely given name by GPresets, based on furniId and className

    public PresetFurni(int furniId, String className, HPoint location, int rotation, String state) {
        this.furniId = furniId;
        this.className = className;
        this.location = location;
        this.rotation = rotation;
        this.state = state;
    }

    // deep copy constructor
    public PresetFurni(PresetFurni furni) {
        this.furniId = furni.furniId;
        this.className = furni.className;
        this.location =
                new HPoint(furni.location.getX(), furni.location.getY(), furni.location.getZ());
        this.rotation = furni.rotation;
        this.state = furni.state;
        this.furniName = furni.furniName;
    }

    public PresetFurni(JSONObject jsonObject) {
        this.furniId = jsonObject.getInt("id");
        this.className = jsonObject.getString("className");
        JSONObject jsonLocation = jsonObject.getJSONObject("location");
        this.location =
                new HPoint(
                        jsonLocation.getInt("x"),
                        jsonLocation.getInt("y"),
                        jsonLocation.getDouble("z"));
        this.rotation = jsonObject.getInt("rotation");
        this.state = jsonObject.has("state") ? jsonObject.getString("state") : null;
        this.furniName = jsonObject.getString("name"); // required when parsing from jsonobject
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();
        object.put("id", furniId);
        object.put("className", className);

        JSONObject jsonLocation = new JSONObject();
        jsonLocation.put("x", location.getX());
        jsonLocation.put("y", location.getY());
        jsonLocation.put("z", location.getZ());
        object.put("location", jsonLocation);

        object.put("rotation", rotation);

        if (state != null) {
            object.put("state", state);
        }

        //        if (furniName != null) {
        object.put("name", furniName); // required when exporting to json object
        //        }

        return object;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public HPoint getLocation() {
        return location;
    }

    public void setLocation(HPoint location) {
        this.location = location;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getFurniId() {
        return furniId;
    }

    public void setFurniId(int furniId) {
        this.furniId = furniId;
    }

    public void setFurniName(String furniName) {
        this.furniName = furniName;
    }

    public String getFurniName() {
        return furniName;
    }
}
