/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.collector.manager.internal;

import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.Source;

/**
 * Utility methods for collector manager framework
 */
public class CollectorManagerUtils {

    public static final String NAME = "name";
    public static final String LOCATION = "location";

    public static String getSourceId(Source source) {
        return source.getSourceName() + "|" + source.getLocation();
    }

    public static String getHandlerId(Handler handler) {
        return handler.getHandlerName();
    }
}
