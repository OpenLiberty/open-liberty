/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogStartTime extends AccessLogData {

    // We're assuming that the methods below that use this start time will be called on the same thread
    private static ThreadLocal<Long> accessLogStartTime = new ThreadLocal<>();

    public AccessLogStartTime() {
        super("%t");
        // %T - Note the start time for the request
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {
        long startTime = getStartTime(response, request, data);

        if (startTime != 0) {
            Date startDate = new Date(startTime);
            String formattedDate = "[" + HttpDispatcher.getDateFormatter().getNCSATime(startDate) + "]";
            accessLogEntry.append(formattedDate);
            accessLogStartTime.set(startTime);
        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

    public static long getStartTime(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        HttpRequestMessageImpl requestMessageImpl = null;
        long startTime = 0;
        if (request != null) {
            requestMessageImpl = (HttpRequestMessageImpl) request;
        }

        if (requestMessageImpl != null) {
            long elapsedTime = System.nanoTime() - requestMessageImpl.getStartNanoTime();
            startTime = System.currentTimeMillis() - TimeUnit.NANOSECONDS.toMillis(elapsedTime);
        }
        return startTime;
    }

    public static long getStartTimeAsLongForJSON(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        return accessLogStartTime.get();
    }
}
