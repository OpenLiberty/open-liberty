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
package com.ibm.ws.injection.envxml.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.AssertionFailedError;

public class EnvXMLPrimTestHelper {
    private static final String CLASS_NAME = EnvXMLPrimTestHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants as defined in the XML
    private static final char E_CHAR = 'a';
    private static final byte E_BYTE = 1;
    private static final short E_SHORT = 1;
    private static final int E_INTEGER = 5;
    private static final long E_LONG = 100L;
    private static final boolean E_BOOL = true;
    private static final double E_DOUBLE = 100.0D;
    private static final float E_FLOAT = 100.0F;

    // These names should match the names of the variables being passed in for
    // testing (specifically their names in the web.xml). If not, the test will
    // fail to perform a JNDI lookup on them.
    private static String[] fieldNames = { "ifchar", "ifbyte", "ifshort", "ifint", "iflong", "ifboolean", "ifdouble", "iffloat" };
    private static String[] methodNames = { "imchar", "imbyte", "imshort", "imint", "imlong", "imboolean", "imdouble", "imfloat" };

    private static InitialContext initCtx;

    /**
     * Performs a global lookup on the given name.
     *
     * @param name Name to lookup
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

    public static void testEnvXMLPrimInjection(String className, String key, boolean isFieldInj, char tChar, byte tByte,
                                               short tShort, int tInt, long tLong, boolean tBool, double tDouble, float tFloat) {
        try {
            initCtx = new InitialContext();
        } catch (NamingException e) {
            svLogger.info("Error setting up the context");
            throw new RuntimeException(e);
        }

        String[] names = (isFieldInj) ? fieldNames : methodNames;
        String event = "";
        try {
            event = testEnvXMLPrimChar(className, names[0], tChar);
            WCEventTracker.addEvent(key, event);
            event = testEnvXMLPrimByte(className, names[1], tByte);
            WCEventTracker.addEvent(key, event);
            event = testEnvXMLPrimShort(className, names[2], tShort);
            WCEventTracker.addEvent(key, event);
            event = testEnvXMLPrimInt(className, names[3], tInt);
            WCEventTracker.addEvent(key, event);
            event = testEnvXMLPrimLong(className, names[4], tLong);
            WCEventTracker.addEvent(key, event);
            event = testEnvXMLPrimBool(className, names[5], tBool);
            WCEventTracker.addEvent(key, event);
            event = testEnvXMLPrimDouble(className, names[6], tDouble);
            WCEventTracker.addEvent(key, event);
            event = testEnvXMLPrimFloat(className, names[7], tFloat);
            WCEventTracker.addEvent(key, event);
        } catch (Throwable t) {
            if (t instanceof AssertionFailedError) {
                WCEventTracker.addEvent(key, "FAIL:" + t.getMessage());
            }

            throw new RuntimeException("The was an error while testing the injected environment objects", t);
        }
    }

    private static String testEnvXMLPrimChar(String className, String name, char test) {
        assertEquals("The " + name + " was not the expected value: " + test + "!=" + E_CHAR, E_CHAR, test);
        assertNotNull("The " + name + " was not found in the namespace", lookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    private static String testEnvXMLPrimByte(String className, String name, byte test) {
        assertEquals("The " + name + " was not the expected value", E_BYTE, test);
        assertNotNull("The " + name + " was not found in the namespace", lookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    private static String testEnvXMLPrimShort(String className, String name, short test) {
        assertEquals("The " + name + " was not the expected value", E_SHORT, test);
        assertNotNull("The " + name + " was not found in the namespace", lookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    private static String testEnvXMLPrimInt(String className, String name, int test) {
        assertEquals("The " + name + " was not the expected value", E_INTEGER, test);
        assertNotNull("The " + name + " was not found in the namespace", lookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    private static String testEnvXMLPrimLong(String className, String name, long test) {
        assertEquals("The " + name + " was not the expected value", E_LONG, test);
        assertNotNull("The " + name + " was not found in the namespace", lookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    private static String testEnvXMLPrimBool(String className, String name, boolean test) {
        assertEquals("The " + name + " was not the expected value", E_BOOL, test);
        assertNotNull("The " + name + " was not found in the namespace", lookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    private static String testEnvXMLPrimDouble(String className, String name, double test) {
        assertEquals("The " + name + " was not the expected value", E_DOUBLE, test);
        assertNotNull("The " + name + " was not found in the namespace", lookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }

    private static String testEnvXMLPrimFloat(String className, String name, float test) {
        assertEquals("The " + name + " was not the expected value", E_FLOAT, test);
        assertNotNull("The " + name + " was not found in the namespace", lookup(className, name));
        return "PASS: The " + name + " was successfully injected.";
    }
}