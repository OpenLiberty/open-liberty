/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.twitter.TwitterConstants;
import com.ibm.ws.security.social.twitter.TwitterTokenServices;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.wsspi.security.tai.TAIResult;

public class TwitterLoginFlow {

    public static final TraceComponent tc = Tr.register(TwitterLoginFlow.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    TAIWebUtils taiWebUtils = new TAIWebUtils();
    TwitterTokenServices twitterTokenServices = new TwitterTokenServices();
    SocialWebUtils webUtils = new SocialWebUtils();
    TAIJwtUtils taiJwtUtils = new TAIJwtUtils();

    public TAIResult handleTwitterRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws WebTrustAssociationFailedException {
        // If the flow has already been initiated, the access token and access token secret should have been cached
        String accessToken = webUtils.getAndClearCookie(request, response, TwitterConstants.COOKIE_NAME_ACCESS_TOKEN);
        String accessTokenSecret = webUtils.getAndClearCookie(request, response, TwitterConstants.COOKIE_NAME_ACCESS_TOKEN_SECRET);

        if (accessToken == null || accessTokenSecret == null) {
            // Obtain request token from Twitter. User should be redirected to authorize application to access user data.
            getTwitterRequestToken(request, response, config);
        } else {
            taiWebUtils.restorePostParameters(request);

            // Obtain user data based on the cached access token to create a subject
            return createSubjectFromTwitterCredentials(response, config, accessToken, accessTokenSecret);
        }
        return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
    }

    protected void getTwitterRequestToken(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) {
        taiWebUtils.savePostParameters(request);

        // State value is used as a kind of unique identifier for this request
        String stateValue = taiWebUtils.createStateCookie(request, response);

        // Set the callback URL to redirect to the appropriate URL for this configuration
        String callbackUrl = taiWebUtils.getRedirectUrl(request, config);
        twitterTokenServices.getRequestToken(request, response, callbackUrl, stateValue, config);
    }

    protected TAIResult createSubjectFromTwitterCredentials(HttpServletResponse response, SocialLoginConfig config, String accessToken, @Sensitive String accessTokenSecret) throws WebTrustAssociationFailedException {
        // Obtain Twitter profile information for the corresponding access token
        Map<String, Object> userApiResponseMap = twitterTokenServices.verifyCredentials(response, accessToken, accessTokenSecret, config);
        if (userApiResponseMap == null) {
            // Error message and status code already logged
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }

        String userApiResponse = JsonUtils.toJson(userApiResponseMap);
        if (userApiResponse == null || userApiResponse.isEmpty()) {
            Tr.error(tc, "USER_API_RESPONSE_NULL_OR_EMPTY", new Object[] { config.getUniqueId() });
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }

        return createResultFromUserApiResponse(response, config, userApiResponseMap, userApiResponse);
    }

    @FFDCIgnore(SocialLoginException.class)
    TAIResult createResultFromUserApiResponse(HttpServletResponse response, SocialLoginConfig config, Map<String, Object> userApiResponseMap, String userApiResponse) throws WebTrustAssociationFailedException {
        String token = (String) userApiResponseMap.get(ClientConstants.ACCESS_TOKEN);
        JwtToken issuedJwtToken = null;
        try {
            if (config.getJwtRef() != null) {
                issuedJwtToken = taiJwtUtils.createJwtTokenFromJson(userApiResponse, config, false); //oauth login flow
            }
        } catch (Exception e) {
            Tr.error(tc, "AUTH_CODE_FAILED_TO_CREATE_JWT", new Object[] { config.getUniqueId(), e.getLocalizedMessage() });
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }

        try {
            TAISubjectUtils subjectUtils = getTAISubjectUtils(token, null, issuedJwtToken, userApiResponseMap, userApiResponse);
            return subjectUtils.createResult(response, config);
        } catch (SocialLoginException e) {
            Tr.error(tc, "TWITTER_ERROR_CREATING_RESULT", new Object[] { config.getUniqueId(), e.getLocalizedMessage() });
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        } catch (Exception e) {
            Tr.error(tc, "TWITTER_ERROR_CREATING_RESULT", new Object[] { config.getUniqueId(), e.getLocalizedMessage() });
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }
    }

    TAISubjectUtils getTAISubjectUtils(@Sensitive String accessToken, JwtToken jwt, JwtToken issuedJwt, @Sensitive Map<String, Object> userApiResponseTokens, String userApiResponse) {
        return new TAISubjectUtils(accessToken, jwt, issuedJwt, userApiResponseTokens, userApiResponse);
    }

}
