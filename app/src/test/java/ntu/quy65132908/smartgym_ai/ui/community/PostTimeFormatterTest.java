package ntu.quy65132908.smartgym_ai.ui.community;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class PostTimeFormatterTest {

    private Context context;

    @Before
    public void setup() {
        context = RuntimeEnvironment.getApplication();
    }

    @Test
    public void format_recentTimesUsesRelativeLabels() {
        long now = 10L * 24 * 60 * 60_000L;

        assertEquals("Vừa xong", PostTimeFormatter.format(context, now - 10_000L, now));
        assertEquals("5 phút trước", PostTimeFormatter.format(context, now - 5 * 60_000L, now));
        assertEquals("3 giờ trước", PostTimeFormatter.format(context, now - 3 * 60 * 60_000L, now));
        assertEquals("2 ngày trước", PostTimeFormatter.format(context, now - 2 * 24 * 60 * 60_000L, now));
    }

    @Test
    public void format_oldTimeUsesDate() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            long eightDays = 8L * 24 * 60 * 60_000L;
            assertEquals("01/01/1970", PostTimeFormatter.format(context, 0L, eightDays));
        } finally {
            Locale.setDefault(original);
        }
    }
}
