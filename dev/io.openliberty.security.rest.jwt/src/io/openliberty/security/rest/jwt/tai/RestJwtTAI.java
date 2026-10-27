/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.rest.jwt.tai;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.jwt.JwtConsumer;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.jwt.config.MpConfigProperties;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.internal.TAIAuthenticator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

@Component(service = { TrustAssociationInterceptor.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           name = "restJwtTAI",
           property = { "service.vendor=IBM",
                        "type=restJwtTAI",
                        "id=RestJwtTAI",
                        "TAIName=RestJwtTAI",
                        "invokeBeforeSSO:Boolean=true",
                        "disableLtpaCookie:Boolean=true" })
public class RestJwtTAI implements TrustAssociationInterceptor {

    private static final TraceComponent tc = Tr.register(RestJwtTAI.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_SERVICE_PID = "service.pid";
    public static final String KEY_SECURITY_SERVICE = "securityService";
    public static final String KEY_FILTER = "authFilter";
    public static final String DEFAULT_AUTH_FILTER_ID = "defaultRestJwtAuthFilter";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String LTPA_COOKIE_NAME = "LtpaToken2";

    public static final String KEY_JWT_CONSUMER_CONFIG = "jwtConsumerConfig";
    public static final String DEFAULT_JWT_CONSUMER_ID = "defaultJwtConsumer";

    //public static final String ADMIN_REST_ENDPOINT_PREFIX = "/IBMJMXConnectorREST/adminEndpoint"; //UTLE: TODO - USE: ADMIN_REST_ENDPOINT
    public static final String ADMIN_REST_ENDPOINT_PREFIX = "/IBMJMXConnectorREST"; 

    static final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    static protected final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = new ConcurrentServiceReferenceMap<String, AuthenticationFilter>(KEY_FILTER);
    static protected final ConcurrentServiceReferenceMap<String, JwtConsumerConfig> jwtConsumerConfigRef = new ConcurrentServiceReferenceMap<String, JwtConsumerConfig>(KEY_JWT_CONSUMER_CONFIG);

    @Reference(service = JwtConsumerConfig.class, name = KEY_JWT_CONSUMER_CONFIG, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void setJwtConsumerConfig(ServiceReference<JwtConsumerConfig> ref) {
        String id = (String) ref.getProperty("id");
        synchronized (jwtConsumerConfigRef) {
            jwtConsumerConfigRef.putReference(id, ref);
        }
    }

    protected void updatedJwtConsumerConfig(ServiceReference<JwtConsumerConfig> ref) {
        String id = (String) ref.getProperty("id");
        synchronized (jwtConsumerConfigRef) {
            jwtConsumerConfigRef.putReference(id, ref);
        }
    }

    protected void unsetJwtConsumerConfig(ServiceReference<JwtConsumerConfig> ref) {
        String id = (String) ref.getProperty("id");
        synchronized (jwtConsumerConfigRef) {
            jwtConsumerConfigRef.removeReference(id, ref);
        }
    }

    @Reference(service = SecurityService.class, name = KEY_SECURITY_SERVICE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    public void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    @Reference(service = AuthenticationFilter.class, name = KEY_FILTER, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.RELUCTANT)
    protected void setAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.putReference(pid, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setAuthFilter pid: " + pid);
        }
    }

    protected void updatedAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.putReference(pid, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "updatedAuthFilter pid: " + pid);
        }
    }

    protected void unsetAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.removeReference(pid, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetAuthFilter pid: " + pid);
        }
    }

    public static AuthenticationFilter getAuthFilter(String pid) {
        return authFilterServiceRef.getService(pid);
    }

    public static Iterator<AuthenticationFilter> getAuthFilters() {
        return authFilterServiceRef.getServices();
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.activate(cc);
        }
        synchronized (jwtConsumerConfigRef) {
            jwtConsumerConfigRef.activate(cc);
        }
        securityServiceRef.activate(cc);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        // Do nothing
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.deactivate(cc);
        }
        synchronized (jwtConsumerConfigRef) {
            jwtConsumerConfigRef.deactivate(cc);
        }
        securityServiceRef.deactivate(cc);
    }

    @Override
    public boolean isTargetInterceptor(HttpServletRequest request) throws WebTrustAssociationException {
        String methodName = "isTargetInterceptor";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request);
        }

        // 1. Verify this request targets the REST admin endpoint
        if (!isTargetRestAdminRequest(request)) {
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, false);
            }
            return false;
        }

        // 2. Check if authentication filter accepts this request
        if (!isAcceptedByAuthFilter(request)) {
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, false);
            }
            return false;
        }

        // 3. Verify that the request carries a Bearer JWT token in the Authorization header.
        //    A request that carries an LTPA cookie instead is also claimed, so that it is rejected
        //    rather than authenticated by LTPA SSO: the REST admin endpoint only accepts JWT.
        String token = getBearerToken(request);
        boolean isTarget = (token != null && !token.isEmpty()) || hasLtpaCookie(request);

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, isTarget);
        }
        return isTarget;
    }

    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest request, HttpServletResponse response) throws WebTrustAssociationFailedException {
        String methodName = "negotiateValidateandEstablishTrust";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, response);
        }

        String token = getBearerToken(request);
        if ((token == null || token.isEmpty()) && hasLtpaCookie(request)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Rejecting request with an " + LTPA_COOKIE_NAME + " cookie; the REST admin endpoint only accepts a JWT");
            }
            TAIResult result = TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }
        if (token == null || token.isEmpty()) {
            TAIResult result = getResultForMissingToken(request);
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }

        TAIResult result = validateJwtAndCreateResult(token);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    @FFDCIgnore({ Exception.class })
    protected TAIResult validateJwtAndCreateResult(@Sensitive String token) throws WebTrustAssociationFailedException {
        String methodName = "validateJwtAndCreateResult";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, token);
        }

        JwtToken jwtToken = null;
        try {
            jwtToken = parseJwt(token);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Error validating JWT token: " + e.getMessage(), e);
            }
            TAIResult result = TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }

        if (jwtToken == null) {
            TAIResult result = TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }

        RestJwtMappingHelper mappingHelper = new RestJwtMappingHelper(jwtToken, getRegistryRealm());
        String username = mappingHelper.getUsername();
        if (username == null || username.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not extract username from JWT token claims");
            }
            TAIResult result = TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }

        Subject subject = mappingHelper.createSubject();
        TAIResult result = TAIResult.create(HttpServletResponse.SC_OK, username, subject);

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    /**
     * Validates the token with the jwtConsumer. If the defaultJwtConsumer has a jwkEndpointUrl configured,
     * for example jwkEndpointUrl="file:///path/to/jwk", the public key is read from that location;
     * otherwise the key comes from the jwtConsumer keystore configuration.
     */
    protected JwtToken parseJwt(@Sensitive String token) throws Exception {
        JwtConsumerConfig config = jwtConsumerConfigRef.getService(DEFAULT_JWT_CONSUMER_ID);
        MpConfigProperties mpProps = getKeyLocationProperties(config);
        if (mpProps == null) {
            return JwtConsumer.create().createJwt(token);
        }
        return config.getConsumerUtils().parseJwt(token, config, mpProps);
    }

    /**
     * @return properties that point the jwtConsumer at the key location given by its jwkEndpointUrl,
     *         or null if the jwtConsumer or its jwkEndpointUrl is not configured
     */
    protected MpConfigProperties getKeyLocationProperties(JwtConsumerConfig config) {
        if (config == null) {
            return null;
        }
        String location = config.getJwkEndpointUrl();
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        MpConfigProperties mpProps = new MpConfigProperties();
        mpProps.put(MpConfigProperties.KEY_LOCATION, location.trim());
        return mpProps;
    }

    /**
     * @return the realm of the configured user registry, or null if it cannot be determined
     */
    @FFDCIgnore({ Exception.class })
    protected String getRegistryRealm() {
        try {
            SecurityService securityService = securityServiceRef.getService();
            if (securityService != null) {
                UserRegistryService urs = securityService.getUserRegistryService();
                if (urs != null && urs.isUserRegistryConfigured()) {
                    return urs.getUserRegistry().getRealm();
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to determine user registry realm: " + e.getMessage());
            }
        }
        return null;
    }

    protected boolean isTargetRestAdminRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String requestURI = request.getRequestURI();
        if (requestURI != null && requestURI.contains(ADMIN_REST_ENDPOINT_PREFIX)) {
            return true;
        }

        StringBuffer requestURL = request.getRequestURL();
        if (requestURL != null && requestURL.toString().contains(ADMIN_REST_ENDPOINT_PREFIX)) {
            return true;
        }

        String servletPath = request.getServletPath();
        if (servletPath != null && servletPath.contains(ADMIN_REST_ENDPOINT_PREFIX)) {
            return true;
        }

        String contextPath = request.getContextPath();
        if (contextPath != null && contextPath.contains(ADMIN_REST_ENDPOINT_PREFIX)) {
            return true;
        }

        return false;
    }

    protected boolean isAcceptedByAuthFilter(HttpServletRequest request) {
        // First check the default auth filter
        AuthenticationFilter defaultFilter = getAuthFilter(DEFAULT_AUTH_FILTER_ID);
        if (defaultFilter != null) {
            return defaultFilter.isAccepted(request);
        }

        // If other auth filters are configured, check if any accepts
        Iterator<AuthenticationFilter> filters = getAuthFilters();
        if (filters != null && filters.hasNext()) {
            while (filters.hasNext()) {
                AuthenticationFilter filter = filters.next();
                if (filter.isAccepted(request)) {
                    return true;
                }
            }
            return false;
        }

        // If no auth filter service is available yet, default to allowing admin requests
        return true;
    }

    protected boolean hasLtpaCookie(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (LTPA_COOKIE_NAME.equals(cookie.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getBearerToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        return null;
    }

    protected TAIResult getResultForMissingToken(HttpServletRequest request) throws WebTrustAssociationFailedException {
        if (isUnprotectedRequest(request)) {
            return TAIResult.create(HttpServletResponse.SC_CONTINUE);
        }
        return TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
    }

    protected boolean isUnprotectedRequest(HttpServletRequest request) {
        if (request != null) {
            WebRequest webRequest = (WebRequest) request.getAttribute(TAIAuthenticator.WEB_REQUEST_ATTRIBUTE_NAME);
            if (webRequest != null) {
                return webRequest.isUnprotectedURI();
            }
        }
        return false;
    }

    @Override
    public int initialize(Properties props) throws WebTrustAssociationFailedException {
        return 0;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getType() {
        return this.getClass().getName();
    }

    @Override
    public void cleanup() {
        // Nothing to clean up
    }
}
