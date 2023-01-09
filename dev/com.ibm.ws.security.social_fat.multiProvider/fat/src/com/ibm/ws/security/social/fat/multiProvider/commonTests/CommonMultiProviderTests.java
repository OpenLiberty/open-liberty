/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.multiProvider.commonTests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.web.WebRequestUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

public class CommonMultiProviderTests extends SocialCommonTest {

    public static Class<?> thisClass = CommonMultiProviderTests.class;

    public WebRequestUtils webReqUtils = new WebRequestUtils();

    /**
     * Returns a map of request parameters to use in the original protected resource request.
     */
    protected Map<String, List<String>> getRequestParameters() {
        Map<String, List<String>> paramMap = new HashMap<String, List<String>>();
        paramMap.put("abc", Arrays.asList("123"));
        paramMap.put("Some more complicated param!", Arrays.asList("`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?", "And its other value"));
        paramMap.put("empty_param_value", Arrays.asList(""));
        return paramMap;
    }

    /**
     * Adds all of the specified parameters as request parameters to the protected resource URL in the provided settings object.
     */
    protected SocialTestSettings addParametersToProtectedResourceUrl(SocialTestSettings updatedSocialTestSettings, Map<String, List<String>> paramMap) throws Exception {
        String method = "addParametersToProtectedResourceUrl";

        String protectedResourceUrl = updatedSocialTestSettings.getProtectedResource();
        String queryString = webReqUtils.buildUrlQueryString(paramMap);
        String protectedResourceWithParams = protectedResourceUrl + "?" + queryString;
        Log.info(thisClass, method, "Created new protected resource URL with request parameters: " + protectedResourceWithParams);

        updatedSocialTestSettings.setProtectedResource(protectedResourceWithParams);
        return updatedSocialTestSettings;
    }

    /**
     * Adds expectations that check for the presence of all of the specified request parameters and values in the request URL.
     */
    protected List<validationData> addQueryParameterExpectations(List<validationData> expectations, Map<String, List<String>> paramMap, String action) throws Exception {
        for (Entry<String, List<String>> param : paramMap.entrySet()) {
            String key = param.getKey();
            List<String> values = param.getValue();

            for (String value : values) {
                expectations = addQueryParameterExpectation(expectations, action, webReqUtils.getRegexSearchStringForUrlQueryValue(key), webReqUtils.getRegexSearchStringForUrlQueryValue(value));
            }
        }
        return expectations;
    }

    /**
     * Verifies that the provided key and value appear in the URL of the response for the specified action.
     */
    protected List<validationData> addQueryParameterExpectation(List<validationData> expectations, String action, String keyRegex, String valueRegex) throws Exception {
        String checkFor = keyRegex + "=" + valueRegex;
        return vData.addExpectation(expectations, action, SocialConstants.RESPONSE_URL, SocialConstants.STRING_MATCHES, "Did not find expected key and value for parameter [" + keyRegex + "] in protected resource request URL for action [" + action + "].", null, checkFor);
    }

}
