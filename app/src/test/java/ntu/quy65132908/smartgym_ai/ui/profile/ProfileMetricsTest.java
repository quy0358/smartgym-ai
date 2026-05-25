package ntu.quy65132908.smartgym_ai.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ProfileMetricsTest {

    @Test
    public void parseOptionalFloat_acceptsCommaDecimal() {
        assertEquals(70.5f, ProfileMetrics.parseOptionalFloat("70,5"), 0.001f);
        assertEquals(167.25f, ProfileMetrics.parseOptionalFloat("167.25"), 0.001f);
    }

    @Test
    public void parseOptionalFloat_rejectsInvalidValues() {
        assertNull(ProfileMetrics.parseOptionalFloat(""));
        assertNull(ProfileMetrics.parseOptionalFloat("abc"));
        assertNull(ProfileMetrics.parseOptionalFloat("NaN"));
    }

    @Test
    public void calculateBmi_usesMetricFormula() {
        assertEquals(25.1f, ProfileMetrics.calculateBmi(70f, 167f), 0.05f);
    }

    @Test
    public void categoryForBmi_matchesBoundaries() {
        assertEquals("Thiếu cân", ProfileMetrics.categoryForBmi(18.4f));
        assertEquals("Bình thường", ProfileMetrics.categoryForBmi(18.5f));
        assertEquals("Bình thường", ProfileMetrics.categoryForBmi(24.9f));
        assertEquals("Thừa cân", ProfileMetrics.categoryForBmi(25f));
        assertEquals("Béo phì", ProfileMetrics.categoryForBmi(30f));
    }
}
