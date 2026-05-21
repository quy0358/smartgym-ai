package ntu.quy65132908.smartgym_ai.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class InputValidatorTest {
    @Test
    public void sanitizeName_trimsWhitespace() {
        assertEquals("John", InputValidator.sanitizeName("  John  "));
    }
    @Test
    public void sanitizeName_limitsTo50Chars() {
        String longName = new String(new char[100]).replace('\0', 'A');
        assertEquals(50, InputValidator.sanitizeName(longName).length());
    }
    @Test
    public void sanitizeName_stripsHtml() {
        assertEquals("alert", InputValidator.sanitizeName("<script>alert</script>"));
    }
    @Test
    public void sanitizeName_nullReturnsEmpty() {
        assertEquals("", InputValidator.sanitizeName(null));
    }
    @Test
    public void sanitizeContent_limitsTo500() {
        String s = new String(new char[600]).replace('\0', 'B');
        assertEquals(500, InputValidator.sanitizeContent(s).length());
    }
    @Test
    public void isValidName_emptyReturnsFalse() {
        assertFalse(InputValidator.isValidName(""));
        assertFalse(InputValidator.isValidName("   "));
        assertFalse(InputValidator.isValidName(null));
    }
    @Test
    public void isValidName_validReturnsTrue() {
        assertTrue(InputValidator.isValidName("Nguyen Van A"));
    }
}
