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
package com.ibm.ws.injection.envmix.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.AssertionFailedError;

public class EnvMixPrimTestHelper {
    private static final String CLASS_NAME = EnvMixPrimTestHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants as defined in the XML
    private static final char E_CHAR = 'o';
    private static final byte E_BYTE = 1;
    private static final short E_SHORT = 1;
    private static final int E_INTEGER = 158;
    private static final long E_LONG = 254L;
    private static final boolean E_BOOL = true;
    private static final double E_DOUBLE = 856.93D;
    private static final float E_FLOAT = 548.72F;

    private static InitialContext initCtx;

    /**
     * Performs a global lookup on the given name.
     *
     * @param name
     *            Name to lookup
     * @return The Object that was returned from the lookup;
     */
    public static Object lookup(String className, String name) {
        try {
            return initCtx.lookup("java:comp/env/" + className + "/" + name);
        } catch (NamingException e) {
            svLogger.info("There was an exception while performing the lookup");
            e.printStackTrace();
            return null;
        }
    }

    public static void testEnvMixPrimInjection(String className, String key, char tChar, byte tByte, short tShort, int tInt, long tLong, boolean tBool, double tDouble,
                                               float tFloat, String[] names) {
        try {
            initCtx = new InitialContext();
        } catch (NamingException e) {
            svLogger.info("Error setting up the context");
            throw new RuntimeException(e);
        }

        String event = "";
        try {
            event = testEnvMixPrimChar(className, names[0], tChar);
            WCEventTracker.addEvent(key, event);
            event = testEnvMixPrimByte(className, names[1], tByte);
            WCEventTracker.addEvent(key, event);
            event = testEnvMixPrimShort(className, names[2], tShort);
            WCEventTracker.addEvent(key, event);
            event = testEnvMixPrimInt(className, names[3], tInt);
            WCEventTracker.addEvent(key, event);
            event = testEnvMixPrimLong(className, names[4], tLong);
            WCEventTracker.addEvent(key, event);
            event = testEnvMixPrimBool(className, names[5], tBool);
            WCEventTracker.addEvent(key, event);
            event = testEnvMixPrimDouble(className, names[6], tDouble);
            WCEventTracker.addEvent(key, event);
            event = testEnvMixPrimFloat(className, names[7], tFloat);
            WCEventTracker.addEvent(key, event);
        } catch (AssertionFailedError afe) {
            StringWriter sw = new StringWriter();
            afe.printStackTrace(new PrintWriter(sw));
            event = "FAIL:" + sw.toString();
            WCEventTracker.addEvent(key, event);
        }
    }

    public static String testEnvMixPrimChar(String className, String name, char test) {
        assertEquals("The " + name + " was not the expected value", E_CHAR, test);
        testLookup(className, name, E_CHAR);
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvMixPrimByte(String className, String name, byte test) {
        assertEquals("The " + name + " was not the expected value", E_BYTE, test);
        testLookup(className, name, E_BYTE);
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvMixPrimShort(String className, String name, short test) {
        assertEquals("The " + name + " was not the expected value", E_SHORT, test);
        testLookup(className, name, E_SHORT);
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvMixPrimInt(String className, String name, int test) {
        assertEquals("The " + name + " was not the expected value", E_INTEGER, test);
        testLookup(className, name, E_INTEGER);
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvMixPrimLong(String className, String name, long test) {
        assertEquals("The " + name + " was not the expected value", E_LONG, test);
        testLookup(className, name, E_LONG);
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvMixPrimBool(String className, String name, boolean test) {
        assertEquals("The " + name + " was not the expected value", E_BOOL, test);
        testLookup(className, name, E_BOOL);
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvMixPrimDouble(String className, String name, double test) {
        assertEquals("The " + name + " was not the expected value", E_DOUBLE, test);
        testLookup(className, name, E_DOUBLE);
        return "PASS: The " + name + " was successfully injected.";
    }

    public static String testEnvMixPrimFloat(String className, String name, float test) {
        assertEquals("The " + name + " was not the expected value", E_FLOAT, test);
        testLookup(className, name, E_FLOAT);
        return "PASS: The " + name + " was successfully injected.";
    }

    public static void testLookup(String className, String name, Object expected) {
        Object obj = lookup(className, name);
        assertNotNull("The " + name + " was not found in the namespace", obj);
        assertEquals("The " + name + " was not the expected value", obj, expected);
    }
}