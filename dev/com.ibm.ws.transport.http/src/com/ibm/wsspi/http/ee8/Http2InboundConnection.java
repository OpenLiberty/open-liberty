/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.ee8;

import java.util.Enumeration;

import com.ibm.wsspi.http.HttpInboundConnection;

/**
 * Representation of an inbound HTTP connection that the dispatcher will provide
 * to containers.
 */
public interface Http2InboundConnection extends HttpInboundConnection {

    /**
     * Determine if a request is an http2 upgrade request
     */
    boolean isHTTP2UpgradeRequest(Enumeration<String> connection, Enumeration<String> upgrade, boolean checkEnabledOnly);

    /**
     * Determine if a map of headers contains an http2 upgrade header
     */
    boolean handleHTTP2UpgradeRequest(String http2Settings);
}
