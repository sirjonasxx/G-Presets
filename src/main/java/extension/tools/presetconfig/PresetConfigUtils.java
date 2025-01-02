package extension.tools.presetconfig;

import extension.GPresets;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PresetConfigUtils {
    public static final String PRESET_EXT = ".json";

    public static String presetPath() {
        try {
            String path = (new File(GPresets.class.getProtectionDomain().getCodeSource().getLocation().toURI()))
                    .getParentFile().toString();
            return Paths.get(path, "presets").toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return "";
    }


    public static List<String> listPresets() {
        List<String> presets = new ArrayList<>();
        File presetsDir = new File(presetPath());

        if (!presetsDir.isDirectory()) {
            return presets;
        }

        File[] files = presetsDir.listFiles();
        if (files != null) {
            for (File presetFile : files) {
                if (presetFile.isFile()) {
                    String name = presetFile.getName();
                    if (name.endsWith(PRESET_EXT)) {
                        name = name.substring(0, name.length() - PRESET_EXT.length());
                        presets.add(name);
                    }
                }
            }
        }
        return presets;
    }

    public static boolean savePreset(String name, PresetConfig config) {
        File presetPath = new File(presetPath());
        presetPath.mkdirs();

        try (Writer file = new OutputStreamWriter(Files.newOutputStream(new File(presetPath(), name + PRESET_EXT).toPath()), StandardCharsets.UTF_8)) {
            file.write(config.toJsonObject().toString(4));
            file.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static PresetConfig loadPreset(String name) {
        File file = new File(presetPath(), name + PRESET_EXT);
        if (file.exists() && file.isFile()) {
            try {
                String contents = String.join("\n", Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
                return new PresetConfig(new JSONObject(contents));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

}
