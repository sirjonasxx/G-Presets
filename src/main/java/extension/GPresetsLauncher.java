package extension;

import gearth.extensions.ExtensionInfo;
import gearth.extensions.ThemedExtensionFormCreator;
import java.net.URL;
import javafx.stage.Stage;

public class GPresetsLauncher extends ThemedExtensionFormCreator {

    @Override
    protected String getTitle() {
        return "G-Presets - Building & Wired Presets - "
                + GPresets.class.getAnnotation(ExtensionInfo.class).Version();
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("ui/gpresets.fxml");
    }

    @Override
    protected void initialize(Stage primaryStage) {
        primaryStage
                .getScene()
                .getStylesheets()
                .add(getClass().getResource("ui/styles.css").toExternalForm());
    }

    public static void main(String[] args) {
        runExtensionForm(args, GPresetsLauncher.class);
    }
}
