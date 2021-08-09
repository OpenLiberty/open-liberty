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
package com.ibm.ws.injection.transaction.web;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used as an in-memory event tracker for out-of-band listeners and filters.
 *
 * @author jrbauer
 *
 */
public class WCEventTracker {
    public static final String KEY_LISTENER_INIT_AdvTransactionServletContextListener = "AdvTransactionServletContextListener";
    public static final String KEY_LISTENER_INIT_AdvTransactionServletRequestListener = "AdvTransactionServletRequestListener";
    public static final String KEY_LISTENER_CREATED_AdvTransactionHttpSessionListener = "AdvTransactionHttpSessionListener";
    public static final String KEY_LISTENER_ADD_AdvTransactionContextAttributeListener = "AdvTransactionContextAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvTransactionContextAttributeListener = "AdvTransactionContextAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvTransactionContextAttributeListener = "AdvTransactionContextAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvTransactionContextAttributeListener = "AdvTransactionContextAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvTransactionServletRequestAttributeListener = "AdvTransactionServletRequestAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvTransactionServletRequestAttributeListener = "AdvTransactionServletRequestAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvTransactionServletRequestAttributeListener = "AdvTransactionServletRequestAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvTransactionServletRequestAttributeListener = "AdvTransactionServletRequestAttributeListener:Attribute";
    public static final String KEY_LISTENER_ADD_AdvTransactionHttpSessionAttributeListener = "AdvTransactionHttpSessionAttributeListener:Added";
    public static final String KEY_LISTENER_REP_AdvTransactionHttpSessionAttributeListener = "AdvTransactionHttpSessionAttributeListener:Replaced";
    public static final String KEY_LISTENER_DEL_AdvTransactionHttpSessionAttributeListener = "AdvTransactionHttpSessionAttributeListener:Removed";
    public static final String KEY_ATTRIBUTE_AdvTransactionHttpSessionAttributeListener = "AdvTransactionHttpSessionAttributeListener:Attribute";
    public static final String KEY_FILTER_DOFILTER_AdvTransactionFilter = "AdvTransactionFilter:DoFilter";

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