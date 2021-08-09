/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.access;

/**
 *
 */
public class TransportConstants {

    // following added for WebSockets
    public static final String UPGRADED_CONNECTION = "UpgradedConnection";

    public static final String UPGRADED_WEB_CONNECTION_OBJECT = "UpgradedWebConnectionObject";

    // for HTTP2 and other protocols that need the DispatcherLink to close when not using the more
    // generic "user upgraded protocol" listeners below
    public static final String UPGRADED_WEB_CONNECTION_NEEDS_CLOSE = "UpgradedWebConnectionNeedsClose";

    // following added for Upgrade
    public static final String CLOSE_NON_UPGRADED_STREAMS = "CloseNonUpgradedStreams";
    public static final String UPGRADED_LISTENER = "UpgradedListener";
    public static final String CLOSE_UPGRADED_WEBCONNECTION = "CloseUpgradedWebConnection";

}
