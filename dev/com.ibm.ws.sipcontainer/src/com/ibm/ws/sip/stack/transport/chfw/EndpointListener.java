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

/**
 * This is used for coordination within the bundle for propagating knowledge about
 * servable aliases from the HttpChain that changes state to the VirtualHost and
 * Dispatcher that use the alias to serve requests.
 */
public interface EndpointListener {

    /**
     * Called when an endpoint is started or updated and the active list
     * of aliases changes
     * 
     * @param endpoint the endpoint this host/port combination belong to
     * @param hostName hostname of the started listener
     * @param port started port
     * @param isHttps true if port is an https port
     */
    void listenerStarted(GenericEndpointImpl endpoint, String hostName, int port, boolean isHttps);

    /**
     * Called when an endpoint is stopped or disabled. Known aliases should
     * be cleared.
     * 
     * @param endpoint the endpoint this host/port combination belong to
     * @param hostName hostname of the stopped listener
     * @param port stopped port
     * @param isHttps true if port is an https port
     */
    void listenerStopped(GenericEndpointImpl endpoint, String hostName, int port, boolean isHttps);
}
