/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testing.opentracing.service;

/**
 * <p>Constants for the open trace test service.  Externally useful constants
 * are consolidated here for easy of use.  It is intended that a copy of these
 * constants will be put in the test code.<p>
 */
public interface FATOpentracingConstants {
    // Service application ...
    String APP_PATH = "rest"; // used in '@ApplicationPath'
    String SERVICE_PATH = "testService"; // used in '@Path'

	// 'getImmediate' and 'getManual' both accept a "response" parameter.
    String GET_IMMEDIATE_PATH = "getImmediate";
    String GET_MANUAL_PATH = "getManual";
    String RESPONSE_PARAM_NAME = "response";

    // 'getDelayed' adds a 'delay' parameter.
    String GET_DELAYED_PATH = "getDelayed";
    String DELAY_PARAM_NAME = "delay";

    // 'getNested" add "nestDepth", "async", "host", "port", and "contextRoot" parameters.
    String GET_NESTED_PATH = "getNested";
    String NEST_DEPTH_PARAM_NAME = "nestDepth";
    String ASYNC_PARAM_NAME = "async";
    String HOST_PARAM_NAME = "host";
    String PORT_PARAM_NAME = "port";
    String CONTEXT_ROOT_PARAM_NAME = "contextRoot";

    // Introspection service API ...
    String GET_TRACER_STATE_PATH = "getTracerState";
    
    // 'excludeTest' API
    String GET_EXCLUDE_TEST_PATH = "excludeTest";
}
