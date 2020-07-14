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
package com.ibm.ws.http.logging.internal;

import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.values.AccessLogData;
import com.ibm.ws.http.channel.internal.values.AccessLogElapsedRequestTime;
import com.ibm.ws.http.channel.internal.values.AccessLogLocalIP;
import com.ibm.ws.http.channel.internal.values.AccessLogLocalPort;
import com.ibm.ws.http.channel.internal.values.AccessLogStartTime;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.http.channel.HttpChannelUtils;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.logging.AccessLog;
import com.ibm.wsspi.http.logging.AccessLogForwarder;
import com.ibm.wsspi.http.logging.AccessLogRecordData;
import com.ibm.wsspi.http.logging.LogForwarderManager;

/**
 * Implementation of an NCSA access log file. This will perform the disk IO on
 * a background thread, not on the caller's thread.
 *
 */
@Component(configurationPid = "com.ibm.ws.http.log.access", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = AccessLogger.class,
           property = { "service.vendor=IBM" })
public class AccessLogger extends LoggerOffThread implements AccessLog {

    /**  */
    private static final String PROP_MAXFILES = "maxFiles";

    /**  */
    private static final String PROP_MAXFILESIZE = "maxFileSize";

    /**  */
    private static final String PROP_LOGFORMAT = "logFormat";

    /**  */
    private static final String PROP_FILEPATH = "filePath";

