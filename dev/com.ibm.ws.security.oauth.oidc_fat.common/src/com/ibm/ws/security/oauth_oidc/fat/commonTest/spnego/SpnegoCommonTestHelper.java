/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

/**
 *
 */
public class SpnegoCommonTestHelper extends com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTestHelpers {
    private final static Class<?> thisClass = SpnegoCommonTestHelper.class;

    public static CommonValidationTools validationTools = new CommonValidationTools();

    public Object getSpnegoLoginPage(String testcase, WebClient webClient, TestSettings settings, List<endpointSettings> headers,
                                     List<validationData> expectations) throws Exception {
        return spnegorpLoginPage(testcase, webClient, settings, headers, expectations, Constants.GETMETHOD);
    }

    public Object postSpnegoLoginPage(String testcase, WebClient webClient, TestSettings settings, List<validationData> expectations) throws Exception {
        return spnegorpLoginPage(testcase, webClient, settings, null, expectations, Constants.POSTMETHOD);
    }

    public Object spnegorpLoginPage(String testcase, WebClient webClient, TestSettings settings, List<endpointSettings> headers, List<validationData> expectations,
                                    String invokeType) throws Exception {

        String thisStep = null;
        String thisMethod = "rpLoginPage";
        msgUtils.printMethodName(thisMethod);
        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();

        Object thePage = null;

        try {
            com.gargoylesoftware.htmlunit.WebRequest requestSettings = null;

            // Invoke
            URL url = AutomationTools.getNewUrl(settings.getTestURL());
            if (invokeType.equals(Constants.GETMETHOD)) {
                Log.info(thisClass, thisMethod, "GET request");
                requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.GET);
                thisStep = Constants.GET_LOGIN_PAGE;

            } else {
                Log.info(thisClass, thisMethod, "POST request");
                requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.POST);
                thisStep = Constants.POST_LOGIN_PAGE;
            }

            requestSettings.setRequestParameters(new ArrayList());
            Map<String, String> reqParms = settings.getRequestParms();
            if (reqParms != null) {
                for (String key : reqParms.keySet()) {
                    Log.info(thisClass, thisMethod, "Key: " + key + " Value: " + reqParms.get(key));
                    setRequestParameterIfSet(requestSettings, key, reqParms.get(key));
                }
            }
            Map<String, String> reqFileParms = settings.getRequestFileParms();
            if (reqFileParms != null) {
                for (String key : reqFileParms.keySet()) {
                    Log.info(thisClass, thisMethod, "Key: " + key + " Value: " + reqFileParms.get(key));
                    setRequestParameterIfSet(requestSettings, key, reqParms.get(key));
                }
            }

            if (headers != null) {
                Map<String, String> headerParms = new HashMap<String, String>();
                for (endpointSettings header : headers) {
                    Log.info(thisClass, thisMethod, "Setting header field:  key: " + header.getKey() + " value: " + header.getValue());
                    headerParms.put(header.getKey(), header.getValue());
                    Log.info(thisClass, thisMethod, "Setting header in the webclient field:  key: " + header.getKey() + " value: " + header.getValue());
                    webClient.addRequestHeader(header.getKey(), header.getValue());
                }
                requestSettings.setAdditionalHeaders(headerParms);
            } else {
                Log.info(thisClass, thisMethod, "No header fields to add");
            }

            // Check the response
            Log.info(thisClass, thisMethod, "Outgoing request url: " + requestSettings.getUrl().toString());
            thePage = webClient.getPage(requestSettings);

            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisStep + ": ");

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, thisStep, e);
        }
        validationTools.validateResult(thePage, thisStep, expectations, settings);

        return thePage;
    }

}
