/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.chfw;

public interface GenericServiceConstants {

    public static final String TOPIC_PFX = "com/ibm/ws/transport/sip/endpoint/";
    public static final String ENDPOINT_ACTIVE_HOST = "activeHost";
    public static final String ENDPOINT_ACTIVE_PORT = "activePort";
    public static final String ENDPOINT_CONFIG_HOST = "configHost";
    public static final String ENDPOINT_CONFIG_PORT = "configPort";
    public static final String ENDPOINT_ALIAS = "alias";
    public static final String ENDPOINT_IS_TLS = "isTls"; 
    public static final String ENDPOINT_EXCEPTION = "exception";

    public static final String ENDPOINT_FAILED = "/FAILED";
    public static final String ENDPOINT_STARTED = "/STARTED";
    public static final String ENDPOINT_STOPPED = "/STOPPED";
    public static final String ENPOINT_FPID_ALIAS = "sipEndpoint";
    public static final String ENDPOINT_FPID = "com.ibm.ws.sip";
}
