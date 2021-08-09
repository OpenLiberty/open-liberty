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

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used as an in-memory event tracker for out-of-band listeners and filters.
 *
 * @author jrbauer
 *
 */
public class WCEventTracker {
    // Environment Entry XMLotated Primitives
    public static final String KEY_LISTENER_INIT_AdvEnvXMLPrimServletContextListener = "AdvEnvXMLPrimServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvEnvXMLPrimServletRequestListener = "AdvEnvXMLPrimServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvEnvXMLPrimHttpSessionListener = "AdvEnvXMLPrimHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvEnvXMLPrimContextAttributeListener = "AdvEnvXMLPrimContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvXMLPrimContextAttributeListener = "AdvEnvXMLPrimContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvXMLPrimContextAttributeListener = "AdvEnvXMLPrimContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvXMLPrimContextAttributeListener = "AdvEnvXMLPrimContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvXMLPrimServletRequestAttributeListener = "AdvEnvXMLPrimServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvXMLPrimServletRequestAttributeListener = "AdvEnvXMLPrimServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvXMLPrimServletRequestAttributeListener = "AdvEnvXMLPrimServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvXMLPrimServletRequestAttributeListener = "AdvEnvXMLPrimServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvXMLPrimHttpSessionAttributeListener = "AdvEnvXMLPrimHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvXMLPrimHttpSessionAttributeListener = "AdvEnvXMLPrimHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvXMLPrimHttpSessionAttributeListener = "AdvEnvXMLPrimHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvXMLPrimHttpSessionAttributeListener = "AdvEnvXMLPrimHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvEnvXMLObjFilter = "AdvEnvXMLObjFilter:DoFilter";

    // Environment Entry XMLotated Primitives
    public static final String KEY_LISTENER_INIT_AdvEnvXMLObjServletContextListener = "AdvEnvXMLObjServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvEnvXMLObjServletRequestListener = "AdvEnvXMLObjServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvEnvXMLObjHttpSessionListener = "AdvEnvXMLObjHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvEnvXMLObjContextAttributeListener = "AdvEnvXMLObjContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvXMLObjContextAttributeListener = "AdvEnvXMLObjContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvXMLObjContextAttributeListener = "AdvEnvXMLObjContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvXMLObjContextAttributeListener = "AdvEnvXMLObjContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvXMLObjServletRequestAttributeListener = "AdvEnvXMLObjServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvXMLObjServletRequestAttributeListener = "AdvEnvXMLObjServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvXMLObjServletRequestAttributeListener = "AdvEnvXMLObjServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvXMLObjServletRequestAttributeListener = "AdvEnvXMLObjServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvXMLObjHttpSessionAttributeListener = "AdvEnvXMLObjHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvXMLObjHttpSessionAttributeListener = "AdvEnvXMLObjHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvXMLObjHttpSessionAttributeListener = "AdvEnvXMLObjHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvXMLObjHttpSessionAttributeListener = "AdvEnvXMLObjHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvEnvXMLPrimFilter = "AdvEnvXMLPrimFilter:DoFilter";

    private static ConcurrentHashMap<String, Vector<String>> chm = new ConcurrentHashMap<String, Vector<String>>();

    public static void addEvent(String key, String event) {
        //  svLogger.info("Adding event: " + key + " - " + event);
        if (chm.containsKey(key)) {
            Vector<String> vec = chm.get(key);
            vec.add(event);
        } else {
            Vector<String> vec = new Vector<String>();
            vec.add(event);
            chm.put(key, vec);
        }
    }

    public static void clearEvents(String key) {
        chm.remove(key);
    }

    public static Vector<String> getEvents(String key) {
        Vector<String> vec = chm.get(key);
        return vec;
    }

    /**
     * Used to create a new event string.
     *
     * @param success - value is PASS or FAIL
     * @param message - message
     * @return - a new event string
     */
    public static String createEvent(String success, String message) {
        return success + ":" + message;
    }

    public static String[] splitEvent(String event) {
        String[] strArray = event.split(":");
        String[] retArray = new String[2];
        if (strArray.length >= 1) {
            retArray[0] = strArray[0];
        }

        if (strArray.length >= 2) {
            retArray[1] = strArray[1];
        }

        return retArray;
    }
}