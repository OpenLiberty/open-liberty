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
package com.ibm.ws.injection.jpa.web;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used as an in-memory event tracker for out-of-band listeners and filters.
 *
 * @author jrbauer
 *
 */
public class WCEventTracker {
    public static final String KEY_LISTENER_INIT_AdvJPAPersistenceServletContextListener = "AdvJPAPersistenceServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvJPAPersistenceServletRequestListener = "AdvJPAPersistenceServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvJPAPersistenceHttpSessionListener = "AdvJPAPersistenceHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvJPAPersistenceContextAttributeListener = "AdvJPAPersistenceContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvJPAPersistenceContextAttributeListener = "AdvJPAPersistenceContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvJPAPersistenceContextAttributeListener = "AdvJPAPersistenceContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvJPAPersistenceContextAttributeListener = "AdvJPAPersistenceContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvJPAPersistenceServletRequestAttributeListener = "AdvJPAPersistenceServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvJPAPersistenceServletRequestAttributeListener = "AdvJPAPersistenceServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvJPAPersistenceServletRequestAttributeListener = "AdvJPAPersistenceServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvJPAPersistenceServletRequestAttributeListener = "AdvJPAPersistenceServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvJPAPersistenceHttpSessionAttributeListener = "AdvJPAPersistenceHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvJPAPersistenceHttpSessionAttributeListener = "AdvJPAPersistenceHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvJPAPersistenceHttpSessionAttributeListener = "AdvJPAPersistenceHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvJPAPersistenceHttpSessionAttributeListener = "AdvJPAPersistenceHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvJPAPersistenceFilter = "AdvJPAPersistenceFilter:DoFilter";

    private static ConcurrentHashMap<String, Vector<String>> chm = new ConcurrentHashMap<String, Vector<String>>();

    public static void addEvent(String key, String event) {
        // svLogger.info("Adding event: " + key + " - " + event);
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