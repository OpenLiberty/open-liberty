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

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used as an in-memory event tracker for out-of-band listeners and filters.
 *
 * @author jrbauer
 *
 */
public class WCEventTracker {
    // Environment Entry Annotated Primitives
    public static final String KEY_LISTENER_INIT_AdvEnvAnnPrimServletContextListener = "AdvEnvAnnPrimServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvEnvAnnPrimServletRequestListener = "AdvEnvAnnPrimServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvEnvAnnPrimHttpSessionListener = "AdvEnvAnnPrimHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvEnvAnnPrimContextAttributeListener = "AdvEnvAnnPrimContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvAnnPrimContextAttributeListener = "AdvEnvAnnPrimContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvAnnPrimContextAttributeListener = "AdvEnvAnnPrimContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvAnnPrimContextAttributeListener = "AdvEnvAnnPrimContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvAnnPrimServletRequestAttributeListener = "AdvEnvAnnPrimServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvAnnPrimServletRequestAttributeListener = "AdvEnvAnnPrimServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvAnnPrimServletRequestAttributeListener = "AdvEnvAnnPrimServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvAnnPrimServletRequestAttributeListener = "AdvEnvAnnPrimServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvAnnPrimHttpSessionAttributeListener = "AdvEnvAnnPrimHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvAnnPrimHttpSessionAttributeListener = "AdvEnvAnnPrimHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvAnnPrimHttpSessionAttributeListener = "AdvEnvAnnPrimHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvAnnPrimHttpSessionAttributeListener = "AdvEnvAnnPrimHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvEnvAnnObjFilter = "AdvEnvAnnObjFilter:DoFilter";

    // Environment Entry Annotated Primitives
    public static final String KEY_LISTENER_INIT_AdvEnvAnnObjServletContextListener = "AdvEnvAnnObjServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvEnvAnnObjServletRequestListener = "AdvEnvAnnObjServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvEnvAnnObjHttpSessionListener = "AdvEnvAnnObjHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvEnvAnnObjContextAttributeListener = "AdvEnvAnnObjContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvAnnObjContextAttributeListener = "AdvEnvAnnObjContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvAnnObjContextAttributeListener = "AdvEnvAnnObjContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvAnnObjContextAttributeListener = "AdvEnvAnnObjContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvAnnObjServletRequestAttributeListener = "AdvEnvAnnObjServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvAnnObjServletRequestAttributeListener = "AdvEnvAnnObjServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvAnnObjServletRequestAttributeListener = "AdvEnvAnnObjServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvAnnObjServletRequestAttributeListener = "AdvEnvAnnObjServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvEnvAnnObjHttpSessionAttributeListener = "AdvEnvAnnObjHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvEnvAnnObjHttpSessionAttributeListener = "AdvEnvAnnObjHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvEnvAnnObjHttpSessionAttributeListener = "AdvEnvAnnObjHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvEnvAnnObjHttpSessionAttributeListener = "AdvEnvAnnObjHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvEnvAnnPrimFilter = "AdvEnvAnnPrimFilter:DoFilter";

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