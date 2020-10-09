/*******************************************************************************
 * Copyright (c) 2016, 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.tai;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;
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
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.config.MpConfigUtil;
import com.ibm.ws.security.mp.jwt.error.ErrorHandlerImpl;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.security.mp.jwt.impl.utils.MicroProfileJwtTaiRequest;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

@Component(service = { TrustAssociationInterceptor.class }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, name = "microProfileJwtTAI", property = { "service.vendor=IBM", "type=microProfileJwtTAI", "id=MPJwtTAI", "TAIName=MPJwtTAI", "invokeBeforeSSO:Boolean=true", "disableLtpaCookie:Boolean=true" })
public class MicroProfileJwtTAI implements TrustAssociationInterceptor {

    private static TraceComponent tc = Tr.register(MicroProfileJwtTAI.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_SERVICE_PID = "service.pid";
    public static final String KEY_PROVIDER_ID = "id";
    public static final String KEY_ID = "id";
    private final static String KEY_MPJWT_CONFIG = "microProfileJwtConfig";
    public static final String KEY_LOCATION_ADMIN = "locationAdmin";
    public static final String KEY_AUTH_CACHE_SERVICE = "authCacheService";
    public static final String KEY_SECURITY_SERVICE = "securityService";
    public static final String KEY_FILTER = "authFilter";
    public static final String KEY_MP_JWT_CONFIG = "microProfileJwtConfig";
    public static final String ATTRIBUTE_TAI_REQUEST = "MPJwtTaiRequest";
    public static final String JTI_CLAIM = "jti";
    public static final String KEY_AUTHORIZATION_HEADER_SCHEME = "authorizationHeaderScheme";
    public static final String KEY_MP_JWT_EXTENSION_SERVICE = "mpJwtExtensionService";
    static final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    static protected final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = new ConcurrentServiceReferenceMap<String, AuthenticationFilter>(KEY_FILTER);
    static final ConcurrentServiceReferenceMap<String, MicroProfileJwtConfig> mpJwtConfigRef = new ConcurrentServiceReferenceMap<String, MicroProfileJwtConfig>(KEY_MP_JWT_CONFIG);
    static final AtomicServiceReference<MpConfigProxyService> mpConfigProxyServiceRef = new AtomicServiceReference<MpConfigProxyService>(KEY_MP_JWT_EXTENSION_SERVICE);

    TAIJwtUtils taiJwtUtils = new TAIJwtUtils();

    ReferrerURLCookieHandler referrerURLCookieHandler = null;
    TAIRequestHelper taiRequestHelper = new TAIRequestHelper();
    MpConfigUtil mpConfigUtil = null;

    public MicroProfileJwtTAI() {
        mpConfigUtil = new MpConfigUtil(mpConfigProxyServiceRef);
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
            Tr.debug(tc, " setAuthFilter pid:" + pid);
            Tr.debug(tc, "@AV999 setAuthFilter service ref:" + getAuthFilter(pid));
        }
    }

    protected void updatedAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.putReference(pid, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setAuthFilter pid:" + pid);
        }
    }

    protected void unsetAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.removeReference(pid, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetAuthFilter pid:" + pid);
        }
    }

    static public AuthenticationFilter getAuthFilter(String pid) {
        return authFilterServiceRef.getService(pid);
    }

    @Reference(service = MicroProfileJwtConfig.class, name = KEY_MPJWT_CONFIG, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.RELUCTANT)
    protected void setMicroProfileJwtConfig(ServiceReference<MicroProfileJwtConfig> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (mpJwtConfigRef) {
            mpJwtConfigRef.putReference(id, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setMicroProfileJwtConfig id:" + id + " Number of references is now: " + mpJwtConfigRef.size() + "service = " + mpJwtConfigRef.getService(id));
        }
    }

    protected void updatedMicroProfileJwtConfig(ServiceReference<MicroProfileJwtConfig> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (mpJwtConfigRef) {
            mpJwtConfigRef.putReference(id, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " updateMicroProfileJwtConfig id:" + id);
        }
    }

    protected void unsetMicroProfileJwtConfig(ServiceReference<MicroProfileJwtConfig> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (mpJwtConfigRef) {
            mpJwtConfigRef.removeReference(id, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetMicroProfileJwtConfig id:" + id);
        }
    }

    public static MicroProfileJwtConfig getMicroProfileJwtConfig(String key) {
        // TODO: Use read/write locks to serialize access when the mpJwtConfigRef is being updated.
        return mpJwtConfigRef.getService(key);
    }

    public static Iterator<MicroProfileJwtConfig> getServices() {
        return mpJwtConfigRef.getServices();
    }

    @Reference(service = MpConfigProxyService.class, name = KEY_MP_JWT_EXTENSION_SERVICE, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setMpConfigProxyService(ServiceReference<MpConfigProxyService> reference) {
        mpConfigProxyServiceRef.setReference(reference);
    }

    protected void unsetMpConfigProxyService(ServiceReference<MpConfigProxyService> reference) {
        mpConfigProxyServiceRef.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.activate(cc);
        }

        synchronized (mpJwtConfigRef) {
            mpJwtConfigRef.activate(cc);
        }
        securityServiceRef.activate(cc);
        mpConfigProxyServiceRef.activate(cc);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        // Do nothing for now.

    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.deactivate(cc);
        }
        synchronized (mpJwtConfigRef) {
            // 240443 work around small kernel bug.
            // need to remove all references, because if we changed id param, osgi will not remove old one.
            // it will however add everything back in later, so we can remove everything now.
            Iterator<String> keysIt = mpJwtConfigRef.keySet().iterator();
            while (keysIt.hasNext()) {
                String key = keysIt.next();
                ServiceReference<MicroProfileJwtConfig> configref = mpJwtConfigRef.getReference(key);
                mpJwtConfigRef.removeReference(key, configref);
            }
            mpJwtConfigRef.deactivate(cc);
        }
        securityServiceRef.deactivate(cc);
        mpConfigProxyServiceRef.deactivate(cc);
    }

    @Override
    public boolean isTargetInterceptor(HttpServletRequest request) throws WebTrustAssociationException {
        String methodName = "isTargetInterceptor";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request);
        }
        MicroProfileJwtTaiRequest mpJwtTaiRequest = taiRequestHelper.createMicroProfileJwtTaiRequestAndSetRequestAttribute(request);
        updateTaiRequestWithMpConfigProps(request, mpJwtTaiRequest);
        boolean result = taiRequestHelper.requestShouldBeHandledByTAI(request, mpJwtTaiRequest);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    void updateTaiRequestWithMpConfigProps(HttpServletRequest request, MicroProfileJwtTaiRequest mpJwtTaiRequest) {
        mpJwtTaiRequest.setMpConfigProps(mpConfigUtil.getMpConfig(request));
        request.setAttribute(ATTRIBUTE_TAI_REQUEST, mpJwtTaiRequest);
    }

    /**
     * @param request
     * @return
     */
    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest request, HttpServletResponse response) throws WebTrustAssociationFailedException {
        String methodName = "negotiateValidateandEstablishTrust";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, response);
        }
        TAIResult taiResult = TAIResult.create(HttpServletResponse.SC_FORBIDDEN);

        MicroProfileJwtTaiRequest mpJwtTaiRequest = (MicroProfileJwtTaiRequest) request.getAttribute(ATTRIBUTE_TAI_REQUEST);
        TAIResult result = getAssociatedConfigAndHandleRequest(request, response, mpJwtTaiRequest, taiResult);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    @FFDCIgnore({ MpJwtProcessingException.class })
    TAIResult getAssociatedConfigAndHandleRequest(HttpServletRequest request, HttpServletResponse response, MicroProfileJwtTaiRequest mpJwtTaiRequest, TAIResult defaultTaiResult) throws WebTrustAssociationFailedException {
        String methodName = "getAssociatedConfigAndHandleRequest";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, response, mpJwtTaiRequest, defaultTaiResult);
        }
        MicroProfileJwtConfig clientConfig = null;
        try {
            clientConfig = mpJwtTaiRequest.getOnlyMatchingConfig();
        } catch (MpJwtProcessingException e) {
            // did not find unique mpJwt config to serve this request
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A unique mpJwt config wasn't found for this request. Exception was " + e);
            }
            TAIResult result = sendToErrorPage(response, defaultTaiResult);
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }
        TAIResult result = handleRequestBasedOnJwtConfig(request, response, clientConfig, defaultTaiResult);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    TAIResult handleRequestBasedOnJwtConfig(HttpServletRequest request, HttpServletResponse response, MicroProfileJwtConfig config, TAIResult defaultTaiResult) throws WebTrustAssociationFailedException {
        String methodName = "handleRequestBasedOnJwtConfig";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, response, config, defaultTaiResult);
        }
        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Client config for request could not be found. An error must have occurred initializing this request.");
            }
            TAIResult result = sendToErrorPage(response, defaultTaiResult);
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }
        TAIResult result = getAndValidateMicroProfileJwt(request, response, config);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public int initialize(Properties props) throws WebTrustAssociationFailedException {
        // Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        // Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        // Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() {
        // Auto-generated method stub

    }

    TAIResult getAndValidateMicroProfileJwt(HttpServletRequest request, HttpServletResponse response, MicroProfileJwtConfig mpJwtConfig) throws WebTrustAssociationFailedException {
        String methodName = "getAndValidateMicroProfileJwt";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, request, response, mpJwtConfig);
        }

        String token = taiRequestHelper.getBearerToken(request, mpJwtConfig);
        if (token == null) {
            Tr.error(tc, "JWT_NOT_FOUND_IN_REQUEST");
            TAIResult result = sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }
        if (TAIJwtUtils.isJwtPreviouslyLoggedOut(token)) {
            Tr.error(tc, "JWT_PREVIOUSLY_LOGGED_OUT");
            TAIResult result = sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, result);
            }
            return result;
        }
        TAIResult result = handleMicroProfileJwtValidation(request, response, mpJwtConfig, token, false);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    @FFDCIgnore({ Exception.class })
    public TAIResult handleMicroProfileJwtValidation(HttpServletRequest req, HttpServletResponse res, MicroProfileJwtConfig clientConfig, String token, boolean addJwtPrincipal) throws WebTrustAssociationFailedException {
        String methodName = "handleMicroProfileJwtValidation";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, req, res, clientConfig, token);
        }

        JwtToken jwtToken = null;
        String decodedPayload = null;

        if (token != null) {
            // Create JWT from access token / id token
            try {
                Map<String, String> mpCfg = taiRequestHelper.getMpConfigPropsFromRequestObject(req);
                if (!mpCfg.isEmpty()) {
                    jwtToken = clientConfig.getConsumerUtils().parseJwt(token, clientConfig, mpCfg);
                } else {
                    jwtToken = taiJwtUtils.createJwt(token, clientConfig.getUniqueId());
                }
            } catch (Exception e) {
                if (!JwtUtils.isJwtSsoValidationExpiredTokenCodePath()) {
                    Tr.error(tc, "ERROR_CREATING_JWT_USING_TOKEN_IN_REQ", new Object[] { e.getLocalizedMessage() }); //CWWKS5523E
                } else {
                    Tr.debug(tc, "ERROR_CREATING_JWT_USING_TOKEN_IN_REQ", new Object[] { e.getLocalizedMessage() }); //CWWKS5523E
                }
                return sendToErrorPage(res, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
            }
            String payload = JsonUtils.getPayload(token);
            decodedPayload = JsonUtils.decodeFromBase64String(payload);
        }

        TAIResult authnResult = null;
        try {
            authnResult = createResult(res, clientConfig, jwtToken, decodedPayload, addJwtPrincipal);
        } catch (Exception e) {
            if (e instanceof MpJwtProcessingException) {
                FFDCFilter.processException(e, MicroProfileJwtTAI.class.getName(), "387");
            }
            Tr.error(tc, "ERROR_CREATING_RESULT", new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return sendToErrorPage(res, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, authnResult);
        }
        return authnResult;
    }

    TAIResult createResult(HttpServletResponse res, MicroProfileJwtConfig clientConfig, @Sensitive JwtToken jwtToken, @Sensitive String decodedPayload, boolean addJwtPrincipal) throws WebTrustAssociationFailedException, MpJwtProcessingException {
        String methodName = "createResult";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, res, clientConfig, jwtToken, decodedPayload);
        }
        TAIMappingHelper mappingHelper = new TAIMappingHelper(decodedPayload, clientConfig);
        mappingHelper.createJwtPrincipalAndPopulateCustomProperties(jwtToken, addJwtPrincipal);
        mappingHelper.addDisableSsoLtpaCacheProp();
        Subject subject = mappingHelper.createSubjectFromCustomProperties(addJwtPrincipal);
        TAIResult result = TAIResult.create(HttpServletResponse.SC_OK, mappingHelper.getUsername(), subject);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    TAIResult sendToErrorPage(HttpServletResponse response, TAIResult taiResult) {
        if (response != null) {
            return ErrorHandlerImpl.getInstance().handleErrorResponse(response, taiResult);
        }
        return taiResult;
    }

}
