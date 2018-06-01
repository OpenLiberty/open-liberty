/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.Timer;

public class TimerData {
    private final static Logger svLogger = Logger.getLogger(TimerData.class.getName());

    public static Map<String, TimerData> svIntEventMap = Collections.synchronizedMap(new HashMap<String, TimerData>());

    public static final long MAX_TIMER_WAIT = 60 * 1000; // 60 seconds

    boolean isFired;

    ArrayList<String> intEvents = new ArrayList<String>();

    static void addIntEvent(Timer t, String eventTag) {
        svLogger.info("--> Entered TimerData.addIntEvent");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);

            TimerData td = svIntEventMap.get(infoKey);

            svLogger.info("--> svIntEventMap = " + svIntEventMap);
            svLogger.info("--> TimerData td = " + td);

            if (td == null) {
                svLogger.info("--> td was null so we need to create it...");
                td = new TimerData();
                svLogger.info("--> new TimerData object created, td = " + td);
                svIntEventMap.put(infoKey, td);
                svLogger.info("--> svIntEventMap = " + svIntEventMap);
            }
            svLogger.info("-->td.isFired = " + td.isFired);
            if (!td.isFired) {
                svLogger.info("--> eventTag = " + eventTag);
                td.intEvents.add(eventTag);
                svLogger.info("--> td.intEvents.size() = " + td.intEvents.size()
                              + ", td.intEvents = " + td.intEvents);
            }
        } finally {
            svLogger.info("<-- Exiting TimerData.addIntEvent");
        }
    }

    public void setIsFired(boolean fired) {
        isFired = fired;
    }

    public ArrayList<String> getIntEvents() {
        svLogger.info("--> Entered TimerData.getIntEvents()");
        try {
            svLogger.info("--> intEvents = " + intEvents);
            return intEvents;
        } finally {
            svLogger.info("<-- Exiting TimerData.getIntEvents()");
        }
    }
}
