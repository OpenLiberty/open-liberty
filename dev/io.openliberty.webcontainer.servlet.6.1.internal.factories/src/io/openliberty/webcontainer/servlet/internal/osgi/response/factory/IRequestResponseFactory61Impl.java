/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet.internal.osgi.response.factory;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.webcontainer.osgi.request.IRequestFactory;
import com.ibm.ws.webcontainer.osgi.response.IResponseFactory;
import com.ibm.wsspi.http.HttpInboundConnection;

import io.openliberty.webcontainer60.osgi.request.IRequest60Impl;
import io.openliberty.webcontainer60.osgi.response.IResponse60Impl;

@Component(property = { "service.vendor=IBM", "service.ranking:Integer=61", "servlet.version=6.1" })
public class IRequestResponseFactory61Impl implements IRequestFactory, IResponseFactory {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.osgi.response.IResponseFactory#createRequest(com.ibm.websphere.servlet.request.IRequest, com.ibm.wsspi.http.HttpInboundConnection)
     */
    @Override
    public IRequest createRequest(HttpInboundConnection inboundConnection) {
        return new IRequest60Impl(inboundConnection);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.osgi.request.IRequestFactory#createRequest(com.ibm.wsspi.http.HttpInboundConnection)
     */
    @Override
    public IResponse createResponse(IRequest ireq, HttpInboundConnection inboundConnection) {
        return new IResponse60Impl(ireq, inboundConnection);
    }
}
