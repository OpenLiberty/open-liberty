/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogElapsedRequestTime extends AccessLogData {

    public AccessLogElapsedRequestTime() {
        super("%{R}W");
        // %{R}W - Elapsed time - in nanoseconds - of the request.
    }

    @Override
    public Object init(String rawToken) {
        // The only token supported is t, i.e. %{R}W
        if ("R".equals(rawToken)) {
            return null;
        }
        return rawToken;
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {

        long elapsedRequestTime = getElapsedRequestTime(response, request, data);

        if (elapsedRequestTime > 0) {
            accessLogEntry.append(elapsedRequestTime);

        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

    public static long getElapsedRequestTime(HttpResponseMessage response, HttpRequestMessage request, Object data) {

        long startTime = 0;
        long endTime = 0;

        HttpRequestMessage message;

        if (Objects.nonNull(request)) {

            message = request;
            startTime = message.getStartTime();
            endTime = message.getEndTime();
        }

        if (startTime != 0 && endTime >= startTime) {
            long elapsedTime = endTime - startTime;
            return TimeUnit.NANOSECONDS.toMicros(elapsedTime);
        } else {
            return -1;
        }
    }

}
