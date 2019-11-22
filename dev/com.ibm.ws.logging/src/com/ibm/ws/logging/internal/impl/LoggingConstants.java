/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

public interface LoggingConstants {
    public static enum TraceFormat {
        BASIC, ENHANCED, ADVANCED;
    }

    String DEFAULT_LOG_LEVEL = "AUDIT";
    int DEFAULT_FILE_MAX_SIZE = 20;
    int MAX_DATA_LENGTH = 1024 * 16;
    int DEFAULT_MAX_FILES = 2;

    String DEFAULT_MSG_FILE = "messages.log";
    String DEFAULT_TRACE_FILE = "trace.log";

    String PROP_FFDC_SUMMARY_POLICY = "com.ibm.ws.logging.ffdc.summary.policy";

    String PROP_TRACE_DELEGATE = "com.ibm.ws.logging.trace.delegate";
    String DEFAULT_TRACE_IMPLEMENTATION = "com.ibm.ws.logging.internal.impl.BaseTraceService";
    String JSR47_TRACE_IMPLEMENTATION = "com.ibm.ws.logging.internal.impl.Jsr47TraceService";

    String PROP_FFDC_DELEGATE = "com.ibm.ws.logging.ffdc.delegate";
    String DEFAULT_FFDC_IMPLEMENTATION = "com.ibm.ws.logging.internal.impl.BaseFFDCService";

    String SYSTEM_OUT = "SystemOut";
    String SYSTEM_ERR = "SystemErr";

    String nl = System.getProperty("line.separator");
    int nlen = nl.length();

    String DEFAULT_MESSAGE_SOURCE = "message";
    String DEFAULT_MESSAGE_FORMAT = "simple";
    String DEFAULT_CONSOLE_SOURCE = "message";
    String DEFAULT_CONSOLE_FORMAT = "dev";
    String DEFAULT_TRACE_SOURCE = "trace";
    String JSON_FORMAT = "json";
    String DEPRECATED_DEFAULT_FORMAT = "basic";

    String ENV_WLP_LOGGING_MESSAGE_SOURCE = "WLP_LOGGING_MESSAGE_SOURCE";
    String ENV_WLP_LOGGING_MESSAGE_FORMAT = "WLP_LOGGING_MESSAGE_FORMAT";
    String ENV_WLP_LOGGING_CONSOLE_SOURCE = "WLP_LOGGING_CONSOLE_SOURCE";
    String ENV_WLP_LOGGING_CONSOLE_FORMAT = "WLP_LOGGING_CONSOLE_FORMAT";
    String ENV_WLP_LOGGING_JSON_FIELD_MAPPINGS = "WLP_LOGGING_JSON_FIELD_MAPPINGS";
    String ENV_WLP_LOGGING_CONSOLE_LOGLEVEL = "WLP_LOGGING_CONSOLE_LOGLEVEL";

    //beta env var for omission of json fields
    String ENV_WLP_LOGGING_OMIT_JSON_FIELD_MAPPINGS = "WLP_LOGGING_OMIT_JSON_FIELD_MAPPINGS";

    enum FFDCSummaryPolicy {
        DEFAULT, IMMEDIATE
    };
}