package io.peripage.helper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

public class ByteHelper {

    public static String toStringAscii(byte[] data) {
        return new String(data, StandardCharsets.US_ASCII);
    }

    public static byte[] hexStringToByteArray(String hexString) {
        return HexFormat.of().parseHex(hexString);
    }

    public static String asciiToHex(String asciiString) {
        StringBuilder hexString = new StringBuilder();

        for (char ch : asciiString.toCharArray()) {
            String hex = Integer.toHexString(ch);
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static byte[] intToBigEndianBytes(int value) {
        return intToBigEndianBytes(value, 1);
    }

    public static byte[] intToBigEndianBytes(int value, int numBytes) {
        byte[] result = new byte[numBytes];

        for (int i = numBytes - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        return result;
    }

    public static byte[] padRowBytes(int length, byte[] rowBytes) {
        if (rowBytes.length < length) {
            byte[] padding = new byte[length - rowBytes.length];
            rowBytes = ByteBuffer.allocate(length).put(rowBytes).put(padding).array();
        } else if (rowBytes.length > length) {
            rowBytes = Arrays.copyOfRange(rowBytes, 0, length);
        }
        return rowBytes;
    }
}
