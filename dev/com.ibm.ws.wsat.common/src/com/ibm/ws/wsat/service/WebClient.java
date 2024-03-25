/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.wsat.service;

import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsat.common.impl.WSATEndpoint;
import com.ibm.ws.wsat.service.impl.WSATConfigServiceImpl;
import com.ibm.ws.wsat.service.impl.WebClientImpl;

/**
 * This class contains the client code for invoking the WS-AT and WS-Coord
 * protocol web services.
 */
public abstract class WebClient {
    private static final TraceComponent TC = Tr.register(WebClient.class);

    private static WebClient testClient;

    public static WebClient getWebClient(WSATEndpoint toEpr, WSATEndpoint fromEpr) {
        return new WebClientImpl(toEpr, fromEpr);
    }

    protected void setTimeouts(Object bp) {
        long timeout = WSATConfigServiceImpl.getInstance().getAsyncResponseTimeout();
        Map<String, Object> requestContext = ((BindingProvider) bp).getRequestContext();
        requestContext.put("javax.xml.ws.client.connectionTimeout", timeout);
        requestContext.put("javax.xml.ws.client.receiveTimeout", timeout);
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

    public abstract void setMisrouting(boolean b);
}
