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
package com.ibm.ws.injection.repeatable.envmix.web;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used as an in-memory event tracker for out-of-band listeners and filters.
 *
 * @author jrbauer
 *
 */
public class WCEventTracker {
    // Environment Entry Mixotated Primitives
    public static final String KEY_LISTENER_INIT_AdvRepeatableEnvMixPrimServletContextListener = "AdvRepeatableEnvMixPrimServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvRepeatableEnvMixPrimServletRequestListener = "AdvRepeatableEnvMixPrimServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvRepeatableEnvMixPrimHttpSessionListener = "AdvRepeatableEnvMixPrimHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvRepeatableEnvMixPrimContextAttributeListener = "AdvRepeatableEnvMixPrimContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvRepeatableEnvMixPrimContextAttributeListener = "AdvRepeatableEnvMixPrimContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvRepeatableEnvMixPrimContextAttributeListener = "AdvRepeatableEnvMixPrimContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvRepeatableEnvMixPrimContextAttributeListener = "AdvRepeatableEnvMixPrimContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvRepeatableEnvMixPrimServletRequestAttributeListener = "AdvRepeatableEnvMixPrimServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvRepeatableEnvMixPrimServletRequestAttributeListener = "AdvRepeatableEnvMixPrimServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvRepeatableEnvMixPrimServletRequestAttributeListener = "AdvRepeatableEnvMixPrimServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvRepeatableEnvMixPrimServletRequestAttributeListener = "AdvRepeatableEnvMixPrimServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvRepeatableEnvMixPrimHttpSessionAttributeListener = "AdvRepeatableEnvMixPrimHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvRepeatableEnvMixPrimHttpSessionAttributeListener = "AdvRepeatableEnvMixPrimHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvRepeatableEnvMixPrimHttpSessionAttributeListener = "AdvRepeatableEnvMixPrimHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvRepeatableEnvMixPrimHttpSessionAttributeListener = "AdvRepeatableEnvMixPrimHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvRepeatableEnvMixObjFilter = "AdvRepeatableEnvMixObjFilter:DoFilter";

    // Environment Entry Mixotated Primitives
    public static final String KEY_LISTENER_INIT_AdvRepeatableEnvMixObjServletContextListener = "AdvRepeatableEnvMixObjServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvRepeatableEnvMixObjServletRequestListener = "AdvRepeatableEnvMixObjServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvRepeatableEnvMixObjHttpSessionListener = "AdvRepeatableEnvMixObjHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvRepeatableEnvMixObjContextAttributeListener = "AdvRepeatableEnvMixObjContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvRepeatableEnvMixObjContextAttributeListener = "AdvRepeatableEnvMixObjContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvRepeatableEnvMixObjContextAttributeListener = "AdvRepeatableEnvMixObjContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvRepeatableEnvMixObjContextAttributeListener = "AdvRepeatableEnvMixObjContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvRepeatableEnvMixObjServletRequestAttributeListener = "AdvRepeatableEnvMixObjServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvRepeatableEnvMixObjServletRequestAttributeListener = "AdvRepeatableEnvMixObjServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvRepeatableEnvMixObjServletRequestAttributeListener = "AdvRepeatableEnvMixObjServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvRepeatableEnvMixObjServletRequestAttributeListener = "AdvRepeatableEnvMixObjServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvRepeatableEnvMixObjHttpSessionAttributeListener = "AdvRepeatableEnvMixObjHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvRepeatableEnvMixObjHttpSessionAttributeListener = "AdvRepeatableEnvMixObjHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvRepeatableEnvMixObjHttpSessionAttributeListener = "AdvRepeatableEnvMixObjHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvRepeatableEnvMixObjHttpSessionAttributeListener = "AdvRepeatableEnvMixObjHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvRepeatableEnvMixPrimFilter = "AdvRepeatableEnvMixPrimFilter:DoFilter";

    private static ConcurrentHashMap<String, Vector<String>> chm = new ConcurrentHashMap<String, Vector<String>>();

    public static void addEvent(String key, String event) {
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