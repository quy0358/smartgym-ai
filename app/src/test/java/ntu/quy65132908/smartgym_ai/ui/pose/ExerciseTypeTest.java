package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ExerciseTypeTest {

    @Test
    public void fromKey_defaultsToPushUp() {
        assertEquals(ExerciseType.PUSH_UP, ExerciseType.fromKey(null));
        assertEquals(ExerciseType.PUSH_UP, ExerciseType.fromKey("unknown"));
    }

    @Test
    public void fromKey_acceptsCommonSeparators() {
        assertEquals(ExerciseType.PUSH_UP, ExerciseType.fromKey("push-up"));
        assertEquals(ExerciseType.SQUAT, ExerciseType.fromKey("squat"));
        assertEquals(ExerciseType.PLANK, ExerciseType.fromKey("plank"));
    }

    @Test
    public void displayNames_areReadableVietnamese() {
        assertEquals("Chống đẩy", ExerciseType.PUSH_UP.getDisplayName());
        assertEquals("Squat", ExerciseType.SQUAT.getDisplayName());
        assertEquals("Plank", ExerciseType.PLANK.getDisplayName());

        for (ExerciseType type : ExerciseType.values()) {
            assertNoMojibake(type.getDisplayName());
        }
    }

    private static void assertNoMojibake(String value) {
        String[] markers = {"Ã", "Ä", "á»", "áº", "Æ", "â€¢", "â€", "â€¦", "â†", "â", "ðŸ", "ï¸", "Å"};
        for (String marker : markers) {
            assertFalse("Unexpected mojibake marker " + marker + " in: " + value,
                    value.contains(marker));
        }
    }
}
