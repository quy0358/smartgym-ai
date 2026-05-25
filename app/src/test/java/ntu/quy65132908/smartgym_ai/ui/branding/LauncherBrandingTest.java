package ntu.quy65132908.smartgym_ai.ui.branding;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LauncherBrandingTest {
    private static final String[] LEGACY_NEON_COLORS = {
            "#FFC3F400",
            "#FF0356FF",
            "#FF00E5FF",
            "#26C3F400",
            "#33C3F400"
    };

    @Test
    public void launcherVectors_useSmartGymLogoPaletteOnly() throws Exception {
        String foreground = read("src/main/res/drawable/ic_launcher_foreground.xml");
        String background = read("src/main/res/drawable/ic_launcher_background.xml");
        String todayBadge = read("src/main/res/drawable/bg_badge_today.xml");
        String launcher = (foreground + background + todayBadge).toUpperCase(Locale.ROOT);

        for (String legacyColor : LEGACY_NEON_COLORS) {
            assertFalse("Legacy neon color still present: " + legacyColor,
                    launcher.contains(legacyColor));
        }

        assertTrue("Launcher foreground should use SmartGym purple",
                launcher.contains("#FF6739FF"));
        assertTrue("Launcher foreground should use SmartGym secondary purple",
                launcher.contains("#FF997AFF"));
        assertTrue("Launcher foreground should use SmartGym green sparkle",
                launcher.contains("#FF34C759"));
    }

    @Test
    public void drawableXmlAssets_doNotUseLegacyNeonPalette() throws Exception {
        List<File> drawables = new ArrayList<>();
        collectXmlFiles(new File("src/main/res/drawable"), drawables);
        collectXmlFiles(new File("app/src/main/res/drawable"), drawables);

        List<String> failures = new ArrayList<>();
        for (File drawable : drawables) {
            String content = read(drawable.getPath()).toUpperCase(Locale.ROOT);
            for (String legacyColor : LEGACY_NEON_COLORS) {
                if (content.contains(legacyColor)) {
                    failures.add(drawable.getPath() + " contains " + legacyColor);
                }
            }
        }

        assertTrue("Legacy neon colors found in drawable XML: " + failures, failures.isEmpty());
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    private static void collectXmlFiles(File root, List<File> files) {
        if (!root.exists()) {
            return;
        }
        File[] children = root.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectXmlFiles(child, files);
            } else if (child.getName().endsWith(".xml")) {
                files.add(child);
            }
        }
    }
}
