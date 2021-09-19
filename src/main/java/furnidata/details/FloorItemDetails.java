package furnidata.details;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// source: https://github.com/kouris-h/HabboAPI/blob/master/src/main/java/gamedata/furnidata/furnidetails/FurniDetails.java
// by wiredspast & kouris
public class FloorItemDetails extends FurniDetails {
    public final String customParams;
    public final int xDim, yDim, defaultDir, specialType;
    public final boolean canStandOn, canSitOn, canLayOn;
    public final List<String> partColors;

    public FloorItemDetails(JSONObject jsonObject) {
        super(jsonObject);

        this.customParams = jsonObject.optString("customparams", null);

        this.xDim = jsonObject.getInt("xdim");
        this.yDim = jsonObject.getInt("ydim");
        this.defaultDir = jsonObject.getInt("defaultdir");
        this.specialType = jsonObject.getInt("specialtype");

        this.canStandOn = jsonObject.getBoolean("canstandon");
        this.canSitOn = jsonObject.getBoolean("cansiton");
        this.canLayOn = jsonObject.getBoolean("canlayon");

        this.partColors = jsonObject.has("partcolors") ?
                Collections.unmodifiableList(
                        jsonObject
                            .getJSONObject("partcolors")
                            .getJSONArray("color")
                            .toList()
                            .stream()
                            .map(o -> (String) o)
                            .collect(Collectors.toList())
                ) : null;
    }

}