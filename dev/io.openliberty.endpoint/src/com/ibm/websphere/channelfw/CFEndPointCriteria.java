/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.channelfw;

/**
 * The methods of this interface describe requirements needed to
 * determine an appropriate endpoint to connect to. They are used
 * in conjunction with request data to determine the appropriate endpoint.
 * Comparison preferences are chain name first, then virtual host, then
 * factory classes, then channel accessors, then ssl, and finally local.
 * Each one narrows down the list of endpoints.
 */
public interface CFEndPointCriteria {

    /**
     * Access the interface class that will be used to communicate
     * with the first channel of the outbound chain that will be used
     * to connect to the endpoint.
     * 
     * @return interface class of first outbound channel
     */
    Class<?> getChannelAccessor();

    /**
     * This is the name of the inbound chain on the server side that the client
     * wants to talk to.
     * 
     * @return String
     */
    String getChainName();

    /**
     * Access an optional ordered list of channel factories that will
     * be needed to make the connection. This is where things like
     * tunneling and SSL can be specified. If no channel factories
     * must be specified, then null should return.
     * 
     * @return list of channel factories needed for connection
     */
    Class<?>[] getOptionalChannelFactories();

    /**
     * Specifies whether the chain to be searched for must have SSL capabilities.
     * 
     * @return true of Chain must have SSL capabilities
     */
    boolean isSSLRequired();

    /**
     * Query the optional virtual host target of this criteria.
     * 
     * @return String, null if not necessary
     */
    String getVirtualHost();
}
