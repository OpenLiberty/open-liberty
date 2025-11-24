/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.logging.utils;

import java.util.Map;

import com.ibm.ws.logging.internal.impl.BaseTraceService;

public class LogThrottlingUtils {

    private static volatile BaseTraceService BaseTraceService;

    /**
     * Collect throttle data from BaseTraceService to be used in the logging.osgi project which is unable to access
     * BaseTraceService. This data is being used for the Throttle Introspector.
     */
    public LogThrottlingUtils() {
    }

    public static void publish(BaseTraceService baseTraceService) {
        BaseTraceService = baseTraceService;
    }

    public static Map<String, ThrottleState> getThrottleStates() {
        return BaseTraceService.getThrottleStates();
    }

    public static int getThrottleMaxMessages() {
        return BaseTraceService.getThrottleMaxMessagesPerWindow();
    }

    public static String getThrottleType() {
        return BaseTraceService.getThrottleType();
    }

    public static int getThrottleMapSize() {
        return BaseTraceService.getThrottleMapSize();
    }
}
