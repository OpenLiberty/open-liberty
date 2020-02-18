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
package com.ibm.ws.ui.internal.rest.v1.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler;
import com.ibm.ws.ui.internal.rest.exceptions.BadRequestException;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.ws.ui.internal.v1.utils.URLUtility;
import com.ibm.ws.ui.internal.v1.utils.URLUtilityImpl;
import com.ibm.ws.ui.internal.v1.utils.Utils;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the utility methods that different parts of the UI will need to use.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter/v1/utils/url</p>
 */
public class URLUtils extends CommonJSONRESTHandler implements V1UtilsConstants {
    private static final TraceComponent tc = Tr.register(URLUtils.class);

    private final URLUtility utils;

    /**
     * Sets the URLUtils to handle itself and child resources.
     */
    public URLUtils() {
        super(URL_UTILS_PATH, true, false);
        utils = new URLUtilityImpl();
    }

    /**
     * Sets the URLUtils to handle itself and child resources.
     */
    public URLUtils(URLUtility utils) {
        super(URL_UTILS_PATH, true, false);
        this.utils = utils;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isKnownChildResource(String child, RESTRequest request) {
        if ("getTool".equals(child)) {
            return true;
        } else if ("getStatus".equals(child)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns a JSON object with the defined utilities URLs.
     *
     * @param request The RESTRequest from handleRequest
     * @return A JSON object with the defined utilities.
     */
    /** {@inheritDoc} */
    @Override
    public Map<String, String> getBase(RESTRequest request, RESTResponse response) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        Map<String, String> map = new HashMap<String, String>();
        String url = request.getURL();
        map.put("getTool", url + (url.endsWith("/") ? "getTool" : "/getTool"));
        map.put("getStatus", url + (url.endsWith("/") ? "getStatus" : "/getStatus"));

        return map;
    }

    /**
     * Gets the URL from the request parameter. If the URL is not defined or is
     * somehow bad, a descriptive BadRequestException is thrown.
     *
     * @param request
     * @return
     * @throws BadRequestException
     */
    @FFDCIgnore(MalformedURLException.class)
    private URL getURLParameter(String operation, RESTRequest request) throws BadRequestException {
        String toolURL = request.getParameter("url");
        if (toolURL == null) {
            Message error = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "OP_REQUIRES_URL", operation));
            throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, error);
        }

        URL urlObj = null;
        try {
            urlObj = Utils.getURL(toolURL);
        } catch (MalformedURLException e) {
            Message error = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "OP_BAD_URL", operation, e.getMessage()));
            throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, error);
        }
        return urlObj;
    }

    /**
     * Retrieve the HTTP status of a GET request for the provided URL.
     * The URL to GET is specified in the parameter 'url'.
     *
     * @param request The RESTRequest from handleRequest
     * @return
     * @throws RESTException
     */
    private Object getStatus(RESTRequest request) throws RESTException {
        final URL url = getURLParameter("getStatus", request);
        return utils.getStatus(url);
    }

    /**
     * Attempt to construct a Tool object to represent the specified URL.
     * The URL to GET is specified in the parameter 'url'.
     *
     * @param request The RESTRequest from handleRequest
     * @return
     * @throws RESTException
     */
    private Object getTool(RESTRequest request) throws RESTException {
        final URL url = getURLParameter("getTool", request);
        return utils.analyzeURL(url);
    }

    /** {@inheritDoc} */
    @Override
    public Object getChild(RESTRequest request, RESTResponse response, String childResource) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if ("getStatus".equals(childResource)) {
            return getStatus(request);
        } else if ("getTool".equals(childResource)) {
            return getTool(request);
        } else {
            throw new NoSuchResourceException();
        }
    }

}
