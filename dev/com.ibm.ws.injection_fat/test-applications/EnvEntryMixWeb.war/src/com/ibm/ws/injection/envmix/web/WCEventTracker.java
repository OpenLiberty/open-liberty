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
    public static final String KEY_LISTENER_INIT_AdvEnvMixPrimServletContextListener = "AdvEnvMixPrimServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvEnvMixPrimServletRequestListener = "AdvEnvMixPrimServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvEnvMixPrimHttpSessionListener = "AdvEnvMixPrimHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvEnvMixPrimContextAttributeListener = "AdvEnvMixPrimContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvMixPrimContextAttributeListener = "AdvEnvMixPrimContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvMixPrimContextAttributeListener = "AdvEnvMixPrimContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvMixPrimContextAttributeListener = "AdvEnvMixPrimContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvMixPrimServletRequestAttributeListener = "AdvEnvMixPrimServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvMixPrimServletRequestAttributeListener = "AdvEnvMixPrimServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvMixPrimServletRequestAttributeListener = "AdvEnvMixPrimServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvMixPrimServletRequestAttributeListener = "AdvEnvMixPrimServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvMixPrimHttpSessionAttributeListener = "AdvEnvMixPrimHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvMixPrimHttpSessionAttributeListener = "AdvEnvMixPrimHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvMixPrimHttpSessionAttributeListener = "AdvEnvMixPrimHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvMixPrimHttpSessionAttributeListener = "AdvEnvMixPrimHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvEnvMixObjFilter = "AdvEnvMixObjFilter:DoFilter";

    // Environment Entry Mixotated Primitives
    public static final String KEY_LISTENER_INIT_AdvEnvMixObjServletContextListener = "AdvEnvMixObjServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvEnvMixObjServletRequestListener = "AdvEnvMixObjServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvEnvMixObjHttpSessionListener = "AdvEnvMixObjHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvEnvMixObjContextAttributeListener = "AdvEnvMixObjContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvMixObjContextAttributeListener = "AdvEnvMixObjContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvMixObjContextAttributeListener = "AdvEnvMixObjContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvMixObjContextAttributeListener = "AdvEnvMixObjContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvMixObjServletRequestAttributeListener = "AdvEnvMixObjServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvMixObjServletRequestAttributeListener = "AdvEnvMixObjServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvMixObjServletRequestAttributeListener = "AdvEnvMixObjServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvMixObjServletRequestAttributeListener = "AdvEnvMixObjServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvMixObjHttpSessionAttributeListener = "AdvEnvMixObjHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvMixObjHttpSessionAttributeListener = "AdvEnvMixObjHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvMixObjHttpSessionAttributeListener = "AdvEnvMixObjHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvMixObjHttpSessionAttributeListener = "AdvEnvMixObjHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvEnvMixPrimFilter = "AdvEnvMixPrimFilter:DoFilter";

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