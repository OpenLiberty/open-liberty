/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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

        long startTime = 0;

        if (Objects.nonNull(request)) {
            long elapsedTime = System.nanoTime() - request.getStartTime();
            startTime = System.currentTimeMillis() - TimeUnit.NANOSECONDS.toMillis(elapsedTime);
        }
        return startTime;
    }

    public static long getStartTimeAsLongForJSON(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        return accessLogStartTime.get();
    }
}
