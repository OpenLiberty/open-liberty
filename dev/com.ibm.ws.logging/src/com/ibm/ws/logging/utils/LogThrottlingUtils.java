/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

public class LogThrottlingUtils {

    private static volatile Map<String, ThrottleState> throttleStates;

    /**
     * Create LogSource, TraceSource and their respective conduits and sets the conduit into the respective sources.
     * In effect this creates the Source + Conduit portion of the pipeline.
     * The rest of the pipline (i.e the handler) is created in the JsonTraceService and will hook into the pipeline
     * there
     * If HPEL or JSR47 TrServices are active, then only the source and conduit/bufferManager portion of the pipeline
     * will be activated in anticpation for consumption by Logstash,LogMet,Audit or GC.
     */
    public LogThrottlingUtils() {
    }

    public static void publish(Map<String, ThrottleState> throttleStatesBTS) {
        throttleStates = throttleStatesBTS;
    }

    public static Map<String, ThrottleState> getThrottleStates() {
        return throttleStates;
    }

}
