/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.filter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.response.StoredResponse;

/**
 * 
 * 
 * ChainedResponse is a response object that can be instantiated
 * by any servlet and used/passed as a standard HttpResponse. The
 * data that is written to this response can then be retrieved as
 * a request to passed into another servlet in a chain.
 *
 * The ChainedRequest must be instantiated with the original request
 * and response objects so that attributes and sessions associated
 * with the chain can be propagated correctly (Deprecated since WebSphere 6.0).
 * 
 * @deprecated Application developers requiring this functionality
 *  should implement this using javax.servlet.filter classes.
 *
 * @ibm-api 
 */
public class ChainedResponse extends StoredResponse
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3256438097326520118L;
	private static final String AUTO_XFER_HEADERS_ATTR = "com.ibm.websphere.servlet.filter.ChainedResponse.auto_transfer_headers";
    private HttpServletRequest _req;
    private HttpServletResponse _resp;

    /**
     * Create a chained response.
     * @param req the original request.
     * @param the original response.
     */
    public ChainedResponse(HttpServletRequest req,
                           HttpServletResponse resp)
    {
        _req = req;
        _resp = resp;
        setResponse(resp);
    }

    /**
     * Returns a chained request that contains the data that was written to this response.
     */
    @SuppressWarnings("unchecked")
    public HttpServletRequest getChainedRequest() throws IOException, ServletException{
        if (super.containsError())
        {
            throw super.getError();
        }
        ChainedRequest req = new ChainedRequest(this, _req);
        //transfer any auto transfer headers
        Hashtable headers = getAutoTransferringHeaders();
        Enumeration names = headers.keys();
        while (names.hasMoreElements())
        {
            String name = (String)names.nextElement();
            String value = (String)headers.get(name);
            req.setHeader(name, value);
        }

        //get headers from response and add to request
        Iterable<String> headerNames = getHeaderNames();
        for (String name:headerNames)
        {
            String value = (String)getHeader(name);
            req.setHeader(name, value);
        }


        return req;
    }

    public String encodeRedirectURL(String url)
    {
        return _resp.encodeRedirectURL(url);
    }

    public String encodeRedirectUrl(String url)
    {
        return _resp.encodeRedirectUrl(url);
    }

    public String encodeURL(String url)
    {
        return _resp.encodeURL(url);
    }

    public String encodeUrl(String url)
    {
        return _resp.encodeUrl(url);
    }

    //defect 55215 - support auto transfer of client headers.
    /**
     * Set a header that should be automatically transferred to all requests
     * in a chain.  These headers will be backed up in a request attribute that
     * will automatically read and transferred by all ChainedResponses.  This method
     * is useful for transparently transferring the original headers sent by the client
     * without forcing servlets to be specially written to transfer these headers.
     */
    @SuppressWarnings("unchecked")
    public void setAutoTransferringHeader(String name, String value)
    {
        Hashtable headers = getAutoTransferringHeaders();
        headers.put(name, value);
//        setHeader(name, value);
    }

    public HttpServletResponse getProxiedHttpServletResponse()
    {
        return _resp;
    }

    /**
     * Get the headers that are designated as auto-transfer.
     */
    @SuppressWarnings("unchecked")
    private Hashtable getAutoTransferringHeaders()
    {
        Hashtable headers = (Hashtable)_req.getAttribute(AUTO_XFER_HEADERS_ATTR);
        if (headers == null)
        {
            headers = new Hashtable();
            _req.setAttribute(AUTO_XFER_HEADERS_ATTR, headers);
        }
        return headers;
    }
}

