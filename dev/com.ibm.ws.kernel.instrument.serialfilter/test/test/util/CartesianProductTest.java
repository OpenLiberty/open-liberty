package test.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;

public class CartesianProductTest {
    static final String[] NO_STRINGS = {};
    static final String[] ONE_STRING = {"A"};
    static final String[] TWO_STRINGS = {"B", "C"};
    static final String[] THREE_STRINGS = {"D", "E", "F"};

    @Test
    public void testEmpty() {
        assertEmpty(new CartesianProduct(new Object[][]{}));
        assertEmpty(CartesianProduct.of(NO_STRINGS));
        assertEmpty(CartesianProduct.of(NO_STRINGS).with(ONE_STRING));
        assertEmpty(CartesianProduct.of(TWO_STRINGS).with(ONE_STRING).with(NO_STRINGS));
    }

    private void assertEmpty(CartesianProduct p) {
        assertEquals(0, p.size());
        assertTrue(p.isEmpty());
        assertEquals(Collections.EMPTY_LIST, new ArrayList<Object[]>(p));
    }

    @Test
    public void testSingleArray() {assertPattern("A", ONE_STRING);}

    @Test
    public void test1x1() {assertPattern("AA",ONE_STRING, ONE_STRING);}

    @Test
    public void test1x2() {assertPattern("AB AC",ONE_STRING, TWO_STRINGS);}

    @Test
    public void test3x2() {assertPattern("DB DC EB EC FB FC", THREE_STRINGS, TWO_STRINGS);}

    @Test
    public void test2x2() {assertPattern("BB BC CB CC", TWO_STRINGS, TWO_STRINGS);}

    @Test
    public void test2x2x2() {assertPattern("BBB BBC BCB BCC CBB CBC CCB CCC", TWO_STRINGS, TWO_STRINGS, TWO_STRINGS);}

    private static void assertPattern(String expected, Object[]...arrs) {
        CartesianProduct p = new CartesianProduct(arrs);
        String actual = p.toString().replaceAll("[{}(),]", "");
        assertEquals(expected, actual);
    }
}
