package extension.tools.presetconfig.binding;

import extension.tools.presetconfig.PresetJsonConfigurable;
import gearth.extensions.parsers.HPoint;
import org.json.JSONObject;

public class PresetWiredFurniBinding implements PresetJsonConfigurable {

    private int furniId;
    private int wiredId;

    // binding only required if != null (-> means the checkbox is checked)
    private HPoint location; // relative to selection, only x & y matters
    private Integer rotation;

    // only needs to be applied if state can be changed with UseFurniture packets
    // and if furni category = LegacyStuffData (0) & state is a number
    // or if furni category = MapStuffData (1) and contains "state" & is a number? / check if this
    // can ever be toggled with UseFurniture
    private String state;

    private Integer altitude;

    public PresetWiredFurniBinding(
            int furniId,
            int wiredId,
            HPoint location,
            Integer rotation,
            String state,
            Integer altitude) {
        this.furniId = furniId;
        this.wiredId = wiredId;
        this.location = location;
        this.rotation = rotation;
        this.state = state;
        this.altitude = altitude;
    }

    // deep copy constructor
    public PresetWiredFurniBinding(PresetWiredFurniBinding binding) {
        this.furniId = binding.furniId;
        this.wiredId = binding.wiredId;
        this.location =
                binding.location == null
                        ? null
                        : new HPoint(binding.location.getX(), binding.location.getY());
        this.rotation = binding.rotation;
        this.state = binding.state;
        this.altitude = binding.altitude;
    }

    public PresetWiredFurniBinding(JSONObject object) {
        this.furniId = object.getInt("furniId");
        this.wiredId = object.getInt("wiredId");

        this.location =
                object.has("location")
                        ? new HPoint(
                                object.getJSONObject("location").getInt("x"),
                                object.getJSONObject("location").getInt("y"))
                        : null;
        this.rotation = object.has("rotation") ? object.getInt("rotation") : null;
        this.state = object.has("state") ? object.getString("state") : null;
        this.altitude = object.has("altitude") ? object.getInt("altitude") : null;
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();
        object.put("furniId", furniId);
        object.put("wiredId", wiredId);

        if (location != null) {
            JSONObject jsonLocation = new JSONObject();
            jsonLocation.put("x", location.getX());
            jsonLocation.put("y", location.getY());
            object.put("location", jsonLocation);
        }
        if (rotation != null) {
            object.put("rotation", rotation);
        }
        if (state != null) {
            object.put("state", state);
        }
        if (altitude != null) {
            object.put("altitude", altitude);
        }

        return object;
    }

    public int getFurniId() {
        return furniId;
    }

    public void setFurniId(int furniId) {
        this.furniId = furniId;
    }

    public int getWiredId() {
        return wiredId;
    }

    public void setWiredId(int wiredId) {
        this.wiredId = wiredId;
    }

    public HPoint getLocation() {
        return location;
    }

    public void setLocation(HPoint location) {
        this.location = location;
    }

    public Integer getRotation() {
        return rotation;
    }

    public void setRotation(Integer rotation) {
        this.rotation = rotation;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getAltitude() {
        return altitude;
    }

    public void setAltitude(Integer altitude) {
        this.altitude = altitude;
    }
}
