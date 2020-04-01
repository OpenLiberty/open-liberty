/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.consumer.actions;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.consumer.JwtConsumerConstants;

import componenttest.topology.impl.LibertyServer;

public class JwtConsumerActions extends TestActions {
    protected static Class<?> thisClass = JwtConsumerActions.class;

    public static final String ACTION_INVOKE_JWT_CONSUMER = "invokeJwtConsumer";

    public Page invokeJwtConsumer(String testName, LibertyServer server, String consumerId, String jwtToken) throws Exception {
        return invokeJwtConsumer(testName, server, consumerId, jwtToken, null);

    }

    public Page invokeJwtConsumer(String testName, LibertyServer server, String consumerId, String jwtToken, List<NameValuePair> extraClaims) throws Exception {

        String jwtConsumerUrl = SecurityFatHttpUtils.getServerUrlBase(server) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT;

        List<NameValuePair> requestParams = setRequestParms(consumerId, jwtToken, extraClaims);

        WebClient webClient = new WebClient();
        Page response = null;
        try {
            response = invokeUrlWithParameters(testName, webClient, jwtConsumerUrl, requestParams);
            return response;
        } catch (Exception e) {
            Log.info(thisClass, "invokeJwtConsumer", e.getMessage());
            throw e;
        }

    }

    public List<NameValuePair> setRequestParms(String consumerId, String jwtToken, List<NameValuePair> extraClaims) throws Exception {

        List<NameValuePair> requestParms = new ArrayList<NameValuePair>();
        if (consumerId != null) {
            requestParms.add(new NameValuePair(JwtConsumerConstants.JWT_CONSUMER_PARAM_CLIENT_ID, consumerId));
        }
        if (jwtToken != null) {
            requestParms.add(new NameValuePair(JwtConsumerConstants.JWT_CONSUMER_PARAM_JWT, jwtToken));
        }
        if (extraClaims != null) {
            for (NameValuePair claim : extraClaims) {
                Log.info(thisClass, "setRequestParm", "Setting: " + claim.getName() + " value: " + claim.getValue());
                requestParms.add(new NameValuePair(claim.getName(), claim.getValue()));
            }
        }
        return requestParms;
    }
}
