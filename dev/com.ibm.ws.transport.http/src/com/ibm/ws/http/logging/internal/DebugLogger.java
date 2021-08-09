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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.channel.HttpChannelUtils;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.logging.DebugLog;

/**
 * Logger that wraps a debug/error log file. This will perform the disk IO on
 * a background thread, not on the caller's.
 * 
 */
public class DebugLogger extends LoggerOnThread implements DebugLog {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(DebugLogger.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Currently configured log level */
    private Level myLevel = Level.WARN;

    /**
     * Constructor.
     * 
     * @param filename
     * @throws FileNotFoundException
     */
    public DebugLogger(String filename) throws FileNotFoundException {
        super(filename);
    }

    /**
     * @see DebugLog#getCurrentLevel()
     */
    public Level getCurrentLevel() {
        return this.myLevel;
    }

    /**
     * @see DebugLog#setCurrentLevel(DebugLog.Level)
     */
    public void setCurrentLevel(Level logLevel) {
        this.myLevel = logLevel;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Set the loglevel to " + logLevel);
        }
    }

    /**
     * @see DebugLog#isEnabled(DebugLog.Level)
     */
    public boolean isEnabled(Level logLevel) {
        // level is enabled if it's the same or less in the enum list
        return (0 <= this.myLevel.compareTo(logLevel));
    }

    /**
     * @see DebugLog#log(DebugLog.Level, byte[], HttpServiceContext)
     */
    public void log(Level logLevel, byte[] message, HttpServiceContext hsc) {
        if (!isEnabled(logLevel) || null == message) {
            return;
        }
        try {
            // [date] [level] [clientIP:port/serverIP:port] message
            StringBuilder sb = new StringBuilder(125);
            // save the [date]
            sb.append('[');
            sb.append(HttpDispatcher.getDateFormatter().getRFC1123Time());
            sb.append("] [");
            sb.append(logLevel.name());
            sb.append("] [");
            // now the [client IP:port/serverIP:port]
            if (null == hsc) {
                sb.append("-/-");
            } else {
                sb.append(hsc.getRemoteAddr().getHostAddress());
                sb.append(':');
                sb.append(hsc.getRemotePort());
                sb.append('/');
                sb.append(hsc.getLocalAddr().getHostAddress());
                sb.append(':');
                sb.append(hsc.getLocalPort());
            }
            sb.append("] ");
            byte[] data = HttpChannelUtils.getBytes(sb);
            // now dump it into the WsByteBuffer object
            WsByteBuffer wsbb = HttpDispatcher.getBufferManager().allocateDirect(data.length + message.length + 2);
            wsbb.put(data);
            wsbb.put(message);
            wsbb.put(CRLF);
            wsbb.flip();
            super.log(wsbb);
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".log", "1", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error writing to log; " + t);
            }
        }
    }

    /**
     * @see DebugLog#log(DebugLog.Level, String, HttpServiceContext)
     */
    public void log(Level logLevel, String message, HttpServiceContext hsc) {
        if (isEnabled(logLevel) && null != message) {
            log(logLevel, GenericUtils.getEnglishBytes(message), hsc);
        }
    }

    /**
     * @see DebugLog#log(DebugLog.Level, byte[], String, String, String, String)
     */
    public void log(Level logLevel, byte[] message, String remoteIP, String remotePort, String localIP, String localPort) {
        if (!isEnabled(logLevel) || null == message) {
            return;
        }
        try {
            // [date] [level] [clientIP:port/serverIP:port] message
            StringBuilder sb = new StringBuilder(125);
            // save the [date]
            sb.append('[');
            sb.append(HttpDispatcher.getDateFormatter().getRFC1123Time());
            sb.append("] [");
            sb.append(logLevel.name());
            sb.append("] [");
            sb.append(remoteIP);
            sb.append(':');
            sb.append(remotePort);
            sb.append('/');
            sb.append(localIP);
            sb.append(':');
            sb.append(localPort);
            sb.append("] ");
            byte[] data = HttpChannelUtils.getBytes(sb);
            // now dump it into the WsByteBuffer object
            WsByteBuffer wsbb = HttpDispatcher.getBufferManager().allocateDirect(data.length + message.length + 2);
            wsbb.put(data);
            wsbb.put(message);
            wsbb.put(CRLF);
            wsbb.flip();
            super.log(wsbb);
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".log", "3", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error writing to log; " + t);
            }
        }
    }

    /**
     * @see DebugLog#log(DebugLog.Level, String, String, String, String, String)
     */
    public void log(Level logLevel, String message, String remoteIP, String remotePort, String localIP, String localPort) {
        if (isEnabled(logLevel) && null != message) {
            log(logLevel, GenericUtils.getEnglishBytes(message), remoteIP, remotePort, localIP, localPort);
        }
    }

    /**
     * Return the string representation of this file.
     * 
     * @return String
     */
    public String toString() {
        return super.toString() + "\n  LogLevel: " + getCurrentLevel();
    }
}
