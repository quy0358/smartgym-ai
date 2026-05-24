package ntu.quy65132908.smartgym_ai.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeepSeekKeyProviderTest {

    @Test
    public void getApiKey_trimsConfiguredKey() {
        DeepSeekKeyProvider provider = new DeepSeekKeyProvider("  abc123  ");

        assertEquals("abc123", provider.getApiKey());
        assertTrue(provider.hasApiKey());
    }

    @Test
    public void hasApiKey_blankOrNullReturnsFalse() {
        assertFalse(new DeepSeekKeyProvider("").hasApiKey());
        assertFalse(new DeepSeekKeyProvider("   ").hasApiKey());
        assertFalse(new DeepSeekKeyProvider(null).hasApiKey());
    }
}
