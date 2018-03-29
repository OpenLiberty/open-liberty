/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.envann.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.AssertionFailedError;

public class EnvAnnPrimTestHelper {
    private static final String CLASS_NAME = EnvAnnPrimTestHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants
    private static final char E_CHAR = '\u0000';
    private static final byte E_BYTE = 0;
    private static final short E_SHORT = 0;
    private static final int E_INTEGER = 0;
    private static final long E_LONG = 0L;
    private static final boolean E_BOOL = false;
    private static final double E_DOUBLE = 0.0D;
    private static final float E_FLOAT = 0.0F;

    private static InitialContext initCtx;

    private static boolean canLookup(String className, String name) {
        try {
            initCtx.lookup("java:comp/env/" + className + "/" + name);
            return true;
        } catch (NamingException e) {
            return false;
        }
    }

    public static void testEnvAnnPrimInjection(String className, String key, char tChar, byte tByte, short tShort, int tInt,
                                               long tLong, boolean tBool, double tDouble, float tFloat, String[] names) {
        // Start of Method
        try {
            initCtx = new InitialContext();
        } catch (NamingException e) {
            svLogger.info("Error setting up the context");
            throw new RuntimeException(e);
        }

        String event = "";
        try {
            event = testEnvAnnPrimChar(className, names[0], tChar);
            WCEventTracker.addEvent(key, event);
            event = testEnvAnnPrimByte(className, names[1], tByte);
            WCEventTracker.addEvent(key, event);
            event = testEnvAnnPrimShort(className, names[2], tShort);
            WCEventTracker.addEvent(key, event);
            event = testEnvAnnPrimInt(className, names[3], tInt);
            WCEventTracker.addEvent(key, event);
            event = testEnvAnnPrimLong(className, names[4], tLong);
            WCEventTracker.addEvent(key, event);
            event = testEnvAnnPrimBool(className, names[5], tBool);
            WCEventTracker.addEvent(key, event);
            event = testEnvAnnPrimDouble(className, names[6], tDouble);
            WCEventTracker.addEvent(key, event);
            event = testEnvAnnPrimFloat(className, names[7], tFloat);
            WCEventTracker.addEvent(key, event);
        } catch (Throwable t) {
            if (t instanceof AssertionFailedError) {
                WCEventTracker.addEvent(key, "FAIL:" + t.getMessage());
            }

            throw new RuntimeException("The was an error while testing the injected environment objects", t);
        }
    }

    public static String testEnvAnnPrimChar(String className, String name, char test) {
        assertEquals("The " + name + " was not the expected value", E_CHAR, test);
        assertFalse("The " + name + " was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvAnnPrimByte(String className, String name, byte test) {
        assertEquals("The " + name + " was not the expected value", E_BYTE, test);
        assertFalse("The " + name + " was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvAnnPrimShort(String className, String name, short test) {
        assertEquals("The " + name + " was not the expected value", E_SHORT, test);
        assertFalse("The " + name + " was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvAnnPrimInt(String className, String name, int test) {
        assertEquals("The " + name + " was not the expected value", E_INTEGER, test);
        assertFalse("The " + name + " was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvAnnPrimLong(String className, String name, long test) {
        assertEquals("The " + name + " was not the expected value", E_LONG, test);
        assertFalse("The " + name + " was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvAnnPrimBool(String className, String name, boolean test) {
        assertEquals("The " + name + " was not the expected value", E_BOOL, test);
        assertFalse("The " + name + " was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvAnnPrimDouble(String className, String name, double test) {
        assertEquals("The " + name + " was not the expected value", E_DOUBLE, test);
        assertFalse("The " + name + " was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvAnnPrimFloat(String className, String name, float test) {
        assertEquals("The " + name + " was not the expected value", E_FLOAT, test);
        assertFalse("The " + name + " was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }
}