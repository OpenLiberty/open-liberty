/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.api.statistics.OAuthStatisticNames;
import com.ibm.oauth.core.api.statistics.OAuthStatistics;
import com.ibm.oauth.core.internal.statistics.OAuthStatHelper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;

/*
 * Wraps out OAuth20ComponentImpl instrumented with statistics
 */
public class OAuth20ComponetImplWrapper implements OAuth20Component {

    OAuth20Component _real;
    OAuthStatisticsImpl _stats;

    public OAuth20ComponetImplWrapper(OAuth20Component real,
            OAuthStatisticsImpl stats) {
        _real = real;
        _stats = stats;
    }

    @Override
    public OAuthComponentConfiguration getConfiguration() {
        // no stats for this
        return _real.getConfiguration();
    }

    @Override
    public OAuthComponentInstance getParentComponentInstance() {
        // no stats for this
        return _real.getParentComponentInstance();
    }

    @Override
    public OAuthStatistics getStatistics() {
        // no stats for this
        return _real.getStatistics();
    }

    @Override
    public OAuthResult processAuthorization(String username, String clientId,
            String redirectUri, String responseType, String state,
            String[] authorizedScopes, HttpServletResponse response) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_PROCESS_AUTHORIZATION);
        OAuthResult result = _real.processAuthorization(username, clientId,
                redirectUri, responseType, state, authorizedScopes, response);
        _stats.addMeasurement(statHelper);
        return result;
    }

    @Override
    public OAuthResult processAuthorization(HttpServletRequest request, HttpServletResponse response, AttributeList options) {
        OAuthStatHelper statHelper = new OAuthStatHelper(OAuthStatisticNames.OAUTH20_PROCESS_AUTHORIZATION);
        OAuthResult result = _real.processAuthorization(request, response, options);
        _stats.addMeasurement(statHelper);
        return result;
    }

    @Override
    public OAuthResult processTokenRequest(String authenticatedClient,
            HttpServletRequest request, HttpServletResponse response) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_PROCESS_TOKEN);
        OAuthResult result = _real.processTokenRequest(authenticatedClient,
                request, response);
        _stats.addMeasurement(statHelper);
        return result;
    }

    @Override
    public OAuthResult processAppTokenRequest(boolean isAppPasswordRequest, String authenticatedClient,
            HttpServletRequest request, HttpServletResponse response) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_PROCESS_APP_TOKEN);
        OAuthResult result = _real.processAppTokenRequest(isAppPasswordRequest, authenticatedClient,
                request, response);
        _stats.addMeasurement(statHelper);
        return result;
    }

    @Override
    public OAuthResult processResourceRequest(HttpServletRequest request) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_PROCESS_RESOURCE);
        OAuthResult result = _real.processResourceRequest(request);
        _stats.addMeasurement(statHelper);
        return result;
    }

    @Override
    public OAuthResult processResourceRequest(AttributeList attributeList) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_PROCESS_RESOURCE);
        OAuthResult result = _real.processResourceRequest(attributeList);
        _stats.addMeasurement(statHelper);
        return result;
    }
}
