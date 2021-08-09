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

    // boolean tests
    @Test public void testBoolean_True()                                        { expectToParse(true, "true"); }
    @Test public void testBoolean_False()                                       { expectToParse(false, "false"); }

    // character tests: success cases
    @Test public void testChar_Null()                                           { expectToParse('\0', "'\\0'");}
    @Test public void testChar_a()                                              { expectToParse('a', "'a'");}
    @Test public void testChar_unicode()                                        { expectToParse('\u0100', "'\\u0100'");}
    @Test public void testChar_octal()                                          { expectToParse('\100', "'\\100'");}
    // character tests: error cases
    @Test public void testChar_epmtyUnclosed()                                  { expectNotToParse("'"); }
    @Test public void testChar_empty()                                          { expectNotToParse("''"); }
    @Test public void testChar_tooLong()                                        { expectNotToParse("'..'"); }
    @Test public void testChar_unfinishedEscape()                               { expectNotToParse("'\\'"); }
    @Test public void testChar_unfinishedUnclosed()                             { expectNotToParse("'\\"); }
    @Test public void testChar_tooLongUnclosed()                                { expectNotToParse("'x\\"); }

    // double tests
    @Test public void testDouble_0D()                                           { expectToParse(0D, "0D"); }
    @Test public void testDouble_leadingDot()                                   { expectToParse(.1, ".1"); }
    @Test public void testDouble_decimal()                                      { expectToParse(0.1, "0.1"); }
    @Test public void testDouble_trailingDot()                                  { expectToParse(0., "0."); }
    @Test public void testDouble_leadingDotDSuffix()                            { expectToParse(.1D, ".1D"); }
    @Test public void testDouble_decimalDSuffix()                               { expectToParse(0.1D, ".1D"); }
    @Test public void testDouble_engineering()                                  { expectToParse(1e2, "1e2"); }
    @Test public void testDouble_leadingDotEngineering()                        { expectToParse(.234e2, ".234e2"); }
    @Test public void testDouble_decimalEngineering()                           { expectToParse(1.234e2, "1.234e2"); }
    @Test public void testDouble_leadingZeroes()                                { expectToParse(001.234e2, "001.234e2"); }
    @Test public void testDouble_negativeExponent()                             { expectToParse(1.234e-2, "1.234e-2"); }
    @Test public void testDouble_positiveExponent()                             { expectToParse(001.234e+2, "001.234e+2"); }
    @Test public void testDouble_negativeMantissaNegativeExponent()             { expectToParse(-001.234e-2, "-001.234e-2"); }
    @Test public void testDouble_positiveMantissaPositiveExponent()             { expectToParse(+001.234e+2, "+001.234e+2"); }
    @Test public void testDouble_leadingZeroExponent()                          { expectToParse(+001.234e+02, "+001.234e+02"); }
    @Test public void testDouble_largerMantissa()                               { expectToParse(+009.234e+02, "+009.234e+02"); }
    @Test public void testDouble_maxDouble()                                    { expectToParse(Double.MAX_VALUE, "" + Double.MAX_VALUE); }
    @Test public void testDouble_minDouble()                                    { expectToParse(Double.MIN_VALUE, "" + Double.MIN_VALUE); }
    @Test public void testDouble_minNormalDouble()                              { expectToParse(Double.MIN_NORMAL, "" + Double.MIN_NORMAL); }

    // float tests
    @Test public void testFloat_0F()                                            { expectToParse(0F, "0F"); }
    @Test public void testFloat_leadingDot()                                    { expectToParse(.1f, ".1f"); }
    @Test public void testFloat_decimal()                                       { expectToParse(0.1f, "0.1f"); }
    @Test public void testFloat_trailingDot()                                   { expectToParse(0.f, "0.f"); }
    @Test public void testFloat_engineering()                                   { expectToParse(1e2f, "1e2f"); }
    @Test public void testFloat_leadingDotEngineering()                         { expectToParse(.234e2f, ".234e2f"); }
    @Test public void testFloat_decimalEngineering()                            { expectToParse(1.234e2f, "1.234e2f"); }
    @Test public void testFloat_leadingZeroEngineering()                        { expectToParse(001.234e2f, "001.234e2f"); }
    @Test public void testFloat_negativeExponent()                              { expectToParse(1.234e-2f, "1.234e-2f"); }
    @Test public void testFloat_positiveExponent()                              { expectToParse(001.234e+2f, "001.234e+2f"); }
    @Test public void testFloat_negativeMantissaNegativeExponent()              { expectToParse(-001.234e-2f, "-001.234e-2f"); }
    @Test public void testFloat_positiveMantissaPositiveExponent()              { expectToParse(+001.234e+2f, "+001.234e+2f"); }
    @Test public void testFloat_leadingZeroExponent()                           { expectToParse(+001.234e+02f, "+001.234e+02f"); }
    @Test public void testFloat_largerMantissa()                                { expectToParse(+009.234e+02f, "+009.234e+02f"); }

    // int tests: denary
    @Test public void testInt_0()                                               { expectToParse(0, "0"); }
    @Test public void testInt_1()                                               { expectToParse(1, "1"); }
    @Test public void testInt_12345()                                           { expectToParse(12345, "12345"); }
    @Test public void testInt_maxInt()                                          { expectToParse(Integer.MAX_VALUE, "" + Integer.MAX_VALUE); }
    @Test public void testInt_minInt()                                          { expectToParse(Integer.MIN_VALUE, "" + Integer.MIN_VALUE); }
    @Test public void testInt_underscores()                                     { expectToParse(123, "1__2____3"); }
    // int tests: octal
    @Test public void testIntOctal_01()                                         { expectToParse(01, "01"); }
    @Test public void testIntOctal_012345()                                     { expectToParse(012345, "012345"); }
    @Test public void testIntOctal_maxInt()                                     { expectToParse(Integer.MAX_VALUE, "0" + Integer.toOctalString(Integer.MAX_VALUE)); }
    @Test public void testIntOctal_minInt()                                     { expectToParse(Integer.MIN_VALUE, "0" + Integer.toOctalString(Integer.MIN_VALUE)); }
    @Test public void testIntOctal_underscores()                                { expectToParse(0123, "01__2____3"); }
    @Test public void testIntOctal_leadingUnderscore()                          { expectToParse(0123, "0_1__2____3"); }
    // int tests: hex
    @Test public void testIntHex_0x01()                                         { expectToParse(0x01, "0x01"); }
    @Test public void testIntHex_0x012345()                                     { expectToParse(0x012345, "0x012345"); }
    @Test public void testIntHex_maxInt()                                       { expectToParse(Integer.MAX_VALUE, "0x" + Integer.toHexString(Integer.MAX_VALUE)); }
    @Test public void testIntHex_minInt()                                       { expectToParse(Integer.MIN_VALUE, "0x" + Integer.toHexString(Integer.MIN_VALUE)); }
    @Test public void testIntHex_underscores()                                  { expectToParse(0x0123, "0x01__2____3"); }
    // int tests: binary
    @Test public void testIntBinary_0b01()                                      { expectToParse(0x01, "0b01"); }
    @Test public void testIntBinary_0b000000010010001101000101()                { expectToParse(0x012345, "0b000000010010001101000101"); }
    @Test public void testIntBinary_maxInt()                                    { expectToParse(Integer.MAX_VALUE, "0x" + Integer.toHexString(Integer.MAX_VALUE)); }
    @Test public void testIntBinary_minInt()                                    { expectToParse(Integer.MIN_VALUE, "0x" + Integer.toHexString(Integer.MIN_VALUE)); }
    @Test public void testIntBinary_underscores()                               { expectToParse(5, "0b01__0____1"); }

    // long tests: denary
    @Test public void testLong_0L()                                             { expectToParse(0L, "0L"); }
    @Test public void testLong_1l()                                             { expectToParse(1L, "1l"); }
    @Test public void testLong_12345L()                                         { expectToParse(12345L, "12345L"); }
    @Test public void testLong_maxInt()                                         { expectToParse(Long.MAX_VALUE, Long.MAX_VALUE + "l"); }
    @Test public void testLong_minInt()                                         { expectToParse(Long.MIN_VALUE, Long.MIN_VALUE + "L"); }
    @Test public void testLong_underscores()                                    { expectToParse(123L, "1__2____3l"); }
    // long tests: octal
    @Test public void testLongOctal_01l()                                       { expectToParse(01l, "01l"); }
    @Test public void testLongOctal_012345L()                                   { expectToParse(012345L, "012345L"); }
    @Test public void testLongOctal_maxLong()                                   { expectToParse(Long.MAX_VALUE, "0" + Long.toOctalString(Long.MAX_VALUE) + "l"); }
    @Test public void testLongOctal_minLong()                                   { expectToParse(Long.MIN_VALUE, "0" + Long.toOctalString(Long.MIN_VALUE) + "L"); }
    @Test public void testLongOctal_underscores()                               { expectToParse(0123L, "01__2____3l"); }
    @Test public void testLongOctal_leadingUnderscores()                        { expectToParse(0123L, "0_1__2____3L"); }
    // long tests: hex
    @Test public void testLongHex_0x01l()                                       { expectToParse(0x01l, "0x01l"); }
    @Test public void testLongHex_0x012345L()                                   { expectToParse(0x012345L, "0x012345L"); }
    @Test public void testLongHex_maxLong()                                     { expectToParse(Long.MAX_VALUE, "0x" + Long.toHexString(Long.MAX_VALUE) + "l"); }
    @Test public void testLongHex_minLong()                                     { expectToParse(Long.MIN_VALUE, "0x" + Long.toHexString(Long.MIN_VALUE) + "L"); }
    @Test public void testLongHex_underscores()                                 { expectToParse(0x0123L, "0x01__2____3l"); }
    // long tests: binary
    @Test public void testLongBinary_0b01l()                                    { expectToParse(0x01l, "0b01l"); }
    @Test public void testLongBinary_0b000000010010001101000101L()              { expectToParse(0x012345L, "0b000000010010001101000101L"); }
    @Test public void testLongBinary_maxLong()                                  { expectToParse(Long.MAX_VALUE, "0x" + Long.toHexString(Long.MAX_VALUE) + "l"); }
    @Test public void testLongBinary_minLong()                                  { expectToParse(Long.MIN_VALUE, "0x" + Long.toHexString(Long.MIN_VALUE) + "L"); }
    @Test public void testLongBinary_underscores()                              { expectToParse(5L, "0b01__0____1l"); }

    // String tests: success cases
    @Test public void testString_empty()                                        { expectToParseString(""); }
    @Test public void testString_hello()                                        { expectToParseString("Hello, World!"); }
    @Test public void testString_tab()                                          { expectToParseString("Hello\tWorld!", "Hello\\tWorld!"); }
    @Test public void testString_backspace()                                    { expectToParseString("Hello\bWorld!", "Hello\\bWorld!"); }
    @Test public void testString_newline()                                      { expectToParseString("Hello\nWorld!", "Hello\\nWorld!"); }
    @Test public void testString_carriageReturn()                               { expectToParseString("Hello\rWorld!", "Hello\\rWorld!"); }
    @Test public void testString_formfeed()                                     { expectToParseString("Hello\fWorld!", "Hello\\fWorld!"); }
    @Test public void testString_singleQuote()                                  { expectToParseString("Hello\'World!", "Hello\\'World!"); }
    @Test public void testString_doubleQuote()                                  { expectToParseString("Hello\"World!", "Hello\\\"World!"); }
    @Test public void testString_backslash()                                    { expectToParseString("Hello\\World!", "Hello\\\\World!"); }
    @Test public void testString_unicodeEscape()                                { expectToParseString("Hello\u0107World!", "Hello\\u0107World!"); }
    @Test public void testString_3digitOctalEscape()                            { expectToParseString("Hello\040World!", "Hello\\040World!"); }
    @Test public void testString_2digitOctalEscape()                            { expectToParseString("Hello\40World!", "Hello\\40World!"); }
    @Test public void testString_octalSpecialChar()                             { expectToParseString("Hello\32World!", "Hello\\32World!"); }
    // String tests: error cases
    @Test public void testString_3digitUnicodeEscape()                          { expectNotToParseString("Hello\\u100"); }
    @Test public void testString_2digitUnicodeEscape()                          { expectNotToParseString("Hello\\u10"); }
    @Test public void testString_1digitUnicodeEscape()                          { expectNotToParseString("Hello\\u1"); }
    @Test public void testString_unfinishedEscape()                             { expectNotToParseString("Hello\\"); }


    private void expectNotToParse(String value) {
        expectToParse(value, value);
    }

    private void expectToParse(Object expected, String str) {
        Object actual = LiteralParser.parse(str);
        assertNotNull("parse shoud not return null", actual);
        assertEquals("Comparing types for equality", expected.getClass(), actual.getClass());
        if (expected.equals(Character.valueOf('\0')))
            assertEquals("Comparing values for equality", (int) (Character) expected, (int) (Character) actual);
        assertEquals("Comparing values for equality", expected, actual);
    }

    private void expectToParseString(String expected) {
        expectToParseString(expected, expected);
    }

    private void expectToParseString(String expected, String s) {
        expectToParse(expected, '"' + s + '"');
    }

    private void expectNotToParseString(String s) {
        expectNotToParse('"' + s + '"');
    }
}
