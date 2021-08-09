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

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.AssertionFailedError;

public class EnvXMLObjTestHelper {
    private static final String CLASS_NAME = EnvXMLObjTestHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants as defined in the XML
    private static final String E_STRING = "uebrigens";
    private static final Character E_CHARACTER = 'a';
    private static final Byte E_BYTE = 1;
    private static final Short E_SHORT = 1;
    private static final Integer E_INTEGER = 5;
    private static final Long E_LONG = 100L;
    private static final Boolean E_BOOL = true;
    private static final Double E_DOUBLE = 100.0D;
    private static final Float E_FLOAT = 100.0F;

    private static InitialContext initCtx;

    public static String testEnvMixObjInjection(String className, String name, Object expected, Object test) {
        try {
            assertEquals("The " + name + " was not the expected value", expected, test);
            assertNotNull("The " + name + " was incorrectly found in the namespace", lookup(className, name));
            return "PASS: The environment entry was successfully injected - " + name;
        } catch (Throwable t) {
            if (t instanceof AssertionFailedError) {
                return "FAIL:" + t.getMessage();
            }

            throw new RuntimeException("The was an error while testing the injected environment objects", t);
        }
    }

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
            Object test = map.get(name);
            if (test instanceof String) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_STRING);
            } else if (test instanceof Character) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_CHARACTER);
            } else if (test instanceof Byte) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_BYTE);
            } else if (test instanceof Short) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_SHORT);
            } else if (test instanceof Integer) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_INTEGER);
            } else if (test instanceof Long) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_LONG);
            } else if (test instanceof Boolean) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_BOOL);
            } else if (test instanceof Double) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_DOUBLE);
            } else if (test instanceof Float) {
                event = EnvXMLObjTestHelper.testEnvMixObjInjection(className, name, map.get(name), E_FLOAT);
            }

            WCEventTracker.addEvent(key, event);
        }
    }
}