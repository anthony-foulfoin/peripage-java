package io.peripage.helper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ByteHelperTest {

    // Returns a string with ASCII characters when given a byte array with ASCII characters
    @Test
    public void test_returns_string_with_ascii_characters() {
        // Given
        byte[] data = "Hello".getBytes(StandardCharsets.US_ASCII);

        // When
        String result = ByteHelper.toStringAscii(data);

        // Then
        assertEquals("Hello", result);
    }

    // Returns an empty string when given an empty byte array
    @Test
    public void test_returns_empty_string() {
        // Given
        byte[] data = new byte[0];

        // When
        String result = ByteHelper.toStringAscii(data);

        // Then
        assertEquals("", result);
    }

    // Returns a string with special characters when given a byte array with special characters
    @Test
    public void test_returns_string_with_special_characters() {
        // Given
        byte[] data = {65, 66, 67, 33, 64, 35};

        // When
        String result = ByteHelper.toStringAscii(data);

        // Then
        assertEquals("ABC!@#", result);
    }

    // Throws NullPointerException when given a null byte array
    @Test()
    public void test_throws_null_pointer_exception() {
        // Given
        byte[] data = null;

        // When
        assertThrows(NullPointerException.class, () -> {
            ByteHelper.toStringAscii(data);
        });
    }

}
