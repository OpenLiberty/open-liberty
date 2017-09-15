/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal.literals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import test.common.SharedOutputManager;

import static org.junit.Assert.*;

public class LiteralParser_Test {

    @Rule
    public TestRule outputMgr = SharedOutputManager.getInstance().trace("*=all");

    void expectNotToParse(String value) {
        expectToParse(value, value);
    }

    void expectToParse(Object expected, String str) {
        Object actual = LiteralParser.parse(str);
        assertNotNull("parse shoud not return null", actual);
        assertEquals("Comparing types for equality", expected.getClass(), actual.getClass());
        if (expected.equals(Character.valueOf('\0')))
            assertEquals("Comparing values for equality", (int) (Character) expected, (int) (Character) actual);
        assertEquals("Comparing values for equality", expected, actual);
    }

    @Test
    public void testBooleans() {
        expectToParse(true, "true");
        expectToParse(false, "false");
    }

    @Test
    public void testChar() {
        expectToParse('\0', "'\\0'");
        expectToParse('a', "'a'");
        expectToParse('\u0100', "'\\u0100'");
        expectToParse('\100', "'\\100'");
        // failure cases
        expectNotToParse("'");
        expectNotToParse("''");
        expectNotToParse("'..'");
        expectNotToParse("'\\'");
        expectNotToParse("'\\");
        expectNotToParse("'x\\");
    }

    @Test
    public void testDouble() {
        expectToParse(.1, ".1");
        expectToParse(0.1, "0.1");
        expectToParse(0., "0.");
        expectToParse(.1D, ".1D");
        expectToParse(0.1D, ".1D");
        expectToParse(1e2, "1e2");
        expectToParse(.234e2, ".234e2");
        expectToParse(1.234e2, "1.234e2");
        expectToParse(001.234e2, "001.234e2");
        expectToParse(1.234e-2, "1.234e-2");
        expectToParse(001.234e+2, "001.234e+2");
        expectToParse(-001.234e-2, "-001.234e-2");
        expectToParse(+001.234e+2, "+001.234e+2");
        expectToParse(+001.234e+2, "+001.234e+02");
        expectToParse(+009.234e+2, "+009.234e+02");
        expectToParse(Double.MAX_VALUE, "" + Double.MAX_VALUE);
        expectToParse(Double.MIN_VALUE, "" + Double.MIN_VALUE);
        expectToParse(Double.MIN_NORMAL, "" + Double.MIN_NORMAL);
    }

    @Test
    public void testFloat() {
        expectToParse(.1f, ".1f");
        expectToParse(0.1f, "0.1f");
        expectToParse(0.f, "0.f");
        expectToParse(1e2f, "1e2f");
        expectToParse(.234e2f, ".234e2f");
        expectToParse(1.234e2f, "1.234e2f");
        expectToParse(001.234e2f, "001.234e2f");
        expectToParse(1.234e-2f, "1.234e-2f");
        expectToParse(001.234e+2f, "001.234e+2f");
        expectToParse(-001.234e-2f, "-001.234e-2f");
        expectToParse(+001.234e+2f, "+001.234e+2f");
        expectToParse(+001.234e+2f, "+001.234e+02f");
        expectToParse(+009.234e+2f, "+009.234e+02f");
    }

    @Test
    public void testInt() {
        expectToParse(0, "0");
        expectToParse(1, "1");
        expectToParse(12345, "12345");
        expectToParse(Integer.MAX_VALUE, "" + Integer.MAX_VALUE);
        expectToParse(Integer.MIN_VALUE, "" + Integer.MIN_VALUE);
        expectToParse(123, "1__2____3");
    }

    @Test
    public void testIntOctal() {
        expectToParse(01, "01");
        expectToParse(012345, "012345");
        expectToParse(Integer.MAX_VALUE, "0" + Integer.toOctalString(Integer.MAX_VALUE));
        expectToParse(Integer.MIN_VALUE, "0" + Integer.toOctalString(Integer.MIN_VALUE));
        expectToParse(0123, "01__2____3");
        expectToParse(0123, "0_1__2____3");
    }

