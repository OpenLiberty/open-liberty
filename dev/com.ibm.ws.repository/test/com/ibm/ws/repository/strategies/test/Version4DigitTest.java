/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.strategies.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.osgi.framework.Version;

import com.ibm.ws.repository.strategies.writeable.Version4Digit;

/**
 * Test the Version4Digit class
 */
public class Version4DigitTest {

    @Test
    public void testVersionVersusVersion4Digit() {

        try {
            new Version("8.5.5.8+");
            fail("Should not be able to create a Version with '+' in the qualifier");
        } catch (IllegalArgumentException e) {
            // expected
        }
        new Version4Digit("8.5.5.8+");
    }

    @Test
    public void testVersion4DigitConstructorVersion() {

        Version v = new Version("1.2.3.10");
        Version4Digit v4 = new Version4Digit(v);

        assertEquals("major wrong: ", 1, v4.getMajor());
        assertEquals("minor wrong: ", 2, v4.getMinor());
        assertEquals("micro wrong: ", 3, v4.getMicro());
        assertEquals("qualifier wrong: ", "10", v4.getQualifier());

        v = new Version("1.2.3.fp_1");
        v4 = new Version4Digit(v);
        assertEquals("qualifier wrong: ", "fp_1", v4.getQualifier());
    }

    @Test
    public void testVersion4DigitConstructorString() {

        Version4Digit v4 = new Version4Digit("1.2.3.10");
        assertEquals("major wrong: ", 1, v4.getMajor());
        assertEquals("minor wrong: ", 2, v4.getMinor());
        assertEquals("micro wrong: ", 3, v4.getMicro());
        assertEquals("qualifier wrong: ", "10", v4.getQualifier());

        v4 = new Version4Digit("1.2.3.fp_1");
        assertEquals("qualifier wrong: ", "fp_1", v4.getQualifier());

        v4 = new Version4Digit("1.2.3.4+");
        assertEquals("qualifier wrong: ", "4+", v4.getQualifier());

        try {
            v4 = new Version4Digit("1.2.4+");
            fail("should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testVersion4DigitConstructor4Digit() {
        Version4Digit v4;

        v4 = new Version4Digit(8, 5, 5, "5");
        assertEquals("Testing 4digit constructor for 8555: ", "8.5.5.5", v4.toString());

        v4 = new Version4Digit(8, 5, 5, "5+");
        assertEquals("Testing 4digit constructor for 8555+: ", "8.5.5.5+", v4.toString());

        v4 = new Version4Digit(Integer.MAX_VALUE, 0, 0, "0");
        assertEquals("Testing 4digit constructor for max value: ", Integer.MAX_VALUE + ".0.0.0", v4.toString());
    }

    @Test
    public void testVersion4DigitConstructor3Digit() {

        Version4Digit v4a, v4b;

        v4a = new Version4Digit("1.2.9");
        v4b = new Version4Digit("1.2.10");
        assertEquals("wrong is higher: ", v4b, Version4Digit.getHigherVersion(v4a, v4b));

        v4a = new Version4Digit("1.2.3");
        assertEquals("qualifier wrong: ", "", v4a.getQualifier());
        v4b = new Version4Digit("1.2.3.1");
        assertEquals("wrong version is higher: ", v4b, Version4Digit.getHigherVersion(v4a, v4b));
    }

    @Test
    public void testVersion4DigitCompareTo() {

        Version4Digit v4a, v4b;

        v4a = new Version4Digit("8.5.5.5");
        v4b = new Version4Digit("8.5.5.5");
        assertEquals("Should be equal: ", 0, v4a.compareTo(v4b));

        v4a = new Version4Digit("8.5.5.5");
        v4b = new Version4Digit("8.5.5.6");
        assertTrue("Should have returned <0", (v4a.compareTo(v4b) < 0));

        v4a = new Version4Digit("8.5.5.6");
        v4b = new Version4Digit("8.5.5.5");
        assertTrue("Should have returned >0", (v4a.compareTo(v4b) > 0));

        v4a = new Version4Digit("8.5.5.9");
        v4b = new Version4Digit("8.5.5.10");
        assertTrue("Should have returned <0", (v4a.compareTo(v4b) < 0));

        v4a = new Version4Digit("8.5.5.fp_1");
        v4b = new Version4Digit("8.5.5.fp_2");
        assertTrue("Should have returned <0", (v4a.compareTo(v4b) < 0));

        v4a = new Version4Digit("8.5.5.4+");
        v4b = new Version4Digit("8.5.5.5+");
        assertTrue("Should have returned <0", (v4a.compareTo(v4b) < 0));

        v4a = new Version4Digit("8.5.5.9");
        v4b = new Version4Digit("8.5.5.9+");
        assertTrue("Should have returned <0", (v4a.compareTo(v4b) < 0));

        // test unexpected case, not for the sense of the return but to ensure it does not explode
        v4a = new Version4Digit("8.5.5.9");
        v4b = new Version4Digit("8.5.5.fp_1");
        assertTrue("Should have returned <0", (v4a.compareTo(v4b) < 0));
    }

    @Test
    public void testVersion4DigitGetHigherVersion() {

        Version4Digit v4a, v4b;

        v4a = new Version4Digit("8.5.5.8");
        v4b = new Version4Digit("8.5.5.9");
        assertEquals("wrong returned as higher: ", v4b, Version4Digit.getHigherVersion(v4a, v4b));

        v4a = new Version4Digit("8.5.5.9");
        v4b = new Version4Digit("8.5.5.10");
        assertEquals("wrong returned as higher: ", v4b, Version4Digit.getHigherVersion(v4a, v4b));

        v4a = new Version4Digit("8.5.5.5+");
        v4b = new Version4Digit("8.5.5.9+");
        assertEquals("wrong returned as higher: ", v4b, Version4Digit.getHigherVersion(v4a, v4b));

        v4a = new Version4Digit("8.5.5.9");
        v4b = new Version4Digit("8.5.5.9+");
        assertEquals("The plus version should be higher: ", v4b, Version4Digit.getHigherVersion(v4a, v4b));

        v4a = new Version4Digit("8.5.5.9+");
        v4b = new Version4Digit("8.5.5.9");
        assertEquals("The plus version should be higher: ", v4a, Version4Digit.getHigherVersion(v4a, v4b));

        v4a = new Version4Digit("8.5.5.fp_1");
        v4b = new Version4Digit("8.5.5.fp_2");
        assertEquals("wrong returned as higher: ", v4b, Version4Digit.getHigherVersion(v4a, v4b));
    }

    @Test
    public void testVersion4DigitEquals() {

        Version4Digit v4a, v4b;

        v4a = new Version4Digit(new Version("1.2.3.10"));
        v4b = new Version4Digit("1.2.3.10");
        assertEquals("not equals and should be: ", true, v4a.equals(v4b));
        v4b = new Version4Digit("1.2.3.fp_1");
        assertEquals("equals and shouldn't be: ", false, v4a.equals(v4b));

    }

    @Test
    public void testVersion4DigitToString() {

        Version4Digit v4;

        v4 = new Version4Digit("1.2.3.10");
        assertEquals("wrong string: ", "1.2.3.10", v4.toString());
        v4 = new Version4Digit("1.2.3.fp_1");
        assertEquals("wrong string: ", "1.2.3.fp_1", v4.toString());
        v4 = new Version4Digit("1.2.3");
        assertEquals("wrong string: ", "1.2.3", v4.toString());

    }
}
