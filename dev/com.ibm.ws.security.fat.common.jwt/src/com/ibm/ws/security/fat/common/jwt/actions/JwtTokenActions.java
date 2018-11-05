/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt.actions;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.topology.impl.LibertyServer;

public class JwtTokenActions extends TestActions {
    protected static Class<?> thisClass = JwtTokenActions.class;

    // anyone calling this method needs to add upn to the extraClaims that it passes in (if they need it)
    public String getJwtTokenUsingBuilder(String testName, LibertyServer server, String builderId, List<NameValuePair> extraClaims) throws Exception {

        String jwtBuilderUrl = SecurityFatHttpUtils.getServerUrlBase(server) + "/jwtbuilder/build";

        List<NameValuePair> requestParams = setRequestParms(builderId, extraClaims);

        WebClient webClient = new WebClient();
        Page response = invokeUrlWithParameters(testName, webClient, jwtBuilderUrl, requestParams);
        Log.info(thisClass, testName, "JWT builder app response: " + WebResponseUtils.getResponseText(response));

        Cookie jwtCookie = webClient.getCookieManager().getCookie("JWT");
        Log.info(thisClass, testName, "Built JWT cookie: " + jwtCookie);
        Log.info(thisClass, testName, "Cookie value: " + jwtCookie.getValue());
        return jwtCookie.getValue();

    }

    public List<NameValuePair> setRequestParms(String builderId, List<NameValuePair> extraClaims) throws Exception {

        List<NameValuePair> requestParms = new ArrayList<NameValuePair>();
        requestParms.add(new NameValuePair(JwtConstants.PARAM_BUILDER_ID, builderId));
        if (extraClaims != null) {
            for (NameValuePair claim : extraClaims) {
                Log.info(thisClass, "setRequestParm", "Setting: " + claim.getName() + " value: " + claim.getValue());
                requestParms.add(new NameValuePair(claim.getName(), claim.getValue()));
            }
        }
        return requestParms;
    }
}