    @Test
    public void testIntHex() {
        expectToParse(0x01, "0x01");
        expectToParse(0x012345, "0x012345");
        expectToParse(Integer.MAX_VALUE, "0x" + Integer.toHexString(Integer.MAX_VALUE));
        expectToParse(Integer.MIN_VALUE, "0x" + Integer.toHexString(Integer.MIN_VALUE));
        expectToParse(0x0123, "0x01__2____3");
        expectToParse(0x0123, "0x0_1__2____3");
    }

    @Test
    public void testIntBinary() {
        expectToParse(0x01, "0b01");
        expectToParse(0x012345, "0b000000010010001101000101");
        expectToParse(Integer.MAX_VALUE, "0x" + Integer.toHexString(Integer.MAX_VALUE));
        expectToParse(Integer.MIN_VALUE, "0x" + Integer.toHexString(Integer.MIN_VALUE));
        expectToParse(5, "0b01__0____1");
        expectToParse(5, "0b0_1__0____1");
    }

    @Test
    public void testLong() {
        expectToParse(1L, "1l");
        expectToParse(12345L, "12345L");
        expectToParse(Long.MAX_VALUE, Long.MAX_VALUE + "l");
        expectToParse(Long.MIN_VALUE, Long.MIN_VALUE + "L");
        expectToParse(123L, "1__2____3l");
    }

    @Test
    public void testLongOctal() {
        expectToParse(01l, "01l");
        expectToParse(012345L, "012345L");
        expectToParse(Long.MAX_VALUE, "0" + Long.toOctalString(Long.MAX_VALUE) + "l");
        expectToParse(Long.MIN_VALUE, "0" + Long.toOctalString(Long.MIN_VALUE) + "L");
        expectToParse(0123L, "01__2____3l");
        expectToParse(0123L, "0_1__2____3L");
    }

    @Test
    public void testLongHex() {
        expectToParse(0x01l, "0x01l");
        expectToParse(0x012345L, "0x012345L");
        expectToParse(Long.MAX_VALUE, "0x" + Long.toHexString(Long.MAX_VALUE) + "l");
        expectToParse(Long.MIN_VALUE, "0x" + Long.toHexString(Long.MIN_VALUE) + "L");
        expectToParse(0x0123L, "0x01__2____3l");
        expectToParse(0x0123L, "0x0_1__2____3L");
    }

    @Test
    public void testLongBinary() {
        expectToParse(0x01l, "0b01l");
        expectToParse(0x012345L, "0b000000010010001101000101L");
        expectToParse(Long.MAX_VALUE, "0x" + Long.toHexString(Long.MAX_VALUE) + "l");
        expectToParse(Long.MIN_VALUE, "0x" + Long.toHexString(Long.MIN_VALUE) + "L");
        expectToParse(5L, "0b01__0____1l");
        expectToParse(5L, "0b0_1__0____1L");
    }

    void expectToParseString(String expected) {
        expectToParseString(expected, expected);
    }

    void expectToParseString(String expected, String s) {
        expectToParse(expected, '"' + s + '"');
    }

    private void expectNotToParseString(String s) {
        expectNotToParse('"' + s + '"');
    }

    @Test
    public void testString() {
        expectToParseString("");
        expectToParseString("Hello, World!");
        expectToParseString("Hello\tWorld!", "Hello\\tWorld!");
        expectToParseString("Hello\bWorld!", "Hello\\bWorld!");
        expectToParseString("Hello\nWorld!", "Hello\\nWorld!");
        expectToParseString("Hello\rWorld!", "Hello\\rWorld!");
        expectToParseString("Hello\fWorld!", "Hello\\fWorld!");
        expectToParseString("Hello\'World!", "Hello\\'World!");
        expectToParseString("Hello\"World!", "Hello\\\"World!");
        expectToParseString("Hello\\World!", "Hello\\\\World!");
        expectToParseString("Hello\u0107World!", "Hello\\u0107World!");
        expectToParseString("Hello World!", "Hello\\040World!");
        expectToParseString("Hello World!", "Hello\\40World!");
        expectToParseString("Hello\32World!", "Hello\\32World!");
        expectNotToParseString("Hello\\u100");
        expectNotToParseString("Hello\\u10");
        expectNotToParseString("Hello\\u1");
        expectNotToParseString("Hello\\");
    }

}
