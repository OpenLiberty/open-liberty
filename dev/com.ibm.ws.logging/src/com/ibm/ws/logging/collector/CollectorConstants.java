/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

public class CollectorConstants {

    /* Config values */
    public static final String GC_CONFIG_VAL = "garbageCollection";
    public static final String MESSAGES_CONFIG_VAL = "message";
    public static final String FFDC_CONFIG_VAL = "ffdc";
    public static final String TRACE_CONFIG_VAL = "trace";
    public static final String ACCESS_CONFIG_VAL = "accessLog";

    /* Source component names */
    public static final String GC_SOURCE = "com.ibm.ws.health.center.source.gcsource";
    public static final String MESSAGES_SOURCE = "com.ibm.ws.logging.source.message";
    public static final String FFDC_SOURCE = "com.ibm.ws.logging.ffdc.source.ffdcsource";
    public static final String TRACE_SOURCE = "com.ibm.ws.logging.source.trace";
    public static final String ACCESS_LOG_SOURCE = "com.ibm.ws.http.logging.source.accesslog";

    /* Location */
    public static final String MEMORY = "memory";

    /* Event types */
    public static final String GC_EVENT_TYPE = "liberty_gc";
    public static final String MEMORY_REC_EVENT_TYPE = "liberty_recommendations";
    public static final String MESSAGES_LOG_EVENT_TYPE = "liberty_message";
    public static final String FFDC_EVENT_TYPE = "liberty_ffdc";
    public static final String TRACE_LOG_EVENT_TYPE = "liberty_trace";
    public static final String ACCESS_LOG_EVENT_TYPE = "liberty_accesslog";
}
