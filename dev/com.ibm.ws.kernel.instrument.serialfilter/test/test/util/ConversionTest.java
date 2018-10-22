package test.util;

import org.junit.Assert;
import org.junit.Test;

public class ConversionTest {
    @Test
    public void testToHex() {
        Assert.assertEquals("", Conversion.toHex(bytes()));
        Assert.assertEquals("7F", Conversion.toHex(bytes(0x7F)));
        Assert.assertEquals("FF", Conversion.toHex(bytes(0xFF)));
        Assert.assertEquals("FF7FFF01FF00", Conversion.toHex(bytes(0xFF, 0x7F, 0xFF, 0x01, 0xFF, 0x00)));
        Assert.assertEquals("0000", Conversion.toHex(bytes(0x00, 0x00)));
        Assert.assertEquals("0048", Conversion.toHex(bytes(0x00, 0x48)));
        Assert.assertEquals("DA01", Conversion.toHex(bytes(0xDA, 0x01)));
    }

//    @Test
//    public void testToBytes() {
//        assertEquals("", toHex(toBytes(bits())));
//        assertEquals("01", toHex(toBytes(bits(0))));
//        assertEquals("11", toHex(toBytes(bits(0,4))));
//        assertArrayEquals(bytes(0x21), toBytes(bits(0,5)));
//        assertArrayEquals(bytes(0x21,0x43), toBytes(bits(0,5,8,9,14)));
//        assertArrayEquals(bytes(0xEE, 0, 0, 0, 0xFF), toBytes(bits(1,2,3,5,6,7,32,33,34,35,36,37,38,39)));
//    }

    @Test
    public void testFromOctal() {
        Assert.assertArrayEquals(bin(""), Conversion.fromOctal(""));
        Assert.assertArrayEquals(bin("001"), Conversion.fromOctal("1"));
        Assert.assertArrayEquals(bin("010"), Conversion.fromOctal("2"));
        Assert.assertArrayEquals(bin("111"), Conversion.fromOctal("7"));
        Assert.assertArrayEquals(bin("111 1"), Conversion.fromOctal("74"));
        Assert.assertArrayEquals(bin("111 111"), Conversion.fromOctal("77"));
        Assert.assertEquals(Conversion.toBinary(bin("010")), Conversion.toBinary(Conversion.fromOctal("20")));
        Assert.assertEquals(Conversion.toBinary(bin("111 111 101")), Conversion.toBinary(Conversion.fromOctal("775")));
        Assert.assertEquals(Conversion.toBinary(bin("010 010 010")), Conversion.toBinary(Conversion.fromOctal("222")));
        Assert.assertEquals(Conversion.toBinary(bin("010 011 111")), Conversion.toBinary(Conversion.fromOctal("237")));
        Assert.assertArrayEquals(bin("010 011"), Conversion.fromOctal("23"));
    }

    @Test
    public void testToOctal() {
        Assert.assertEquals("", Conversion.toOctal(bin("")));
        Assert.assertEquals("000", Conversion.toOctal(bin("0")));
        Assert.assertEquals("400", Conversion.toOctal(bin("1")));
        Assert.assertEquals("200", Conversion.toOctal(bin("01")));
        Assert.assertEquals("100", Conversion.toOctal(bin("001")));
        Assert.assertEquals("040", Conversion.toOctal(bin("000 1")));
        Assert.assertEquals("007000", Conversion.toOctal(bin("000 000 111")));
        Assert.assertEquals("007500", Conversion.toOctal(bin("000 000 111 101")));
    }

    private static byte[] bytes(int...bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) bytes[i];
        }
        return result;
    }

    private static byte[] bin(String bitString) {
        // remove all chars except '0' and '1'
        char[] bits = bitString.replaceAll("[^01]+", "").toCharArray();
        byte[] result = new byte[(7 + bits.length) / 8];
        for (int i = 0; i < bits.length; i++) result[i/8] |= (bits[i] - '0') << (7 - i%8);
        return result;
    }
}
