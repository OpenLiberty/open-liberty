/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogCurrentTime extends AccessLogData {

    // We're assuming that the methods below that use this datetime will be called on the same thread
    private static ThreadLocal<String> accessLogDatetime = new ThreadLocal<>();

    public AccessLogCurrentTime() {
        super("%{t}W");
        // %{format}t
        // The time, in the form given by format,
        // which should be in strftime(3) format. (potentially localized)
    }

    @Override
    public Object init(String rawToken) {
        // The only token supported is t, i.e. %{t}W
        if ("t".equals(rawToken)) {
            return null;
        }
        return rawToken;
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {
        if (data == null) {
            String currentTime = HttpDispatcher.getDateFormatter().getNCSATime(new Date(System.currentTimeMillis()));
            String currentTimeFormatted = "[" + currentTime + "]";
            accessLogEntry.append(currentTimeFormatted);
            accessLogDatetime.set(currentTimeFormatted);

        } else {
            // just print out what was there
            accessLogEntry.append("%{").append(data).append("}W");
        }

        return true;
    }

    public static String getAccessLogCurrentTimeAsString(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        return accessLogDatetime.get();
    }
}
