/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/


package com.ibm.ws.logging.internal.osgi;


import java.io.PrintWriter;
import java.util.Map;

import com.ibm.wsspi.logging.Introspector;

import com.ibm.ws.logging.utils.LogThrottlingUtils;
import com.ibm.ws.logging.utils.ThrottleState;


public class LogThrottleIntrospector implements Introspector {


    @Override
    public String getIntrospectorName() {
        return "LogThrottleIntrospector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "List of logs being throttled.";
    }

    public void init() {
    }
    
    @Override
    public void introspect(PrintWriter out) throws Exception {
        out.println("~~~~~~~~~~~~~~~~~~~");
        Map<String, ThrottleState> throttleStates = LogThrottlingUtils.getThrottleStates();

        for (Map.Entry<String, ThrottleState> entry : throttleStates.entrySet()) {
            out.println("Key11 being throttled: " + entry.getKey() + " -- Occurences over the last 5 minutes: " + entry.getValue().getRunningTotal() + " -- Last occurence: "
                        + entry.getValue().getLastAccessTime());

        }
        out.println("~~~~~~~~~~~~~~~~~~~");
    }
}
