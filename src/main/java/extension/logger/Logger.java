package extension.logger;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Logger {

    private StyleClassedTextArea area;
    private volatile boolean initialized = false;
    private final List<Element> appendOnLoad = new ArrayList<>();

    public void initialize(BorderPane borderPane) {
        area = new StyleClassedTextArea();
        area.getStyleClass().add("themed-background");
//        area.setWrapText(true);
        area.setEditable(false);

        VirtualizedScrollPane<StyleClassedTextArea> vsPane = new VirtualizedScrollPane<>(area);
        borderPane.setCenter(vsPane);

        synchronized (appendOnLoad) {
            initialized = true;
            if (!appendOnLoad.isEmpty()) {
                appendLog(appendOnLoad);
                appendOnLoad.clear();
            }
        }
    }

    private synchronized void appendLog(List<Element> elements) {
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();
            StyleSpansBuilder<Collection<String>> styleSpansBuilder = new StyleSpansBuilder<>(0);

            for (Element element : elements) {
                sb.append(element.text);

                styleSpansBuilder.add(Collections.singleton(element.className), element.text.length());
            }

            int oldLen = area.getLength();
            area.appendText(sb.toString());
//            System.out.println(sb.toString());
            area.setStyleSpans(oldLen, styleSpansBuilder.create());

//            if (autoScroll) {
            area.moveTo(area.getLength());
            area.requestFollowCaret();
//            }
        });
    }

    public void log(String s, String className) {
        logNoNewline(s + "\n", className);
    }

    public void logNoNewline(String s, String className) {
        s = cleanTextContent(s);

        List<Element> elements = new ArrayList<>();
        elements.add(new Element(s, className.toLowerCase()));

        synchronized (appendOnLoad) {
            if (initialized) {
                appendLog(elements);
            }
            else {
                appendOnLoad.addAll(elements);
            }
        }
    }

    private static String cleanTextContent(String text)
    {
//        // strips off all non-ASCII characters
//        text = text.replaceAll("[^\\x00-\\x7F]", "");
//
//        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        // removes non-printable characters from Unicode
//        text = text.replaceAll("\\p{C}", "");

//        return text.trim();
        return text;
    }

}
