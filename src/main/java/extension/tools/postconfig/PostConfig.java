package extension.tools.postconfig;

import java.util.HashMap;
import java.util.Map;

// cant be edited while importing
public class PostConfig {

    private ItemSource itemSource = ItemSource.PREFER_BC;

    private Map<String, String> globalClassNameMapper = new HashMap<>();
    private Map<String, FurniPostConfig> furniPostConfigs = new HashMap<>(); // key = identifier

    public void removeClassNameMapping(String className) {
        globalClassNameMapper.remove(className);
    }

    public void addClassNameMapping(String oldClass, String newClass) {
        globalClassNameMapper.put(oldClass, newClass);
    }

    public void addFurniPostConfig(FurniPostConfig config) {
        furniPostConfigs.put(config.getFurniIdentifier(), config);
    }

    public void removePostConfig(String furniIdentifier) {
        furniPostConfigs.remove(furniIdentifier);
    }

    public String getClassMapping(String className) {
        return globalClassNameMapper.get(className);
    }

    public FurniPostConfig getFurniPostConfig(String furniIdentifier) {
        return furniPostConfigs.get(furniIdentifier);
    }

    public ItemSource getItemSource() {
        return itemSource;
    }

    public void setItemSource(ItemSource itemSource) {
        this.itemSource = itemSource;
    }
}
