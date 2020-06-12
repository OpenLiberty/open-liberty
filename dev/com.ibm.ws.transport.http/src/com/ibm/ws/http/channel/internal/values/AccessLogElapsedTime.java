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

import java.util.concurrent.TimeUnit;

import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogElapsedTime extends AccessLogData {

    public AccessLogElapsedTime() {
        super("%D");
        // %D - Elapsed time, in milliseconds, of the request/response exchange
        // Millisecond accuracy, microsecond precision
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {
        HttpRequestMessageImpl requestMessageImpl = null;
        long startTime = 0;
        if (request != null) {
            requestMessageImpl = (HttpRequestMessageImpl) request;
        }

        if (requestMessageImpl != null) {
            startTime = requestMessageImpl.getStartTime();
        }

        if (startTime != 0) {
            long elapsedTime = System.nanoTime() - startTime;
            accessLogEntry.append(TimeUnit.NANOSECONDS.toMicros(elapsedTime));
        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

}