    /**  */
    private static final String PROP_ENABLED = "enabled";

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(AccessLogger.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    /** Modifier for converting log size config to runtime value */
    private static final int LOGSIZE_MODIFIER = 1048576;

    private static final String newLine;
    static {
        newLine = AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty("line.separator");
            }
        });

    }

    public static class FormatSegment {
        public String string;
        public Object data;
        public AccessLogData log;
    }

    private volatile Map<String, Object> config;

    /** Configured access log format */
    private Format myFormat = Format.COMMON;
    /** F009742 - New String format */
    private String stringFormat = null; //"%h %u %{t}W \"%r\" %s %b";

    private FormatSegment[] parsedFormat;

    /**
     * Constructor of this NCSA access log file.
     *
     * @param filename
     * @throws FileNotFoundException
     */
    public AccessLogger() {

    }

    public AccessLogger(String filename) throws FileNotFoundException {
        super(filename);
    }

    @Activate
    protected void activate(ComponentContext ctx, Map<String, Object> config) {
        modified(config);
        start();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        stop();
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        this.config = config;
        configure();
    }

    private synchronized void configure() {
        Map<String, Object> config = this.config;
        if (config == null)
            return;

        boolean enabled = Boolean.valueOf(config.get(PROP_ENABLED).toString());
        String filename = config.get(PROP_FILEPATH).toString();
        try {
            if (!!!filename.equals(getFilePathName())) {
                // new file name
                setFilename(filename);
            }

            String logFormat = config.get(PROP_LOGFORMAT).toString();
            if (!!!logFormat.equals(getFormatString())) {
                setFormatString(logFormat);
            }

            String value = config.get(PROP_MAXFILESIZE).toString();
            try {
                long maximumSize = Long.valueOf(value);
                setMaximumSize(maximumSize * LOGSIZE_MODIFIER);
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".modified", PROP_MAXFILESIZE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: invalid access size: " + value);
                }
            }

            value = config.get(PROP_MAXFILES).toString();
            try {
                int maximumBackups = Integer.valueOf(value);
                setMaximumBackupFiles(maximumBackups);
            } catch (NumberFormatException nfe) {
                FFDCFilter.processException(nfe, getClass().getName() + ".modified", PROP_MAXFILES);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Config: invalid access max files: " + value);
                }
            }

        } catch (FileNotFoundException e) {
            FFDCFilter.processException(e, getClass().getName() + ".modified", "name", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Config: invalid access log name [" + filename + "]");
            }
            enabled = false;
        }

        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    /**
     * @see AccessLog#getFormat()
     */
    @Override
    @Trivial
    public Format getFormat() {
        return this.myFormat;
    }

    /**
     * Get the access log format that is the string format
     *
     * @return String
     */
    @Trivial
    public String getFormatString() {
        return this.stringFormat;
    }

    /**
     * @see AccessLog#setFormat(com.ibm.ws.http.logging.AccessLog.Format)
     */
    @Override
    @Trivial
    public void setFormat(Format format) {
        this.myFormat = format;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Set access format to " + format);
        }
    }

    /**
     * Set the access log format to a String
     *
     * @param format
     */
    @Trivial
    public void setFormatString(String format) {
        this.stringFormat = format;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Set access format to " + format);
        }
        parseFormat();
    }

    private void parseFormat() {
        if (stringFormat == null) {
            parsedFormat = null;
            return;
        }

        List<FormatSegment> list = new ArrayList<FormatSegment>();
        boolean formatSpecifier = false; // true when we are going after a format specifier, e.g., %x
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < stringFormat.length(); i++) {
            char ch = stringFormat.charAt(i);
            if (formatSpecifier) {
                /*
                 * For code that processes {, the behavior will be ... if I do
                 * not encounter a closing } - then I ignore the {
                 */
                if ('{' == ch) {
                    StringBuilder name = new StringBuilder();
                    int j = i + 1;
                    for (; j < stringFormat.length() && '}' != stringFormat.charAt(j); j++) {
                        name.append(stringFormat.charAt(j));
                    }
                    // look past the }
                    if (j + 1 < stringFormat.length()) {
                        j++;
                        FormatSegment segment = createAccessLogData(stringFormat.charAt(j), name.toString());
                        if (segment != null) {
                            list.add(segment);
                        } else {
                            String specifier = "%{" + name.toString() + "}" + stringFormat.charAt(j);
                            list.add(createStringData(specifier));
                            String msg = "Config: invalid format segment: " + specifier;
                            FFDCFilter.processException(new IllegalArgumentException(msg),
                                                        this.getClass().getName(), "parseFormat");
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(tc, msg);
                            }
                        }
                        i = j; // reposition our position
                    } else {
                        // %{wer -- incomplete in some way
                        String specifier = "%{";
                        String msg = "Config: incomplete format segment: " + specifier;
                        list.add(createStringData(specifier));
                        FFDCFilter.processException(new IllegalArgumentException(msg), this.getClass().getName(), "parseFormat");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, msg);
                        }
                    }
                } else if (ch == '%') {
                    // %% -> %
                    buf.append('%');
                } else {
                    // %X
                    FormatSegment segment = createAccessLogData(ch, null);
                    if (segment != null) {
                        list.add(segment);
                    } else {
                        String specifier = "%" + ch;
                        String msg = "Config: invalid format segment: " + specifier;
                        list.add(createStringData(specifier));
                        FFDCFilter.processException(new IllegalArgumentException(msg), this.getClass().getName(), "parseFormat");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, msg);
                        }
                    }
                }
                formatSpecifier = false;
            } else if (ch == '%') {
                formatSpecifier = true;
                if (buf.length() > 0) {
                    list.add(createStringData(buf.toString()));
                }
                buf = new StringBuilder();
            } else {
                buf.append(ch);
            }
        }
        if (buf.length() > 0) {
            list.add(createStringData(buf.toString()));
        }

        parsedFormat = list.toArray(new FormatSegment[list.size()]);
    }

    /**
     * @param string
     * @return
     */
    private FormatSegment createStringData(String string) {
        FormatSegment formatSegment = new FormatSegment();
        formatSegment.string = string;
        return formatSegment;
    }

    private FormatSegment createAccessLogData(char ch, String modifier) {

        // PI36010 Start
        AccessLogData accessLogData = null;
        if ('W' == ch) {
            StringBuilder matchString = new StringBuilder("%{");
            matchString.append(modifier);
            matchString.append("}");
            matchString.append(ch);
            accessLogData = AccessLogData.match(matchString.toString(), 0,
                                                matchString.length());
        } else {
            // match against %ch
            byte[] bs = new byte[] { '%', (byte) ch };
            accessLogData = AccessLogData.match(bs, 0, bs.length);
        }
        // PI36010 End
        if (accessLogData == null)
            return null;

        FormatSegment formatSegment = new FormatSegment();
        formatSegment.log = accessLogData;
        if (modifier != null) {
            formatSegment.data = accessLogData.init(modifier);
        }
        return formatSegment;
    }

    /**
     * @see AccessLog#log(com.ibm.wsspi.http.channel.HttpRequestMessage, com.ibm.wsspi.http.channel.HttpResponseMessage, java.lang.String, java.lang.String, java.lang.String, long)
     */
    @Override
    public void log(HttpRequestMessage request,
                    HttpResponseMessage response, String version,
                    String userId, String remoteAddr, long numBytes) {
        if (!isStarted()) {
            return;
        }
        try {
            StringBuilder accessLogLine;
            if (parsedFormat != null) {
                accessLogLine = new StringBuilder();
                for (FormatSegment s : parsedFormat) {
                    if (s.string != null) {
                        accessLogLine.append(s.string);
                    }
                    // Sets information in the extraData object for each format specifier
                    if (s.log != null) {
                        s.log.set(accessLogLine, response, request, s.data);
                    }
                }
            } else {
                // addr - userid [date] "method uri?query version" status_code content_length
                accessLogLine = new StringBuilder(128);
                accessLogLine.append(remoteAddr);
                if (null == userId) {
                    accessLogLine.append(" - - [");
                } else {
                    accessLogLine.append(" - ");
                    accessLogLine.append(userId);
                    accessLogLine.append(" [");
                }
                accessLogLine.append(HttpDispatcher.getDateFormatter().getNCSATime());
                accessLogLine.append("] \"");
                accessLogLine.append(request.getMethodValue().getName());
                accessLogLine.append(' ');
                accessLogLine.append(request.getRequestURI());
                String query = request.getQueryString();
                if (null != query) {
                    accessLogLine.append('?');
                    accessLogLine.append(GenericUtils.nullOutPasswords(query, (byte) '&'));
                }
                accessLogLine.append(' ');
                accessLogLine.append(version);
                accessLogLine.append("\" ");
                accessLogLine.append(response.getStatusCodeAsInt());
                accessLogLine.append(' ');
                if (HeaderStorage.NOTSET != response.getContentLength()) {
                    accessLogLine.append(response.getContentLength());
                } else {
                    accessLogLine.append(numBytes);
                }
                if (Format.COMBINED.equals(getFormat())) {
                    // combined adds "referer user_agent cookie"
                    String val = request.getHeader(HttpHeaderKeys.HDR_REFERER).asString();
                    if (null == val) {
                        accessLogLine.append(" -");
                    } else {
                        accessLogLine.append(" \"");
                        accessLogLine.append(val);
                        accessLogLine.append("\"");
                    }
                    val = request.getHeader(HttpHeaderKeys.HDR_USER_AGENT).asString();
                    if (null == val) {
                        accessLogLine.append(" -");
                    } else {
                        accessLogLine.append(" \"");
                        accessLogLine.append(val);
                        accessLogLine.append('\"');
                    }
                    // now save the Cookie header instances into the output
                    int count = request.getNumberOfHeaderInstances(HttpHeaderKeys.HDR_COOKIE);
                    if (0 == count) {
                        accessLogLine.append(" -");
                    } else {
                        accessLogLine.append(" \"");
                        Iterator<HeaderField> it = request.getHeaders(HttpHeaderKeys.HDR_COOKIE).iterator();
                        accessLogLine.append(it.next().asString());
                        while (it.hasNext()) {
                            accessLogLine.append(", ");
                            accessLogLine.append(it.next().asString());
                        }
                        accessLogLine.append('\"');
                    }
                }
            }

            accessLogLine.append(newLine);

            // Forward the log data to AccessLogForwarder's
            if (!LogForwarderManager.getAccessLogForwarders().isEmpty()) {
                AccessLogRecordData recordData = toAccessLogRecordData(request, response, version, userId, remoteAddr, numBytes);
                AccessLogRecordDataExt recordDataExt = new AccessLogRecordDataExt(recordData, parsedFormat, getFormatString());
                for (AccessLogForwarder forwarder : LogForwarderManager.getAccessLogForwarders()) {
                    try {
                        forwarder.process(recordDataExt);
                    } catch (Throwable t) {
                        FFDCFilter.processException(t, getClass().getName() + ".log", "136", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, "Exception while forwarder log to " + forwarder + " ; " + t);
                        }
                    }
                } // end-for
            }

            WsByteBuffer wsbb = HttpDispatcher.getBufferManager().allocate(accessLogLine.length());
            wsbb.put(HttpChannelUtils.getBytes(accessLogLine));
            wsbb.flip();
            super.log(wsbb);
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".log", "136", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Exception while writing log msg; " + t);
            }
        }
    }

    /**
     * Return a AccessLogRecordData instance
     *
     * @param request
     * @param response
     * @param version
     * @param userId
     * @param remoteAddr
     * @param numBytes
     * @return
     */
    private AccessLogRecordData toAccessLogRecordData(final HttpRequestMessage request, final HttpResponseMessage response, final String version, final String userId,
                                                      final String remoteAddr, final long numBytes) {
        final HttpRequestMessage request2 = request; // request.duplicate();
        final HttpResponseMessage response2 = response; // response.duplicate();

        final long timestamp;
        final long startTime;
        final long elapsedTime;
        final String localIP;
        final String localPort;

        // ** timestamp
        timestamp = System.currentTimeMillis();

        // ** Request Start Time
        startTime = AccessLogStartTime.getStartTime(response2, request2, null);

        // ** Elapsed Request Time
        elapsedTime = AccessLogElapsedRequestTime.getElapsedRequestTime(response2, request2, null);

        // ** LocalIP
        localIP = AccessLogLocalIP.getLocalIP(response2, request2, null);

        // ** LocalPort
        localPort = AccessLogLocalPort.getLocalPort(response2, request2, null);

        // ** AccessLogRecordData
        AccessLogRecordData recordData = new AccessLogRecordData() {

            @Override
            public long getTimestamp() {
                return timestamp;
            }

            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public HttpResponseMessage getResponse() {
                return response2;
            }

            @Override
            public HttpRequestMessage getRequest() {
                return request2;
            }

            @Override
            public String getRemoteAddress() {
                return remoteAddr;
            }

            @Override
            public long getBytesWritten() {
                return numBytes;
            }

            @Override
            public long getStartTime() {
                return startTime;
            }

            @Override
            public long getElapsedTime() {
                return elapsedTime;
            }

            @Override
            public String getLocalIP() {
                return localIP;
            }

            @Override
            public String getLocalPort() {
                return localPort;
            }
        };
        return recordData;
    }

    /**
     * @see com.ibm.ws.http.logging.AccessLog#log(byte[])
     */
    @Override
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
            FFDCFilter.processException(t, getClass().getName() + ".log", "156", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error writing to log; " + t);
            }
        }
    }

    /**
     * @see com.ibm.ws.http.logging.AccessLog#log(java.lang.String)
     */
    @Override
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
    @Override
    @Trivial
    public String toString() {
        return super.toString() + "\n Format: " + getFormatString();
    }
}
