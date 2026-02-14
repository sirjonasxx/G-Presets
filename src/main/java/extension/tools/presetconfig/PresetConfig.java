package extension.tools.presetconfig;

import extension.tools.postconfig.FurniPostConfig;
import extension.tools.postconfig.PostConfig;
import extension.tools.presetconfig.ads_bg.PresetAdsBackground;
import extension.tools.presetconfig.binding.PresetWiredFurniBinding;
import extension.tools.presetconfig.furni.PresetFloorFurni;
import extension.tools.presetconfig.furni.PresetWallFurni;
import extension.tools.presetconfig.wired.PresetWireds;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetConfig implements PresetJsonConfigurable {

    private List<PresetFloorFurni> floorFurniture;
    private List<PresetWallFurni> wallFurniture;
    private PresetWireds presetWireds;
    private List<PresetWiredFurniBinding> bindings;
    private List<PresetAdsBackground> adsBackgrounds;

    public PresetConfig(List<PresetFloorFurni> floorFurniture, List<PresetWallFurni> wallFurniture, PresetWireds presetWireds, List<PresetWiredFurniBinding> bindings, List<PresetAdsBackground> adsBackgrounds) {
        this.floorFurniture = floorFurniture;
        this.wallFurniture = wallFurniture;
        this.presetWireds = presetWireds;
        this.bindings = bindings;
        this.adsBackgrounds = adsBackgrounds;
    }

    public PresetConfig(JSONObject object) {
        floorFurniture = object.getJSONArray("furni").toList().stream()
                .map(o -> new PresetFloorFurni(new JSONObject((Map)o))).collect(Collectors.toList());

        wallFurniture = !object.has("wallFurni") ? new ArrayList<>() :
                object.getJSONArray("wallFurni").toList().stream()
                .map(o -> new PresetWallFurni(new JSONObject((Map)o))).collect(Collectors.toList());

        presetWireds = new PresetWireds(object.getJSONObject("wired"));

        bindings = object.getJSONArray("bindings").toList().stream()
                .map(o -> new PresetWiredFurniBinding(new JSONObject((Map)o))).collect(Collectors.toList());

        adsBackgrounds = !object.has("adsBackgrounds") ? new ArrayList<>() :
                object.getJSONArray("adsBackgrounds").toList().stream()
                .map(o -> new PresetAdsBackground(new JSONObject((Map)o))).collect(Collectors.toList());
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();

        JSONArray jsonFurni = new JSONArray(floorFurniture.stream().map(PresetFloorFurni::toJsonObject).collect(Collectors.toList()));
        JSONArray jsonWallFurni = new JSONArray(wallFurniture.stream().map(PresetWallFurni::toJsonObject).collect(Collectors.toList()));
        JSONArray jsonBindings = new JSONArray(bindings.stream().map(PresetWiredFurniBinding::toJsonObject).collect(Collectors.toList()));

        object.put("furni", jsonFurni);
        object.put("wallFurni", jsonWallFurni);
        object.put("wired", presetWireds.toJsonObject());
        object.put("bindings", jsonBindings);
        if (!adsBackgrounds.isEmpty()) {
            object.put("adsBackgrounds", adsBackgrounds);
        }
        return object;
    }


    public Map<Integer, Integer> applyPostConfig(PostConfig postConfig) {
        Map<Integer, Integer> furniIdMap = new HashMap<>();
        // maps placeholder furni id in the config to the real furni in the room
        // this must be created here because postconfig may already replace preset furni with real furni

        // apply global classname mapper
        // apply useExistingFurni
        for (int i = floorFurniture.size() - 1; i >= 0 ; i--) {
            PresetFloorFurni p = floorFurniture.get(i);
            FurniPostConfig c = postConfig.getFurniPostConfig(p.getFurniName());
//            furniIdMap.put(p.getFurniId(), -1);
            if (c != null && c.isUseExistingFurni()) {
                furniIdMap.put(p.getFurniId(), c.getExistingFurniId());
                floorFurniture.remove(i);
            }
            else if (postConfig.getClassMapping(p.getClassName()) != null) {
                p.setClassName(postConfig.getClassMapping(p.getClassName()));
            }
        }
        // todo apply other stuff

        return furniIdMap;
    }

    public List<PresetFloorFurni> getFloorFurniture() {
        return floorFurniture;
    }

    public void setFloorFurniture(List<PresetFloorFurni> furniture) {
        this.floorFurniture = furniture;
    }

    public List<PresetWallFurni> getWallFurniture() {
        return wallFurniture;
    }

    public void setWallFurniture(List<PresetWallFurni> furniture) {
        this.wallFurniture = furniture;
    }

    public PresetWireds getPresetWireds() {
        return presetWireds;
    }

    public void setPresetWireds(PresetWireds presetWireds) {
        this.presetWireds = presetWireds;
    }

    public List<PresetWiredFurniBinding> getBindings() {
        return bindings;
    }

    public void setBindings(List<PresetWiredFurniBinding> bindings) {
        this.bindings = bindings;
    }

    public List<PresetAdsBackground> getAdsBackgrounds() {
        return adsBackgrounds;
    }

    public void setAdsBackgrounds(List<PresetAdsBackground> adsBackgrounds) {
        this.adsBackgrounds = adsBackgrounds;
    }
}
