/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.tai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.http.AuthUtils;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.config.MpConstants;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.security.mp.jwt.impl.utils.MicroProfileJwtTaiRequest;

/**
 *
 */
public class TAIRequestHelper {

    private static TraceComponent tc = Tr.register(TAIRequestHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String ATTRIBUTE_TAI_REQUEST = "MPJwtTaiRequest";

    private static final String Authorization_Header = "Authorization";
    private static final String APPLICATION_AUTH_METHOD = "com.ibm.ws.security.tai.appAuthType";
    public final static String REQ_METHOD_POST = "POST";
    public final static String REQ_CONTENT_TYPE_NAME = "Content-Type";
    public final static String REQ_CONTENT_TYPE_APP_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String AUTHN_TYPE = "MP-JWT";
    public static final String KEY_AUTHORIZATION_HEADER_SCHEME = "authorizationHeaderScheme";

    private final AuthUtils authUtils = new AuthUtils();

    /**
     * Creates a new {@link MicroProfileJwtTaiRequest} object and sets the object as an attribute in the request object provided.
     *
     * @param request
     * @return
     */
    public MicroProfileJwtTaiRequest createMicroProfileJwtTaiRequestAndSetRequestAttribute(HttpServletRequest request) {
        String methodName = "createMicroProfileJwtTaiRequestAndSetRequestAttribute";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request);
        }
        MicroProfileJwtTaiRequest mpJwtTaiRequest = new MicroProfileJwtTaiRequest(request);
        request.setAttribute(ATTRIBUTE_TAI_REQUEST, mpJwtTaiRequest);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, mpJwtTaiRequest);
        }
        return mpJwtTaiRequest;
    }

    /**
     * Returns whether the provided request should be handled by the microprofile jwt TAI, based on the request path and
     * information in the {@link McroProfileJwtTaiRequest} object provided.
     */
    public boolean requestShouldBeHandledByTAI(HttpServletRequest request, MicroProfileJwtTaiRequest mpJwtTaiRequest) {
        String methodName = "requestShouldBeHandledByTAI";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, mpJwtTaiRequest);
        }

        mpJwtTaiRequest = setTaiRequestConfigInfo(request, mpJwtTaiRequest, isNewMpJwtAndMpConfig(request));
        boolean result = false;
        boolean ignoreAppAuthMethod = true;

        MicroProfileJwtConfig mpJwtConfig = null;
        try {
            mpJwtConfig = mpJwtTaiRequest.getOnlyMatchingConfig();
        } catch (MpJwtProcessingException e) {

        }
        if (mpJwtConfig != null) {
            if (shouldDeferToJwtSso(request, mpJwtConfig, mpJwtTaiRequest.getJwtSsoConfig())) {
                return false;
            }
            ignoreAppAuthMethod = mpJwtConfig.ignoreApplicationAuthMethod(); // true by default
            if (ignoreAppAuthMethod) {
                result = mpJwtTaiRequest.hasServices();
            } else {
                result = isMpJwtSpecifiedInLoginConfig(request);
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        if (result) {
            request.setAttribute(KEY_AUTHORIZATION_HEADER_SCHEME, mpJwtConfig.getAuthorizationHeaderScheme());
        }

        return result;
    }

    private boolean isNewMpJwtAndMpConfig(HttpServletRequest request) {
        boolean newMpjwtAndMpConfig = false;
        Map<String, String> mpConfigProps = getMpConfigPropsFromRequestObject(request);
        if (mpConfigProps != null && !mpConfigProps.isEmpty()) {
            newMpjwtAndMpConfig = true;
        }
        return newMpjwtAndMpConfig;
    }

    public Map<String, String> getMpConfigPropsFromRequestObject(HttpServletRequest request) {
        if (request == null) {
            return new HashMap<String, String>();
        }
        MicroProfileJwtTaiRequest mpJwtTaiRequest = (MicroProfileJwtTaiRequest) request.getAttribute(ATTRIBUTE_TAI_REQUEST);
        return mpJwtTaiRequest.getMpConfigProps();
    }

    // if we don't have a valid bearer header, and jwtsso is active, we should defer.
    private boolean shouldDeferToJwtSso(HttpServletRequest req, MicroProfileJwtConfig config, MicroProfileJwtConfig jwtssoConfig) {
        if ((!isJwtSsoFeatureActive(config)) && (jwtssoConfig == null)) {
            return false;
        }

        String hdrValue = req.getHeader(Authorization_Header);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Authorization header=", hdrValue);
        }
        boolean haveValidBearerHeader = (hdrValue != null && hdrValue.startsWith("Bearer "));
        return !haveValidBearerHeader;

    }

    public boolean isJwtSsoFeatureActive(MicroProfileJwtConfig config) {
        return config.toString().contains("com.ibm.ws.security.jwtsso.internal.JwtSsoComponent");
    }

    /**
     * @param request
     */
    private boolean isMpJwtSpecifiedInLoginConfig(HttpServletRequest request) {

        if (request.getAttribute(APPLICATION_AUTH_METHOD) != null) {
            String loginCfg = (String) request.getAttribute(APPLICATION_AUTH_METHOD);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Auth method = ", loginCfg);
                Tr.debug(tc, "isMpJwtSpecifiedInLoginConfig ", AUTHN_TYPE.equals(loginCfg));
            }
            if (!AUTHN_TYPE.equals(loginCfg)) {
                String msg = Tr.formatMessage(tc, "MPJWT_NOT_FOUND_IN_APPLICATION", new Object[] { AUTHN_TYPE, loginCfg, "ignoreApplicationAuthMethod", "false" });
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "isMpJwtSpecifiedInLoginConfig ", msg);
                }
            }
            return (AUTHN_TYPE.equals(loginCfg));
        }
        String msg = Tr.formatMessage(tc, "MPJWT_NOT_FOUND_IN_APPLICATION", new Object[] { AUTHN_TYPE, "null", "ignoreApplicationAuthMethod", "false" });
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isMpJwtSpecifiedInLoginConfig ", msg);
        }
        return false;

    }

    public String getBearerToken(HttpServletRequest req, MicroProfileJwtConfig clientConfig) {
        String methodName = "getBearerToken";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, req, clientConfig);
        }
        String token = getBearerTokenFromHeader(req, clientConfig);
        if (token == null) {
            token = getBearerTokenFromParameter(req);
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, token);
        }
        return token;
    }

    String getBearerTokenFromHeader(HttpServletRequest req, MicroProfileJwtConfig clientConfig) {
        String methodName = "getBearerTokenFromHeader";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, req);
        }
        String token = null;
        String tokenHeaderName = getTokenHeaderName(req, clientConfig);
        if ("Cookie".equals(tokenHeaderName)) {
            token = getTokenFromCookie(req, clientConfig);
        } else {
            token = getTokenFromHeader(tokenHeaderName, req);
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, token);
        }
        return token;
    }

    String getTokenHeaderName(HttpServletRequest request, MicroProfileJwtConfig clientConfig) {
        String serverConfigTokenHeader = clientConfig.getTokenHeader();
        if (serverConfigTokenHeader != null) {
            return serverConfigTokenHeader;
        }
        String defaultValue = Authorization_Header;
        String tokenHeaderName = getValueFromMpConfigProps(request, MpConstants.TOKEN_HEADER, defaultValue);
        if (!isSupportedTokenHeaderName(tokenHeaderName)) {
            Tr.warning(tc, "MP_CONFIG_VALUE_NOT_SUPPORTED", new Object[] { tokenHeaderName, MpConstants.TOKEN_HEADER, getSupportedTokenHeaderNames(), defaultValue });
            return defaultValue;
        }
        return tokenHeaderName;
    }

    String getValueFromMpConfigProps(HttpServletRequest request, String propName, String defaultValue) {
        Map<String, String> mpConfigProps = getMpConfigPropsFromRequestObject(request);
        if (mpConfigProps == null) {
            return defaultValue;
        }
        String mpConfigPropValue = mpConfigProps.get(propName);
        if (mpConfigPropValue == null || mpConfigPropValue.isEmpty()) {
            return defaultValue;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Obtained " + propName + " from MP Config properties: [" + mpConfigPropValue + "]");
        }
        return mpConfigPropValue;
    }

    boolean isSupportedTokenHeaderName(String tokenHeader) {
        List<String> supportedNames = getSupportedTokenHeaderNames();
        return supportedNames.contains(tokenHeader);
    }

    List<String> getSupportedTokenHeaderNames() {
        List<String> supportedNames = new ArrayList<String>();
        supportedNames.add(Authorization_Header);
        supportedNames.add("Cookie");
        return supportedNames;
    }

    @Sensitive
    String getTokenFromCookie(HttpServletRequest req, MicroProfileJwtConfig clientConfig) {
        String tokenCookieName = getTokenCookieName(req, clientConfig);
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie != null && cookie.getName().equals(tokenCookieName)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found a " + tokenCookieName + " cookie as expected");
                    }
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    String getTokenCookieName(HttpServletRequest request, MicroProfileJwtConfig clientConfig) {
        String serverConfigCookieName = clientConfig.getCookieName();
        if (serverConfigCookieName != null) {
            return serverConfigCookieName;
        }
        return getValueFromMpConfigProps(request, MpConstants.TOKEN_COOKIE, "Bearer");
    }

    @Sensitive
    String getTokenFromHeader(String tokenHeaderName, HttpServletRequest req) {
        String hdrValue = req.getHeader(tokenHeaderName);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, tokenHeaderName + " header null? ", (hdrValue == null));
        }
        String expectedHeaderScheme = (String) req.getAttribute(KEY_AUTHORIZATION_HEADER_SCHEME);
        return authUtils.getBearerTokenFromHeader(hdrValue, expectedHeaderScheme);
    }

    String getBearerTokenFromParameter(HttpServletRequest req) {
        String methodName = "getBearerTokenFromParameter";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, req);
        }
        String param = null;
        String reqMethod = req.getMethod();
        if (REQ_METHOD_POST.equalsIgnoreCase(reqMethod)) {
            String contentType = req.getHeader(REQ_CONTENT_TYPE_NAME);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Request content type: " + contentType);
            }
            if (REQ_CONTENT_TYPE_APP_FORM_URLENCODED.equals(contentType)) {
                param = req.getParameter(ACCESS_TOKEN);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, param);
        }
        return param;
    }

    MicroProfileJwtTaiRequest setTaiRequestConfigInfo(HttpServletRequest request, MicroProfileJwtTaiRequest mpJwtTaiRequest, boolean defaultConfig) {
        String methodName = "setTaiRequestConfigInfo";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, mpJwtTaiRequest);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Specific config ID not provided, so will set generic config information for MpJwtTaiRequest object");
        }
        MicroProfileJwtTaiRequest result = setGenericAndFilteredConfigTaiRequestInfo(request, mpJwtTaiRequest, defaultConfig);

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    MicroProfileJwtTaiRequest setGenericAndFilteredConfigTaiRequestInfo(HttpServletRequest request, MicroProfileJwtTaiRequest mpJwtTaiRequest, boolean defaultConfig) {
        String methodName = "setGenericAndFilteredConfigTaiRequestInfo";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, mpJwtTaiRequest);
        }
        if (mpJwtTaiRequest == null) {
            mpJwtTaiRequest = createMicroProfileJwtTaiRequestAndSetRequestAttribute(request);
        }
        Iterator<MicroProfileJwtConfig> services = getConfigServices();
        MicroProfileJwtTaiRequest result = setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(request, mpJwtTaiRequest, services, defaultConfig);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    MicroProfileJwtTaiRequest setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(HttpServletRequest request, MicroProfileJwtTaiRequest mpJwtTaiRequest, Iterator<MicroProfileJwtConfig> services, boolean defaultConfig) {
        String methodName = "setGenericAndFilteredConfigTaiRequestInfoFromConfigServices";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, mpJwtTaiRequest, services);
        }
        if (services == null) {
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, mpJwtTaiRequest);
            }
            return mpJwtTaiRequest;
        }
        if (mpJwtTaiRequest == null) {
            mpJwtTaiRequest = createMicroProfileJwtTaiRequestAndSetRequestAttribute(request);
        }

        while (services.hasNext()) {
            MicroProfileJwtConfig mpJwtConfig = services.next();
            AuthenticationFilter authFilter = MicroProfileJwtTAI.getAuthFilter(mpJwtConfig.getAuthFilterRef());
            if (authFilter != null) {
                if (authFilter.isAccepted(request)) {
                    mpJwtTaiRequest.addFilteredConfig(mpJwtConfig);
                }
            } else if (defaultConfig) {
                mpJwtTaiRequest.addGenericConfig(mpJwtConfig);
            } else if (!isMpJwtDefaultConfig(mpJwtConfig)) {
                mpJwtTaiRequest.addGenericConfig(mpJwtConfig);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, mpJwtTaiRequest);
        }
        return mpJwtTaiRequest;
    }

    public boolean isMpJwtDefaultConfig(MicroProfileJwtConfig mpJwtConfig) {
        boolean isDefault = false;
        if ("defaultMpJwt".equals(mpJwtConfig.getUniqueId())) {
            isDefault = true;
        }
        return isDefault;

    }

    MicroProfileJwtTaiRequest handleNoMatchingConfiguration(String configId, MicroProfileJwtTaiRequest mpJwtTaiRequest) {
        String msg = Tr.formatMessage(tc, "MPJWT_NO_SUCH_PROVIDER", new Object[] { configId });
        Tr.error(tc, msg);
        MpJwtProcessingException mpjwtException = new MpJwtProcessingException(msg);
        mpJwtTaiRequest.setTaiException(mpjwtException);
        return mpJwtTaiRequest;
    }

    MicroProfileJwtConfig getConfigAssociatedWithRequestAndId(HttpServletRequest request, String configId) {
        String methodName = "getConfigAssociatedWithRequestAndId";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, configId);
        }
        MicroProfileJwtConfig mpJwtConfig = getConfig(configId);
        if (!configAuthFilterMatchesRequest(request, mpJwtConfig)) {
            // The config with the specified ID isn't configured to service this request
            mpJwtConfig = null;
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, mpJwtConfig);
        }
        return mpJwtConfig;
    }

    Iterator<MicroProfileJwtConfig> getConfigServices() {
        return MicroProfileJwtTAI.getServices();
    }

    MicroProfileJwtConfig getConfig(String configId) {
        return MicroProfileJwtTAI.getMicroProfileJwtConfig(configId);
    }

    boolean configAuthFilterMatchesRequest(HttpServletRequest request, MicroProfileJwtConfig config) {
        if (config == null) {
            return false;
        }
        AuthenticationFilter authFilter = MicroProfileJwtTAI.getAuthFilter(config.getAuthFilterRef());
        if (authFilter != null) {
            if (!authFilter.isAccepted(request)) {
                // Specified configuration is present but its auth filter is not configured to service this request
                return false;
            }
        }
        return true;
    }

}
