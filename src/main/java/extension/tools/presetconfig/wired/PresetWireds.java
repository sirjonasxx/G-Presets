package extension.tools.presetconfig.wired;

import extension.tools.presetconfig.PresetJsonConfigurable;
import extension.tools.presetconfig.furni.PresetFurni;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetWireds implements PresetJsonConfigurable {

    private List<PresetWiredCondition> conditions;
    private List<PresetWiredEffect> effects;
    private List<PresetWiredTrigger> triggers;
    private List<PresetWiredAddon> addons;
    private List<PresetWiredSelector> selectors;

    public PresetWireds(List<PresetWiredCondition> conditions, List<PresetWiredEffect> effects, List<PresetWiredTrigger> triggers, List<PresetWiredAddon> addons, List<PresetWiredSelector> selectors) {
        this.conditions = conditions;
        this.effects = effects;
        this.triggers = triggers;
        this.addons = addons;
        this.selectors = selectors;
    }

    public PresetWireds(JSONObject object) {
        conditions = object.getJSONArray("conditions").toList().stream()
                .map(o -> new PresetWiredCondition(new JSONObject((Map)o))).collect(Collectors.toList());

        effects = object.getJSONArray("effects").toList().stream()
                .map(o -> new PresetWiredEffect(new JSONObject((Map)o))).collect(Collectors.toList());

        triggers = object.getJSONArray("triggers").toList().stream()
                .map(o -> new PresetWiredTrigger(new JSONObject((Map)o))).collect(Collectors.toList());

        addons = object.has("addons") ?
                object.getJSONArray("addons").toList().stream()
                        .map(o -> new PresetWiredAddon(new JSONObject((Map)o))).collect(Collectors.toList()) :
                new ArrayList<>();

        selectors = object.has("selectors") ?
                object.getJSONArray("selectors").toList().stream()
                        .map(o -> new PresetWiredSelector(new JSONObject((Map)o))).collect(Collectors.toList()) :
                new ArrayList<>();
    }


    @Override
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();

        JSONArray jsonConditions = new JSONArray(conditions.stream().map(PresetWiredCondition::toJsonObject).collect(Collectors.toList()));
        JSONArray jsonEffects = new JSONArray(effects.stream().map(PresetWiredEffect::toJsonObject).collect(Collectors.toList()));
        JSONArray jsonTriggers = new JSONArray(triggers.stream().map(PresetWiredTrigger::toJsonObject).collect(Collectors.toList()));
        JSONArray jsonAddons = new JSONArray(addons.stream().map(PresetWiredAddon::toJsonObject).collect(Collectors.toList()));
        JSONArray jsonSelectors = new JSONArray(selectors.stream().map(PresetWiredSelector::toJsonObject).collect(Collectors.toList()));

        object.put("conditions", jsonConditions);
        object.put("effects", jsonEffects);
        object.put("triggers", jsonTriggers);
        object.put("addons", jsonAddons);
        object.put("selectors", jsonSelectors);

        return object;
    }

    public List<PresetWiredCondition> getConditions() {
        return conditions;
    }

    public List<PresetWiredEffect> getEffects() {
        return effects;
    }

    public List<PresetWiredTrigger> getTriggers() {
        return triggers;
    }

    public List<PresetWiredAddon> getAddons() {
        return addons;
    }

    public List<PresetWiredSelector> getSelectors() {
        return selectors;
    }

    public void setConditions(List<PresetWiredCondition> conditions) {
        this.conditions = conditions;
    }

    public void setEffects(List<PresetWiredEffect> effects) {
        this.effects = effects;
    }

    public void setTriggers(List<PresetWiredTrigger> triggers) {
        this.triggers = triggers;
    }

    public void setAddons(List<PresetWiredAddon> addons) {
        this.addons = addons;
    }

    public void setSelectors(List<PresetWiredSelector> selectors) {
        this.selectors = selectors;
    }
}
