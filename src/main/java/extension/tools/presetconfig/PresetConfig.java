package extension.tools.presetconfig;

import extension.tools.postconfig.FurniPostConfig;
import extension.tools.postconfig.PostConfig;
import extension.tools.presetconfig.ads_bg.PresetAdsBackground;
import extension.tools.presetconfig.binding.PresetWiredFurniBinding;
import extension.tools.presetconfig.furni.PresetFurni;
import extension.tools.presetconfig.wired.PresetWireds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

public class PresetConfig implements PresetJsonConfigurable {

    private List<PresetFurni> furniture;
    private PresetWireds presetWireds;
    private List<PresetWiredFurniBinding> bindings;
    private List<PresetAdsBackground> adsBackgrounds;

    public PresetConfig(
            List<PresetFurni> furniture,
            PresetWireds presetWireds,
            List<PresetWiredFurniBinding> bindings,
            List<PresetAdsBackground> adsBackgrounds) {
        this.furniture = furniture;
        this.presetWireds = presetWireds;
        this.bindings = bindings;
        this.adsBackgrounds = adsBackgrounds;
    }

    public PresetConfig(JSONObject object) {
        furniture =
                object.getJSONArray("furni").toList().stream()
                        .map(o -> new PresetFurni(new JSONObject((Map) o)))
                        .collect(Collectors.toList());

        presetWireds = new PresetWireds(object.getJSONObject("wired"));

        bindings =
                object.getJSONArray("bindings").toList().stream()
                        .map(o -> new PresetWiredFurniBinding(new JSONObject((Map) o)))
                        .collect(Collectors.toList());

        adsBackgrounds =
                !object.has("adsBackgrounds")
                        ? new ArrayList<>()
                        : object.getJSONArray("adsBackgrounds").toList().stream()
                                .map(o -> new PresetAdsBackground(new JSONObject((Map) o)))
                                .collect(Collectors.toList());
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();

        JSONArray jsonFurni =
                new JSONArray(
                        furniture.stream()
                                .map(PresetFurni::toJsonObject)
                                .collect(Collectors.toList()));
        JSONArray jsonBindings =
                new JSONArray(
                        bindings.stream()
                                .map(PresetWiredFurniBinding::toJsonObject)
                                .collect(Collectors.toList()));

        object.put("furni", jsonFurni);
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
        // this must be created here because postconfig may already replace preset furni with real
        // furni

        // apply global classname mapper
        // apply useExistingFurni
        for (int i = furniture.size() - 1; i >= 0; i--) {
            PresetFurni p = furniture.get(i);
            FurniPostConfig c = postConfig.getFurniPostConfig(p.getFurniName());
            //            furniIdMap.put(p.getFurniId(), -1);
            if (c != null && c.isUseExistingFurni()) {
                furniIdMap.put(p.getFurniId(), c.getExistingFurniId());
                furniture.remove(i);
            } else if (postConfig.getClassMapping(p.getClassName()) != null) {
                p.setClassName(postConfig.getClassMapping(p.getClassName()));
            }
        }
        // todo apply other stuff

        return furniIdMap;
    }

    public List<PresetFurni> getFurniture() {
        return furniture;
    }

    public void setFurniture(List<PresetFurni> furniture) {
        this.furniture = furniture;
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
