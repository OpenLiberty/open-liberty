/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.udp;

/**
 * Constants used by the UDP channel for various user-seen messages.
 */
public interface UDPMessageConstants {

    /** RAS trace bundle for NLS */
    String UDP_BUNDLE = "com.ibm.ws.udpchannel.internal.resources.UDPMessages";
    /** RAS trace group name */
    String NETTY_TRACE_NAME = "Netty";

    String UDP_CHANNEL_STARTED = "CWUDP0001I";
    String UDP_CHANNEL_STOPPED = "CWUDP0002I";
    String INCORRECT_PROPERTY_VALUE = "CWUDP0003W";
    String HOST_RESOLUTION_ERROR = "CWUDP0004E";
    String BIND_FAILURE = "CWUDP0005E";
    String DNS_LOOKUP_FAILURE = "CWUDP0006I";
    String EXECUTOR_SVC_MISSING = "EXECUTOR_SVC_MISSING";
}
