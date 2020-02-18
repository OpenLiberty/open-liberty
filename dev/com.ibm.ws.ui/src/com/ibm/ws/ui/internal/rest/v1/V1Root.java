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
package com.ibm.ws.ui.internal.rest.v1;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the API version 1 base resource for the adminCenter REST API.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter/v1</p>
 */
public class V1Root extends CommonJSONRESTHandler implements V1Constants {

    /**
     * Sets the V1Root to handle itself and child resources.
     */
    public V1Root() {
        super(V1_ROOT_PATH, true, false);
    }

    /**
     * {@inheritDoc} <p>The only child V1Root has is 'coffee'.</p>
     */
    @Override
    public boolean isKnownChildResource(String child, RESTRequest request) {
        return "coffee".equals(child);
    }

    /**
     * Set response with a JSON Object pointing to the known V1 resources.
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @throws RESTException
     */
    @Override
    public Map<String, String> getBase(RESTRequest request, RESTResponse response) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        Map<String, String> map = new HashMap<String, String>();
        String url = request.getURL();
        map.put("catalog", url + (url.endsWith("/") ? "catalog" : "/catalog"));
        map.put("toolbox", url + (url.endsWith("/") ? "toolbox" : "/toolbox"));
        map.put("utils", url + (url.endsWith("/") ? "utils" : "/utils"));
        map.put("icons", url + (url.endsWith("/") ? "icons" : "/icons"));

        return map;
    }

    /**
     * If the requested resource is exactly
     * Constructs a JSON Object (a Map) with references to the known child
     * resources.
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    @Override
    public Map<String, String> getChild(RESTRequest request, RESTResponse response, String childResource) throws RESTException {
        if ("coffee".equals(childResource)) {
            // Yup, this is an Easter egg! 418 is an April Fool's day status code.
            // Only match if its EXACTLY /coffee... trailing slashes need not apply
            throw new RESTException(418, MEDIA_TYPE_TEXT_PLAIN, "I'm a teapot!");
        } else {
            throw new NoSuchResourceException();
        }
    }

}
