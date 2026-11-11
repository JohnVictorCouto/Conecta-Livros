package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IsbnUtilsTest {

    @Test
    void cleanRemovesPunctuationAndNormalizesCase() {
        assertEquals("0306406152", IsbnUtils.clean("0-306-40615-2"));
        assertEquals("9780306406152X", IsbnUtils.clean(" 978 0306406152x "));
        assertEquals("", IsbnUtils.clean(null));
    }

    @Test
    void validatesIsbn10IncludingTerminalX() {
        assertTrue(IsbnUtils.isValidIsbn10("0-306-40615-2"));
        assertTrue(IsbnUtils.isValidIsbn10("0-8044-2957-X"));
        assertFalse(IsbnUtils.isValidIsbn10("0-306-40615-3"));
        assertFalse(IsbnUtils.isValidIsbn10("03064061X2"));
    }

    @Test
    void validatesIsbn13AndRejectsWrongLengthOrChecksum() {
        assertTrue(IsbnUtils.isValidIsbn13("978-0-306-40615-7"));
        assertFalse(IsbnUtils.isValidIsbn13("978-0-306-40615-8"));
        assertFalse(IsbnUtils.isValidIsbn13("978030640615"));
        assertFalse(IsbnUtils.isValidIsbn13(null));
    }

    @Test
    void validatesEitherIsbnFormat() {
        assertTrue(IsbnUtils.isValid("0306406152"));
        assertTrue(IsbnUtils.isValid("9780306406157"));
        assertFalse(IsbnUtils.isValid("not an isbn"));
        assertFalse(IsbnUtils.isValid(null));
    }

    @Test
    void convertsBetweenIsbn10AndIsbn13() {
        assertEquals("9780306406157", IsbnUtils.toIsbn13("0306406152"));
        assertEquals("0306406152", IsbnUtils.toIsbn10("9780306406157"));
    }

    @Test
    void conversionRejectsInvalidOrUnsupportedInputs() {
        assertNull(IsbnUtils.toIsbn13("0306406153"));
        assertNull(IsbnUtils.toIsbn10("9790306406157"));
        assertNull(IsbnUtils.toIsbn10("9780306406158"));
    }
}
