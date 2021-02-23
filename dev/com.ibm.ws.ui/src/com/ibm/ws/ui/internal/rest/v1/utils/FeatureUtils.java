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
package com.ibm.ws.ui.internal.rest.v1.utils;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the utility methods that different parts of the UI will need to use.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter/v1/utils/feature</p>
 */
public class FeatureUtils extends CommonJSONRESTHandler {
    private final IFeatureToolService featureToolService;

    public FeatureUtils(IFeatureToolService featureToolService) {
        super(V1UtilsConstants.FEATURE_UTILS_PATH, true, false);
        this.featureToolService = featureToolService;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getBase(RESTRequest request, RESTResponse response) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        Map<String, String> map = new HashMap<String, String>();
        String url = request.getURL();
        map.put("{featureName}", url + (url.endsWith("/") ? "{featureName}" : "/{featureName}"));

        return map;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Boolean> getChild(RESTRequest request, RESTResponse response, String featureToFind) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        Map<String, Boolean> data = new HashMap<String, Boolean>();
        data.put("provisioned", featureToolService.isFeatureProvisioned(featureToFind));
        return data;
    }
}
