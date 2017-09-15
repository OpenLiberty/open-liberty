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

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.logging.AccessLog;
import com.ibm.wsspi.http.logging.DebugLog;
import com.ibm.wsspi.http.logging.HTTPLoggingService;
import com.ibm.wsspi.http.logging.LogUtils;

/**
 * 
 * Http Logging Service. This provides a logging service that is global for
 * all interested components to use when logging client access information
 * or error messages related to client requests.
 * 
 */
public class HTTPLoggingServiceImpl implements HTTPLoggingService {
    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HTTPLoggingServiceImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Flag on whether this service is actively running right now */
    private boolean bRunning = false;
    /** NCSA access log reference */
    private AccessLog ncsaLog = DisabledLogger.getRef();
    /** FRCA access log reference */
    private AccessLog frcaLog = DisabledLogger.getRef();
    /** Debug/error log reference */
    private DebugLog debugLog = DisabledLogger.getRef();

    /**
     * Constructor.
     * 
     * @param config
     */
    public HTTPLoggingServiceImpl(Map<String, Object> config) {
        // parse the error log info if it's enabled
        if (Boolean.parseBoolean((String) config.get("error.enabled"))) {
            parseErrorLog(config);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error log is disabled by config");
            }
        }

        // parse the access log info if it's enabled
        if (Boolean.parseBoolean((String) config.get("access.enabled"))) {
            parseAccessLog(config);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Access log is disabled by config");
            }
        }

        // parse the FRCA log info if it's enabled
        if (Boolean.parseBoolean((String) config.get("frca.enabled"))) {
            parseFRCALog(config);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "FRCA log is disabled by config");
            }
        }

        // register so that HTTP channels can find this
        HttpDispatcher.getFramework().registerService(HTTPLoggingService.class, this);
    }

    /**
     * Parse the access log related information from the config.
     * 
     * @param config
     */
    private void parseAccessLog(Map<String, Object> config) {
        String filename = (String) config.get("access.filePath");
        if (null == filename || 0 == filename.trim().length()) {
            return;
        }
        try {
            this.ncsaLog = new AccessLogger(filename.trim());
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".parseAccessLog", "1", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Logging service was unable to open a file: " + filename + "; " + t);
            }
            return;
        }

        // save the NCSA format value
        String format = (String) config.get("access.logFormat");
        this.ncsaLog.setFormat(LogUtils.convertNCSAFormat(format));

        // now check for the maximum file size
        String size = (String) config.get("access.maximumSize");
        if (null != size) {
            // convert from MB to bytes
            if (!this.ncsaLog.setMaximumSize(convertInt(size, 0) * 1048576L)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Logging service has invalid access log size: " + size);
                }
            }
        }

        // check for the max backup files
        String backups = (String) config.get("access.maximumBackupFiles");
        if (null != backups) {
            this.ncsaLog.setMaximumBackupFiles(convertInt(backups, 1));
        }

    }

    /**
     * Parse the error log related information from the config.
     * 
     * @param config
     */
    private void parseErrorLog(Map<String, Object> config) {
        String filename = (String) config.get("error.filePath");
        if (null == filename || 0 == filename.trim().length()) {
            return;
        }
        try {
            this.debugLog = new DebugLogger(filename);
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".parseErrorLog", "1", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Logging service was unable to open debug file: " + filename + "; " + t);
            }
            return;
        }

        // check the debug log level setting
        String levelName = (String) config.get("error.logLevel");
        this.debugLog.setCurrentLevel(LogUtils.convertDebugLevel(levelName));

        String size = (String) config.get("error.maximumSize");
        if (null != size) {
            // convert from MB to bytes
            if (!this.debugLog.setMaximumSize(convertInt(size, 0) * 1048576L)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Logging service has invalid error log size: " + size);
                }
            }
        }

        // check for the max backup files
        String backups = (String) config.get("error.maximumBackupFiles");
        if (null != backups) {
            this.debugLog.setMaximumBackupFiles(convertInt(backups, 1));
        }

    }

    /**
     * Parse the FRCA log related information from the config object.
     * 
     * @param config
     */
    private void parseFRCALog(Map<String, Object> config) {
        String filename = (String) config.get("frca.filePath");
        if (null == filename || 0 == filename.trim().length()) {
            return;
        }
        try {
            this.frcaLog = new FRCALogger(filename);
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".parseFRCALog", "1", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Logging service was unable to open FRCA file: " + filename + "; " + t);
            }
            return;
        }

        // save the FRCA format value
        String format = (String) config.get("frca.logFormat");
        this.frcaLog.setFormat(LogUtils.convertNCSAFormat(format));

        // now check for the maximum file size
        String size = (String) config.get("frca.maximumSize");
        if (null != size) {
            // convert from MB to bytes
            if (!this.frcaLog.setMaximumSize(convertInt(size, 0) * 1048576L)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Logging service has invalid frca log size: " + size);
                }
            }
        }

        // check for the max backup files
        String backups = (String) config.get("frca.maximumBackupsFiles");
        if (null != backups) {
            this.frcaLog.setMaximumBackupFiles(convertInt(backups, 1));
        }

    }

    /**
     * Start this service.
     */
    public void start() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "start");
        }
        if (!this.bRunning) {
            this.ncsaLog.start();
            this.frcaLog.start();
            this.debugLog.start();
            this.bRunning = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "start");
        }
    }

    /**
     * Stop this service. It can be restarted after this method call.
     * 
     */
    public void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "stop");
        }
        if (this.bRunning) {
            this.bRunning = false;
            this.ncsaLog.stop();
            this.frcaLog.stop();
            this.debugLog.stop();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "stop");
        }
    }

    /**
     * Final call when the service is being destroyed. The service cannot be
     * restarted once this is used.
     * 
     */
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy");
        }
        this.bRunning = false;
        this.ncsaLog.disable();
        this.frcaLog.disable();
        this.debugLog.disable();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    }

    /*
     * @see com.ibm.wsspi.http.logging.HTTPLoggingService#getAccessLog()
     */
    public AccessLog getAccessLog() {
        return this.ncsaLog;
    }

    /*
     * @see com.ibm.wsspi.http.logging.HTTPLoggingService#getFRCALog()
     */
    public AccessLog getFRCALog() {
        return this.frcaLog;
    }

    /*
     * @see com.ibm.wsspi.http.logging.HTTPLoggingService#getDebugLog()
     */
    public DebugLog getDebugLog() {
        return this.debugLog;
    }

    /**
     * Convert the input string to an int value.
     * 
     * @param input
     * @param defaultValue
     *            - if malformed, then return this instead
     * @return int
     */
    private int convertInt(String input, int defaultValue) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Malformed input: " + input);
            }
            return defaultValue;
        }
    }
}
