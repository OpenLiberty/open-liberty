/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext;

/**
 * Factory for all of the pooled objects used in the HTTP channel.
 *
 */
public class HttpObjectFactory {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpObjectFactory.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /**
     * Constructor of the object factory.
     */
    public HttpObjectFactory() {
        // trigger the FFDC registration if it hasn't already happened
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "New HTTP object factory created: " + this);
        }
    }

    /**
     * Retrieve an uninitialized request message.
     *
     * @return HttpRequestMessageImpl
     */
    public HttpRequestMessageImpl getRequest() {
        HttpRequestMessageImpl req = new HttpRequestMessageImpl();
        return req;
    }

    /**
     * Retrieve an incoming request object from the factory.
     *
     * @param hsc
     * @return HttpRequestMessageImpl
     */
    public HttpRequestMessageImpl getRequest(HttpInboundServiceContext hsc) {
        HttpRequestMessageImpl req = getRequest();
        req.init(hsc);
        return req;
    }

    /**
     * Retrieve an outgoing request object from the factory.
     *
     * @param hsc
     * @return HttpRequestMessageImpl
     */
    public HttpRequestMessageImpl getRequest(HttpOutboundServiceContext hsc) {
        HttpRequestMessageImpl req = getRequest();
        req.init(hsc);
        return req;
    }

    /**
     * Retrieve an uninitialized response message.
     *
     * @return HttpResponseMessageImpl
     */
    public HttpResponseMessageImpl getResponse() {
        HttpResponseMessageImpl resp = new HttpResponseMessageImpl();

        return resp;
    }

    /**
     * Retrieve an outgoing response object from the factory.
     *
     * @param hsc
     * @return HttpResponseMessageImpl
     */
    public HttpResponseMessageImpl getResponse(HttpInboundServiceContext hsc) {
        HttpResponseMessageImpl resp = getResponse();
        resp.init(hsc);
        return resp;
    }

    /**
     * Retrieve an incoming response object from the factory.
     *
     * @param hsc
     * @return HttpResponseMessageImpl
     */
    public HttpResponseMessageImpl getResponse(HttpOutboundServiceContext hsc) {
        HttpResponseMessageImpl resp = getResponse();
        resp.init(hsc);
        return resp;
    }

    /**
     * Get a new trailers object. init() is not called on the object so the
     * default values for certain variables are used (byte cache size, etc).
     *
     * @return HttpTrailersImpl
     */
    public HttpTrailersImpl getTrailers() {

        HttpTrailersImpl hdrs = new HttpTrailersImpl();
        hdrs.setFactory(this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getTrailers: " + hdrs);
        }
        return hdrs;
    }

}