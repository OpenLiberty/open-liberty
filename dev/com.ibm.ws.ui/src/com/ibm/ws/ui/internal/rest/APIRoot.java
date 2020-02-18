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

package com.ibm.ws.ui.internal.rest;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the API base resource for the adminCenter REST API.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter</p>
 */
public class APIRoot extends CommonJSONRESTHandler {

    /**
     * Sets the APIRoot to handle only itself.
     */
    public APIRoot() {
        super(ADMIN_CENTER_ROOT_PATH, false, false);
    }

    /**
     * Constructs a JSON Object (a Map) with references to the known child
     * resources.
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
        map.put("v1", url + (url.endsWith("/") ? "v1" : "/v1"));

        return map;
    }

}
