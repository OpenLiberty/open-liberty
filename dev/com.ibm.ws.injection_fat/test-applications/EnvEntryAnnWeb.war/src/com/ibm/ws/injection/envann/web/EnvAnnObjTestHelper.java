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
import static junit.framework.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.AssertionFailedError;

public class EnvAnnObjTestHelper {
    private static final String CLASS_NAME = EnvAnnObjTestHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants
    private static final String E_STRING = null;
    private static final Character E_CHAR = null;
    private static final Byte E_BYTE = null;
    private static final Short E_SHORT = null;
    private static final Integer E_INTEGER = null;
    private static final Long E_LONG = null;
    private static final Boolean E_BOOL = null;
    private static final Double E_DOUBLE = null;
    private static final Float E_FLOAT = null;

    private static InitialContext initCtx;

    public static String testEnvAnnObjInjection(String className, String name, Object expected, Object test) {
        assertEquals("The " + name + " was not the expected value", expected, test);
        assertFalse("The \"" + className + "/" + name + "\" was incorrectly found in the namespace", canLookup(className, name));
        return "PASS: The environment entry was successfully injected - " + name;
    }

    private static boolean canLookup(String className, String name) {
        try {
            initCtx.lookup("java:comp/env/" + className + "/" + name);
            return true;
        } catch (NamingException e) {
            return false;
        }
    }

    public static void processRequest(String className, String key, HashMap<String, Object> map) {
        if (map.isEmpty()) {
            fail("The map was empty. Impossible to test nothing.");
        }

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
                // Since the objects are expected to be null, simply testing if the
                // object is an instance of a class will not work. Instead, the names
                // have to be tested.
                if (name.contains("String")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_STRING);
                } else if (name.contains("Character")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_CHAR);
                } else if (name.contains("Byte")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_BYTE);
                } else if (name.contains("Short")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_SHORT);
                } else if (name.contains("Integer")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_INTEGER);
                } else if (name.contains("Long")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_LONG);
                } else if (name.contains("Boolean")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_BOOL);
                } else if (name.contains("Double")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_DOUBLE);
                } else if (name.contains("Float")) {
                    event = EnvAnnObjTestHelper.testEnvAnnObjInjection(className, name, test, E_FLOAT);
                }
            } catch (AssertionFailedError afe) {
                StringWriter sw = new StringWriter();
                afe.printStackTrace(new PrintWriter(sw));
                event = "FAIL:" + sw.toString();
            }

            WCEventTracker.addEvent(key, event);
        }
    }
}