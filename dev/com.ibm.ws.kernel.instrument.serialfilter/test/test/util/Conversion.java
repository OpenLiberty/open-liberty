package test.util;

public enum Conversion {;

    public static byte[] fromOctal(String octalString) {
        char[] octal = octalString.replaceAll("[^0-7]+", "").toCharArray();
        byte[] result = new byte[(7 + 3 * octal.length) / 8];
        int byteIndex = 0;
        for (int i = 0; i < octal.length; i++) {
            int octVal = octal[i] - '0';
            switch (i%8) {
                case 0:
                    result[byteIndex] |= (octVal<<5);
                    break;
                case 1:
                    result[byteIndex] |= (octVal<<2);
                    break;
                case 2:
                    result[byteIndex] |= (octVal>>1);
                    byteIndex++;
                    result[byteIndex] |= (octVal<<7);
                    break;
                case 3:
                    result[byteIndex] |= (octVal<<4);
                    break;
                case 4:
                    result[byteIndex] |= (octVal<<1);
                    break;
                case 5:
                    result[byteIndex] |= (octVal>>2);
                    byteIndex++;
                    result[byteIndex] |= (octVal<<6);
                    break;
                case 6:
                    result[byteIndex] |= (octVal<<3);
                    break;
                case 7:
                    result[byteIndex] |= (octVal<<0);
                    byteIndex++;
                    break;
            }
        }
        return result;
    }

    public static String toOctal(byte[] bytes) {
        String result = "";
        int partial = 0;
        for (int i = 0; i < bytes.length; i++) {
            int b = 0xFF & bytes[i];
            switch (i%3) {
                case 0:
                    result += String.format("%o%o", (b & 0xE0)>>>5, (b & 0x1C)>>>2);
                    partial = (b & 0x03) << 1;
                    break;
                case 1:
                    result += String.format("%o%o%o", partial + (b>>>7), (b & 0x70)>>>4, (b & 0x0E)>>>1);
                    partial = (b & 0x01) << 2;
                    break;

                case 2:
                    result += String.format("%o%o%o", partial + (b>>>6), (b & 0x38)>>>3, (b & 0x07)>>>0);
                    break;
            }
        }
        // now write the remaining bits from partial
        switch (bytes.length % 3) {
            case 0: // nothing to write
                break;
            case 1:
            case 2:
                result += String.format("%o", partial);
                break;
        }
        return result;
    }


    public static String toBinary(byte[] bytes) {
        String result = "";
        for (byte b: bytes)
            result += Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
        return result;
    }

    public static String toHex(byte[] bytes) {
        String result = "";
        for (byte b : bytes)
            result += String.format("%02X", b);
        return result;
    }
}
