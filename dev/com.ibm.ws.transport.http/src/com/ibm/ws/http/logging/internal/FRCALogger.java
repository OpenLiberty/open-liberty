/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.logging.internal;

import java.io.FileNotFoundException;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.http.channel.HttpChannelUtils;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.logging.AccessLog;

/**
 * Log wrapper specific to a FRCA NCSA file. This will perform the disk IO
 * on the caller's thread.
 * 
 */
public class FRCALogger extends LoggerOnThread implements AccessLog {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(FRCALogger.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Currently configured NCSA format to use on this log */
    private Format myFormat = Format.COMMON;

    /**
     * Constructor.
     * 
     * @param filename
     * @throws FileNotFoundException
     */
    public FRCALogger(String filename) throws FileNotFoundException {
        super(filename);
    }

    /**
     * @see AccessLog#getFormat()
     */
    public Format getFormat() {
        return this.myFormat;
    }

    /**
     * @see AccessLog#setFormat(AccessLog.Format)
     */
    public void setFormat(Format format) {
        this.myFormat = format;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Set access format to " + format);
        }
    }

    /**
     * @see AccessLog#log(HttpRequestMessage, HttpResponseMessage, String, String, String, long)
     */
    public void log(HttpRequestMessage request, HttpResponseMessage response, String version, String userId, String remoteAddr, long numBytes) {
        if (!isStarted()) {
            return;
        }
        try {
            // addr - userid [date] "method uri?query version" status_code
            // content_length
            StringBuilder sb = new StringBuilder(128);
            sb.append(remoteAddr);
            if (null == userId) {
                sb.append(" - - [");
            } else {
                sb.append(" - ");
                sb.append(userId);
                sb.append(" [");
            }
            sb.append(HttpDispatcher.getDateFormatter().getNCSATime());
            sb.append("] \"");
            sb.append(request.getMethodValue().getName());
            sb.append(' ');
            sb.append(request.getRequestURI());
            String query = request.getQueryString();
            if (null != query) {
                sb.append('?');
                sb.append(GenericUtils.nullOutPasswords(query, (byte) '&'));
            }
            sb.append(' ');
            sb.append(version);
            sb.append("\" ");
            sb.append(response.getStatusCodeAsInt());
            sb.append(' ');
            if (HeaderStorage.NOTSET != response.getContentLength()) {
                sb.append(response.getContentLength());
            } else {
                sb.append(numBytes);
            }
            if (Format.COMBINED.equals(getFormat())) {
                // combined adds "referer user_agent cookie"
                String val = request.getHeader(HttpHeaderKeys.HDR_REFERER).asString();
                if (null == val) {
                    sb.append(" -");
                } else {
                    sb.append(" \"");
                    sb.append(val);
                    sb.append("\"");
                }
                val = request.getHeader(HttpHeaderKeys.HDR_USER_AGENT).asString();
                if (null == val) {
                    sb.append(" -");
                } else {
                    sb.append(" \"");
                    sb.append(val);
                    sb.append('\"');
                }
                // now save the Cookie header instances into the output
                int count = request.getNumberOfHeaderInstances(HttpHeaderKeys.HDR_COOKIE);
                if (0 == count) {
                    sb.append(" -");
                } else {
                    sb.append(" \"");
                    Iterator<HeaderField> it = request.getHeaders(HttpHeaderKeys.HDR_COOKIE).iterator();
                    sb.append(it.next().asString());
                    while (it.hasNext()) {
                        sb.append(", ");
                        sb.append(it.next().asString());
                    }
                    sb.append('\"');
                }
            }
            sb.append("\r\n");

            WsByteBuffer wsbb = HttpDispatcher.getBufferManager().allocate(sb.length());
            wsbb.put(HttpChannelUtils.getBytes(sb));
            wsbb.flip();
            super.log(wsbb);
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".log", "1", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Exception while writing log msg; " + t);
            }
        }
    }

    /**
     * @see AccessLog#log(byte[])
     */
    public void log(byte[] message) {
        if (!isStarted()) {
            return;
        }
        try {
            WsByteBuffer wsbb = HttpDispatcher.getBufferManager().allocate(message.length + 2);
            wsbb.put(message);
            wsbb.flip();
            super.log(wsbb);
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".log", "2", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error writing to log; " + t);
            }
        }
    }

    /**
     * @see AccessLog#log(java.lang.String)
     */
    public void log(String message) {
        if (isStarted()) {
            log(GenericUtils.getEnglishBytes(message));
        }
    }

    /**
     * Return the string representation of this file.
     * 
     * @return String
     */
    public String toString() {
        return super.toString() + "\n  NCSA Format: " + getFormat();
    }

}
