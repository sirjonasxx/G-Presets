package extension.tools.presetconfig.ads_bg;

import org.json.JSONObject;

public class PresetAdsBackground {

    private int furniId;

    private String imageUrl;
    private String offsetX;
    private String offsetY;
    private String offsetZ;

    public PresetAdsBackground(
            int furniId, String imageUrl, String offsetX, String offsetY, String offsetZ) {
        this.furniId = furniId;
        this.imageUrl = imageUrl;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public PresetAdsBackground(JSONObject object) {
        furniId = object.getInt("furniId");

        imageUrl = object.getString("imageUrl");
        offsetX = object.getString("offsetX");
        offsetY = object.getString("offsetY");
        offsetZ = object.getString("offsetZ");
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getOffsetX() {
        return offsetX;
    }

    public String getOffsetY() {
        return offsetY;
    }

    public String getOffsetZ() {
        return offsetZ;
    }

    public int getFurniId() {
        return furniId;
    }

    public void setFurniId(int furniId) {
        this.furniId = furniId;
    }
}
