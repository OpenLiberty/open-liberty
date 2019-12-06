/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.service;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.ws.wsat.common.impl.WSATEndpoint;
import com.ibm.ws.wsat.service.impl.WebClientImpl;

/**
 * This class contains the client code for invoking the WS-AT and WS-Coord
 * protocol web services.
 */
public abstract class WebClient {

    private static WebClient testClient = null;

    /*
     * Factory to return WebClient instances. This allows us to consider caching the clients
     * (if that makes sense) and allows for overriding for unit tests.
     */
    public static WebClient getWebClient(WSATEndpoint toEpr, WSATEndpoint fromEpr) {
        if (testClient != null) {
            return testClient;
        }
        return new WebClientImpl(toEpr, fromEpr);
    }

    /*
     * WS-Coor register
     */
    public abstract EndpointReferenceType register(EndpointReferenceType participant) throws WSATException;

    /*
     * WS-AT participant request
     */
    public abstract void prepare() throws WSATException;

    public abstract void rollback() throws WSATException;

    public abstract void commit() throws WSATException;

    /*
     * WS-AT coordinator responses
     */
    public abstract void prepared() throws WSATException;

    public abstract void readOnly() throws WSATException;

    public abstract void aborted() throws WSATException;

    public abstract void committed() throws WSATException;

}
