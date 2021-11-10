/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.logging.internal;

import com.ibm.ws.http.logging.internal.AccessLogger.FormatSegment;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.logging.AccessLogRecordData;

/**
 *
 */
public class AccessLogRecordDataExt implements AccessLogRecordData {

    AccessLogRecordData delegate;
    FormatSegment[] parsedFormat;
    String formatString;

    AccessLogRecordDataExt(AccessLogRecordData alrd, FormatSegment[] parsedFormat, String formatString) {
        this.delegate = alrd;
        this.parsedFormat = parsedFormat;
        this.formatString = formatString;
    }

    public FormatSegment[] getParsedFormat() {
        return parsedFormat;
    }

    public String getFormatString() {
        return formatString;
    }

    @Override
    public HttpRequestMessage getRequest() {
        return delegate.getRequest();
    }

    @Override
    public HttpResponseMessage getResponse() {
        return delegate.getResponse();
    }

    @Override
    public long getTimestamp() {
        return delegate.getTimestamp();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public String getUserId() {
        return delegate.getUserId();
    }

    @Override
    public String getRemoteAddress() {
        return delegate.getRemoteAddress();
    }

    @Override
    public long getBytesWritten() {
        return delegate.getBytesWritten();
    }

    @Override
    public long getStartTime() {
        return delegate.getStartTime();
    }

    @Override
    public long getElapsedTime() {
        return delegate.getElapsedTime();
    }

    @Override
    public String getLocalIP() {
        return delegate.getLocalIP();
    }

    @Override
    public String getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    public String getRemotePort() {
        return delegate.getRemotePort();
    }
}
