package ntu.quy65132908.smartgym_ai.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class UiTextEncodingTest {
    private static final String[] MOJIBAKE_MARKERS = {
            "Ã", "Ä", "á»", "áº", "Æ", "â€¢", "â€", "â€¦", "â†", "â", "ðŸ", "ï¸", "Å"
    };

    @Test
    public void userVisibleJavaAndXmlText_hasNoMojibakeMarkers() throws Exception {
        List<File> files = new ArrayList<>();
        collectTextFiles(new File("src/main"), files);
        collectTextFiles(new File("app/src/main"), files);

        List<String> failures = new ArrayList<>();
        for (File file : files) {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            for (String marker : MOJIBAKE_MARKERS) {
                if (content.contains(marker)) {
                    failures.add(file.getPath() + " contains " + marker);
                    break;
                }
            }
        }

        assertTrue("Mojibake markers found: " + failures, failures.isEmpty());
    }

    private static void collectTextFiles(File root, List<File> files) {
        if (!root.exists()) {
            return;
        }
        File[] children = root.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                if (!"font".equals(child.getName()) && !"mipmap".equals(child.getName())) {
                    collectTextFiles(child, files);
                }
            } else if (child.getName().endsWith(".java") || child.getName().endsWith(".xml")) {
                files.add(child);
            }
        }
    }
}
