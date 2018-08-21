/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;

public class TAIRequestHelper {

    private static TraceComponent tc = Tr.register(TAIRequestHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    SocialWebUtils webUtils = new SocialWebUtils();

    /**
     * Creates a new {@link SocialTaiRequest} object and sets the object as an attribute in the request object provided.
     *
     * @param request
     * @return
     */
    public SocialTaiRequest createSocialTaiRequestAndSetRequestAttribute(HttpServletRequest request) {
        SocialTaiRequest socialTaiRequest = new SocialTaiRequest(request);
        request.setAttribute(Constants.ATTRIBUTE_TAI_REQUEST, socialTaiRequest);
        return socialTaiRequest;
    }

    /**
     * Returns whether the provided request should be handled by the social login TAI, based on the request path and information
     * in the {@link SocialTaiRequest} object provided.
     *
     * @param request
     * @param socialTaiRequest
     * @return
     */
    public boolean requestShouldBeHandledByTAI(HttpServletRequest request, SocialTaiRequest socialTaiRequest) {
        // 241526 don't process jmx requests with this interceptor
        if (isJmxConnectorRequest(request)) {
            return false;
        }
        String loginHint = webUtils.getLoginHint(request);
        socialTaiRequest = setSocialTaiRequestConfigInfo(request, loginHint, socialTaiRequest);
        return socialTaiRequest.hasServices();
    }

    boolean isJmxConnectorRequest(HttpServletRequest request) {
        String ctxPath = request.getContextPath();
        return "/IBMJMXConnectorREST".equals(ctxPath);
    }

    SocialTaiRequest setSocialTaiRequestConfigInfo(HttpServletRequest request, String loginHintParameter, SocialTaiRequest socialTaiRequest) {
        if (loginHintParameter == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Login hint parameter not provided, so will set generic config information for SocialTaiRequest object");
            }
            return setGenericAndFilteredConfigTaiRequestInfo(request, socialTaiRequest);
        }

        return setSpecificConfigTaiRequestInfo(request, loginHintParameter, socialTaiRequest);
    }

    SocialTaiRequest setGenericAndFilteredConfigTaiRequestInfo(HttpServletRequest request, SocialTaiRequest socialTaiRequest) {
        if (socialTaiRequest == null) {
            socialTaiRequest = createSocialTaiRequestAndSetRequestAttribute(request);
        }
        Iterator<SocialLoginConfig> services = getConfigServices();
        return setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(request, socialTaiRequest, services);
    }

    SocialTaiRequest setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(HttpServletRequest request, SocialTaiRequest socialTaiRequest, Iterator<SocialLoginConfig> services) {
        if (services == null) {
            return socialTaiRequest;
        }
        if (socialTaiRequest == null) {
            socialTaiRequest = createSocialTaiRequestAndSetRequestAttribute(request);
        }
        // For each SocialLoginConfig, determine whether the config does not have an auth filter configured, or has an auth filter that matches this request
        while (services.hasNext()) {
            SocialLoginConfig socialLoginConfig = services.next();
            AuthenticationFilter authFilter = socialLoginConfig.getAuthFilter();
            if (authFilter != null) {
                if (authFilter.isAccepted(request)) {
                    socialTaiRequest.addFilteredConfig(socialLoginConfig);
                }
            } else {
                socialTaiRequest.addGenericConfig(socialLoginConfig);
            }
        }
        return socialTaiRequest;
    }

    SocialTaiRequest setSpecificConfigTaiRequestInfo(HttpServletRequest request, String loginHintParam, SocialTaiRequest socialTaiRequest) {
        if (socialTaiRequest == null) {
            socialTaiRequest = createSocialTaiRequestAndSetRequestAttribute(request);
        }

        SocialLoginConfig config = getConfigAssociatedWithRequestAndId(request, getConfigIdFromLoginHintParam(loginHintParam));
        if (config == null) {
            // error handling-- the specified service is not found
            SocialLoginException socialLoginException = new SocialLoginException("SOCIAL_LOGIN_NO_SUCH_PROVIDER", null, new Object[] { loginHintParam });
            socialTaiRequest.setTaiException(socialLoginException);
        } else {
            socialTaiRequest.setSpecifiedConfig(config);
        }
        return socialTaiRequest;
    }

    /**
     * Returns the original configuration ID that corresponds to the provided login hint value. The login hint value should be an
     * obscured configuration ID. Obscured configuration IDs are used in user-facing situations where internal configuration data
     * either must not or should not be exposed.
     *
     * @param loginHintParam
     *            A configuration ID that has been obscured via the ObscuredConfigIdManager class.
     */
    String getConfigIdFromLoginHintParam(String loginHintParam) {
        return SocialLoginTAI.getConfigIdFromObscuredId(loginHintParam);
    }

    SocialLoginConfig getConfigAssociatedWithRequestAndId(HttpServletRequest request, String configId) {
        SocialLoginConfig socialLoginConfig = getConfig(configId);
        if (!configAuthFilterMatchesRequest(request, socialLoginConfig)) {
            // The config with the specified ID isn't configured to service this request
            socialLoginConfig = null;
        }
        return socialLoginConfig;
    }

    Iterator<SocialLoginConfig> getConfigServices() {
        return SocialLoginTAI.socialLoginConfigRef.getServices();
    }

    SocialLoginConfig getConfig(String configId) {
        return SocialLoginTAI.getSocialLoginConfig(configId);
    }

    boolean configAuthFilterMatchesRequest(HttpServletRequest request, SocialLoginConfig config) {
        if (config == null) {
            return false;
        }
        AuthenticationFilter authFilter = config.getAuthFilter();
        if (authFilter != null) {
            if (!authFilter.isAccepted(request)) {
                // Specified configuration is present but its auth filter is not configured to service this request
                return false;
            }
        }
        return true;
    }

}
