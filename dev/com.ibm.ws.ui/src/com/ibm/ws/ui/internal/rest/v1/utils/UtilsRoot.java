/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest.v1.utils;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.ws.ui.internal.rest.v1.V1Constants;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>This class is designed to hold utility methods that different parts of
 * the UI will need to use.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter/v1/utils</p>
 */
public class UtilsRoot extends CommonJSONRESTHandler implements V1Constants {

    public UtilsRoot() {
        super(UTILS_PATH, false, false);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getBase(RESTRequest request, RESTResponse response) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        Map<String, String> map = new HashMap<String, String>();
        String url = request.getURL();
        map.put("url", url + (url.endsWith("/") ? "url" : "/url"));
        map.put("feature", url + (url.endsWith("/") ? "feature" : "/feature"));

        return map;
    }
}
