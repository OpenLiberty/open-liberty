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
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.AssertionFailedError;

public class EnvMixObjTestHelper {
    private static final String CLASS_NAME = EnvMixObjTestHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants as defined in the XML
    private static final String E_STRING = "uebrigens";
    private static final Character E_CHARACTER = 'o';
    private static final Byte E_BYTE = 1;
    private static final Short E_SHORT = 1;
    private static final Integer E_INTEGER = 158;
    private static final Long E_LONG = 254L;
    private static final Boolean E_BOOL = true;
    private static final Double E_DOUBLE = 856.93D;
    private static final Float E_FLOAT = 548.72F;

    private static InitialContext initCtx;

    public static String testEnvMixObjInjection(String className, String name, Object expected, Object test) {
        assertEquals("The " + name + " was not the expected value", expected, test);
        testLookup(className, name, expected);
        return "PASS: The environment entry was successfully injected - " + name;
    }

    /**
     * Performs a global lookup on the given name.
     *
     * @param name Name to lookup
     * @return The Object that was returned from the lookup;
     */
    public static void testLookup(String className, String name, Object expected) {
        try {
            Object obj = initCtx.lookup("java:comp/env/" + className + "/" + name);
            assertNotNull("The " + name + " was not found in the namespace", obj);
            assertEquals("The " + name + " found in the lookup was not the expected value", expected, obj);
        } catch (NamingException e) {
            svLogger.info("There was an exception while performing the lookup");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void processRequest(String className, String key, HashMap<String, Object> map) {
        try {
            initCtx = new InitialContext();
        } catch (NamingException e) {
            svLogger.info("Error setting up the context");
            throw new RuntimeException(e);
        }

        Set<String> set = map.keySet();
        String event = "";

        for (String name : set) {
            try {
                Object test = map.get(name);
                if (test instanceof String) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_STRING);
                } else if (test instanceof Character) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_CHARACTER);
                } else if (test instanceof Byte) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_BYTE);
                } else if (test instanceof Short) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_SHORT);
                } else if (test instanceof Integer) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_INTEGER);
                } else if (test instanceof Long) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_LONG);
                } else if (test instanceof Boolean) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_BOOL);
                } else if (test instanceof Double) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_DOUBLE);
                } else if (test instanceof Float) {
                    event = EnvMixObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_FLOAT);
                }
            } catch (AssertionFailedError afe) {
                StringWriter sw = new StringWriter();
                afe.printStackTrace(new PrintWriter(sw));
                event = sw.toString();
            }

            WCEventTracker.addEvent(key, event);
        }
    }
}