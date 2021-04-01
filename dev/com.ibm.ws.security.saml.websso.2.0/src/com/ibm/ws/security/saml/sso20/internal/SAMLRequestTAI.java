/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.Constants.EndpointType;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoHandler;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.ErrorHandler;
import com.ibm.ws.security.saml.error.ErrorHandlerImpl;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.impl.KnownSamlUrl;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.rs.SamlInboundService;
import com.ibm.ws.security.saml.sso20.slo.SLOHandler;
import com.ibm.ws.security.sso.common.saml.propagation.SamlCommonUtil;
import com.ibm.ws.webcontainer.security.UnprotectedResourceService;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.saml2.UserCredentialResolver;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class SAMLRequestTAI implements TrustAssociationInterceptor, UnprotectedResourceService {
    public static final TraceComponent tc = Tr.register(SAMLRequestTAI.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);
    static final String KEY_LOCATION_ADMIN = "locationAdmin";
    static final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);
    static final String KEY_AUTH_CACHE_SERVICE = "authCacheService";
    static final AtomicServiceReference<AuthCacheService> authCacheServiceRef = new AtomicServiceReference<AuthCacheService>(KEY_AUTH_CACHE_SERVICE);
    static final String KEY_SECURITY_SERVICE = "securityService";
    static final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    static public final String TYPE = "SAMLSso20";
    static public final String VERSION = "v1.0";
    static protected final String KEY_SERVICE_PID = "service.pid";
    static protected final String KEY_PROVIDER_ID = "id";
    static protected final String KEY_ID = "id";

    static public final String KEY_FILTER = "authFilter";
    static protected final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = new ConcurrentServiceReferenceMap<String, AuthenticationFilter>(KEY_FILTER);

    static HashMap<String, String> filterIdMap = new HashMap<String, String>();

    static public final String KEY_SSO_SAML_SERVICE = "ssoSamlService";
    protected final ConcurrentServiceReferenceMap<String, SsoSamlService> reqSsoSamlServiceRef = new ConcurrentServiceReferenceMap<String, SsoSamlService>(KEY_SSO_SAML_SERVICE);

    static public final String KEY_USER_RESOLVER = "userResolver";
    protected final ConcurrentServiceReferenceMap<String, UserCredentialResolver> userResolverRef = new ConcurrentServiceReferenceMap<String, UserCredentialResolver>(KEY_USER_RESOLVER);

    static SubjectHelper subjectHelper = new SubjectHelper();

    static SamlInboundService activatedSamlInboundService = null;

    static void setActivatedInboundService(SamlInboundService inboundService) {
        activatedSamlInboundService = inboundService;
    }

    public void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    public void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    @Trivial
    protected void setAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String servicePid = (String) ref.getProperty(KEY_SERVICE_PID);
        String id = (String) ref.getProperty(KEY_ID);
        String oldId = null;
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.putReference(servicePid, ref);
            oldId = filterIdMap.put(servicePid, id);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setFilter:" + servicePid + " id:" + id + ":" + oldId);
        }
    }

    protected void updatedAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String servicePid = (String) ref.getProperty(KEY_SERVICE_PID);
        String id = (String) ref.getProperty(KEY_ID);
        String oldId = null;;
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.putReference(servicePid, ref);
            oldId = filterIdMap.put(servicePid, id);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " updateFilter:" + servicePid + " id:" + id + ":" + oldId);
        }
    }

    @Trivial
    protected void unsetAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String servicePid = (String) ref.getProperty(KEY_SERVICE_PID);
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.removeReference(servicePid, ref);
            filterIdMap.remove(servicePid);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetFilter:" + servicePid + " id:" + id);
        }
    }

    //Method for unit testing.
    AuthenticationFilter getAuthFilter(String servicePid) {
        return authFilterServiceRef.getService(servicePid);
    }

    @Trivial
    protected void setSsoSamlService(ServiceReference<SsoSamlService> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (reqSsoSamlServiceRef) {
            reqSsoSamlServiceRef.putReference(id, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setSsoSamlService id:" + id);
        }
    }

    protected void updatedSsoSamlService(ServiceReference<SsoSamlService> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (reqSsoSamlServiceRef) {
            reqSsoSamlServiceRef.putReference(id, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " updateSsoSamlService id:" + id);
        }
    }

    @Trivial
    protected void unsetSsoSamlService(ServiceReference<SsoSamlService> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (reqSsoSamlServiceRef) {
            reqSsoSamlServiceRef.removeReference(id, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetSsoSamlService id:" + id);
        }
    }

    //Method for unit testing.
    SsoSamlService getSsoSamlService(String key) {
        return reqSsoSamlServiceRef.getService(key);
    }

    @Trivial
    protected void setUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.putReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setUserResolver id:" + serviceId);
        }
    }

    protected void updatedUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.putReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " updateUserResolver id:" + serviceId);
        }
    }

    @Trivial
    protected void unsetUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.removeReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetUserResolverRef id:" + serviceId);
        }
    }

    @Trivial
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    @Trivial
    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    @Trivial
    protected void setAuthCacheService(ServiceReference<AuthCacheService> reference) {
        authCacheServiceRef.setReference(reference);
    }

    @Trivial
    protected void unsetAuthCacheService(ServiceReference<AuthCacheService> reference) {
        authCacheServiceRef.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.activate(cc);
        }
        synchronized (reqSsoSamlServiceRef) {
            reqSsoSamlServiceRef.activate(cc);
        }
        userResolverRef.activate(cc);
        locationAdminRef.activate(cc);
        // TODO The cache service maybe disabled in
        //   /com.ibm.ws.security.authentication.builtin/src/com/ibm/ws/security/authentication/internal/AuthenticationServiceImpl.java
        authCacheServiceRef.activate(cc);
        securityServiceRef.activate(cc);
        SAMLResponseTAI.setTheActivatedSsoSamlServiceRef(reqSsoSamlServiceRef);
        // This is supposed to have the same content as RsSamlServiceImpl
        AssertionToSubject.setActivatedUserResolverRef(userResolverRef);
        SAMLResponseTAI.setActivatedRequestTai(this);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {}

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.deactivate(cc);
        }
        synchronized (reqSsoSamlServiceRef) {
            reqSsoSamlServiceRef.deactivate(cc);
        }
        userResolverRef.deactivate(cc);
        locationAdminRef.deactivate(cc);
        authCacheServiceRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
    }

    /*
     *
     */
    @Override
    @FFDCIgnore({ SamlException.class })
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req, HttpServletResponse resp) throws WebTrustAssociationFailedException {
        TAIResult taiResult = handleErrorIfAnyAlready(req, resp);
        if (taiResult != null)
            return taiResult;
        SsoSamlService service = null;
        // This does not have an acs cookie, otherwise, it is handled by the cookieTaiImpl already.

        // sp-initiating now
        String spInitiatorId = (String) req.getAttribute(Constants.HTTP_ATTRIBUTE_SP_INITIATOR);
        // provider has to have something otherwise, it won't reach here. but...
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "spInitiatorId:" + spInitiatorId);
        }

        if (spInitiatorId != null && !spInitiatorId.isEmpty()) {
            service = reqSsoSamlServiceRef.getService(spInitiatorId);
            if (service != null) {
                // This may be sp_initiated
                // Or idp_initiated if the loginPageURL in SP configuration is defined
                // Let the Initiator decides it
                Initiator initiator = new Initiator(service);
                try {
                    return initiator.forwardRequestToSamlIdp(req, resp);
                } catch (SamlException e) {
                    ErrorHandler errorHandler = ErrorHandlerImpl.getInstance();
                    try {
                        errorHandler.handleException(req, resp, e);
                    } catch (Exception e1) {
                        // This should not happen.
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unexpceted exception during errorHandling" + e);
                        }
                        // using the SamlException to handle error message of the unexpected Exception.
                        throw new WebTrustAssociationFailedException((new SamlException(e1)).getMessage());
                    }
                    // Since we have handle the error. No need to throw the Exception to ask the webcontainer to handle the error.
                    return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
                }
            }
        }
        // Obviously  service == null
        // It should not reach here unless dynamically change the server configuration
        // Tr.error(tc, "SAML20_NO_SP_FOUND_INTERNAL_ERROR", spInitiatorId);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "There is no SP service available!!" + spInitiatorId);
        }
        String errMsg = SamlException.formatMessage("SAML20_AUTHENTICATION_FAIL",
                                                    "CWWKS5063E: SAML Exception: The SAML service provider (SP) failed to process the authentication request.",
                                                    new Object[] { spInitiatorId });
        throw new WebTrustAssociationFailedException(errMsg);
    }

    /*
     */
    @Override
    public boolean isTargetInterceptor(HttpServletRequest req) throws WebTrustAssociationException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isTargetInterceptor()");
        }
        if (RequestUtil.isUnprotectedUrlForSaml(req))
            return false;

        // if sp-initiated first time, find a proper Acs Handler
        //    also set the request attribute
        // otherwise return false
        if (findSsoSpSpecificFirst(req, reqSsoSamlServiceRef, Constants.EndpointType.REQUEST, false)) {
            return true;
        } else {
            Object exception = req.getAttribute(Constants.SAML_SAMLEXCEPTION_FOUND);
            if (exception != null)
                return true; // need to do error handling later
        }
        return false;
    }

    /**
     * @param req
     * @return
     * @throws SamlException
     */
    boolean findSsoSpSpecificFirst(HttpServletRequest req,
                                   ConcurrentServiceReferenceMap<String, SsoSamlService> reqSsoSamlServiceRef,
                                   Constants.EndpointType endpointType,
                                   boolean bSamlInboundAsWell) {
        synchronized (reqSsoSamlServiceRef) {
            SsoSamlServiceConfig ssoSamlServiceConfig = new SsoSamlServiceConfig(req, reqSsoSamlServiceRef, bSamlInboundAsWell);
            // error if it has multiple spcific SP
            if (ssoSamlServiceConfig.isMultiple(req)) {
                return false;
            }

            // if find only one specific SP
            SsoSamlService ssoSamlService = ssoSamlServiceConfig.getSpecificConfig(req, endpointType);
            if (ssoSamlService != null) {
                return true;
            }

            // if find a generic SP
            ssoSamlService = ssoSamlServiceConfig.getGenericConfig(req, endpointType);
            if (ssoSamlService != null) {
                return true;
            }

        }
        return false;
    }

    boolean findSpSpecificFirst(HttpServletRequest req,
                                ConcurrentServiceReferenceMap<String, SsoSamlService> ssoSamlServiceRef,
                                Constants.EndpointType endpointType) {
        return findSsoSpSpecificFirst(req, ssoSamlServiceRef, endpointType, true);
    }

    @Override
    public int initialize(Properties props) throws WebTrustAssociationFailedException {
        return 0;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void cleanup() {}

    static public HashMap<String, String> getFilterIdMap() {
        return filterIdMap;
    }

    /**
     * This will only be called when disableLtpaCookie is true
     *
     * @param subject
     * @return
     */
    boolean validateSubject(Subject subject, HttpServletRequest req, HttpServletResponse resp, SsoRequest samlRequest) {
        boolean bValid = (subject != null); // subject can not be null, otherwise invalid
        if (bValid) {
            SsoConfig samlConfig = samlRequest.getSsoConfig();
            long reAuthnCushionMilliseconds = samlConfig.getReAuthnCushion();
            // handle rsAuthnOnAssertionExpire and reAuthnCushion(millisecond)
            if (samlConfig.isReAuthnOnAssertionExpire()) {
                // let's check the SamlAssertion NotOnOrAfter
                Saml20Token saml20Token = SamlCommonUtil.getSaml20TokenFromSubject(subject, true);
                if (saml20Token != null) {
                    DateTime expiredDate = new DateTime(saml20Token.getSamlExpires().getTime() - reAuthnCushionMilliseconds);
                    bValid = expiredDate.isAfterNow(); // Not expired yet
                } else {
                    bValid = false;
                }

            }
            if (bValid) {
                // existing checking even before we implement reAuthnOnAssertionExpire
                // check the SpCookieSessionNotOnOrAfter
                Hashtable<String, ?> hashtable = subjectHelper.getHashtableFromSubject(subject, new String[] { Constants.SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER });
                if (hashtable == null) {
                    bValid = false;
                } else {
                    long lSessionNotOnOrAfter = (Long) hashtable.get(Constants.SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER); // millisecond
                    // subtract the reAuthnCushion to make the expiredDateTime earlier
                    DateTime sessionNotOnOrAfter = new DateTime(lSessionNotOnOrAfter - reAuthnCushionMilliseconds);
                    if (!sessionNotOnOrAfter.isAfterNow()) { // cookie expired
                        bValid = false;
                    }
                }
            }
        }

        if (!bValid && resp != null) { // the SP Cookie is not valid anymore
            removeInvalidSpCookie(req, resp, samlRequest);
            // And the invalid subject will be removed after we return invalid back to the caller
        }
        return bValid;
    }

    void removeInvalidSpCookie(HttpServletRequest req, HttpServletResponse resp, SsoRequest samlRequest) {
        // let's remove the spCookie from httpServletRequest since it's expired
        String spCookieName = samlRequest.getSpCookieName();
        RequestUtil.removeCookie(req, resp, spCookieName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.UnprotectedResourceService#isAuthenticationRequired()
     */
    @Override
    public boolean isAuthenticationRequired(HttpServletRequest request) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isAuthenticationRequired()");
        }
        if (RequestUtil.isUnprotectedUrlForSaml(request))
            return false;

        // Let's get the qualified SP first (174880)
        if (findSsoSpSpecificFirst(request, reqSsoSamlServiceRef, Constants.EndpointType.RESPONSE, false)) {
            IExtendedRequest req = (IExtendedRequest) request;
            SsoRequest samlRequest = (SsoRequest) req.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
            samlRequest.setLocationAdminRef(locationAdminRef); // locationAdminRef is needed for get SpCookieName
            // Remove the samlResuest. It will be generated later.
            req.removeAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);

            // check if Cookie exist
            if (spCookiesExist(req, samlRequest)) {
                return true;
            } else if (RequestUtil.isLogoutRequestFromIdP(req, req.getContextPath())) { // this is slo endpoint request and we did not find valid subject
                return false;
            }
            if (RequestUtil.isUnprocessedAcsCookiePresent(reqSsoSamlServiceRef, req, samlRequest)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param req
     * @return
     */
    boolean spCookiesExist(IExtendedRequest req, SsoRequest samlRequest) {
        if (samlRequest.isDisableLtpaCookie()) {
            // let's check SP Cookie
            // get SP Cookie from request
            String spCookieName = samlRequest.getSpCookieName();
            byte[] cookieValueBytes = req.getCookieValueAsBytes(spCookieName);
            if (cookieValueBytes != null && cookieValueBytes.length > 0) {
                // TODO: Do we want to validate the SP Cookie now?
                // Or allow the SAMLResponseTAI handle it, even it's expired?
                SpCookieRetriver spCookieRetriver = new SpCookieRetriver(authCacheServiceRef.getService(), req, samlRequest);
                Subject subject = spCookieRetriver.getSubjectFromSpCookie();
                boolean bSubjectValid = validateSubject(subject, req, null, samlRequest);
                if (!bSubjectValid) {
                    // remove the subject from cache since it's not valid
                    spCookieRetriver.removeSubject();
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if we take some actions.
     * Otherwise, return false
     * example of userName: "user:sp2_realm_No/user2"
     */
    @Override
    public boolean logout(HttpServletRequest request, HttpServletResponse response, String userName) {
        boolean bSetSubject = false;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "logout() userName:" + userName + ", and request URL = " + request.getRequestURI());
        }

        synchronized (reqSsoSamlServiceRef) {

            SsoRequest samlRequest = getSamlRequest(request, Constants.HTTP_ATTRIBUTE_SP_INITIATOR); // the matching service provider selection is already done in the isTargetInterceptor and negotiate... So just use that.
            if (samlRequest != null) {
                samlRequest.setLocationAdminRef(locationAdminRef); // locationAdminRef is needed for get SpCookieName
                if (isSamlSingleLogoutInProgress(request) || servletLogoutPerformsSLO(samlRequest)) { // check whether the new configuration attribute spLogout is set or the IdP initiated logout is taking place)
                    return handleSpCookie((IExtendedRequest) request, response, samlRequest, userName, bSetSubject);
                }

            } else {
                // if it is ibm_security_logout - we will try finding the matching SP
                if (findSsoSpSpecificFirst(request, reqSsoSamlServiceRef, Constants.EndpointType.LOGOUT, false)) {
                    IExtendedRequest req = (IExtendedRequest) request;
                    samlRequest = (SsoRequest) req.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
                    if (samlRequest != null) {
                        samlRequest.setLocationAdminRef(locationAdminRef); // locationAdminRef is needed for get SpCookieName
                        if (servletLogoutPerformsSLO(samlRequest)) {
                            return handleSpCookie((IExtendedRequest) request, response, samlRequest, userName, bSetSubject);
                        }
                    }
                }
            }
            // Search all services and
            // 1) setSubject if subject matches
            // 2) remove the spCookie and its cached subject
            Iterator<SsoSamlService> services = reqSsoSamlServiceRef.getServices();
            SsoSamlService ssoSamlService = null;
            boolean logError = false;
            StringBuffer spList = new StringBuffer(0);
            services = reqSsoSamlServiceRef.getServices();
            while (services.hasNext()) {
                ssoSamlService = services.next();
                samlRequest = new SsoRequest(ssoSamlService.getProviderId(), Constants.EndpointType.LOGOUT, request, Constants.SamlSsoVersion.SAMLSSO20, ssoSamlService);
                if (servletLogoutPerformsSLO(samlRequest)) {
                    // we need to log an error message in this case
                    spList.append(", ");
                    logError = true;
                    spList.append(samlRequest.getProviderName());
                }
                samlRequest.setLocationAdminRef(locationAdminRef); // locationAdminRef is needed for get SpCookieName
                if (handleSpCookie((IExtendedRequest) request, response, samlRequest, userName, bSetSubject)) {
                    bSetSubject = true;
                }
            }
            if (logError) {
                spList.delete(0, 1); // remove first comma
                Tr.error(tc, "LOGOUT_CANNOT_PERFORM_SLO", new Object[] { spList.toString() });
            }

        }
        return bSetSubject;
    }

    /**
     * @param request
     * @return
     */
    private SsoRequest getSamlRequest(HttpServletRequest request, String attrib) {

        String provider = getSamlSP(request, attrib);
        if (provider != null) {
            SsoSamlService service = getSsoSamlService(provider);
            if (service != null) {
                return new SsoRequest(provider, Constants.EndpointType.LOGOUT, request, Constants.SamlSsoVersion.SAMLSSO20, service);
            }
        }
        return null;
    }

    /**
     * @param request
     */
    private String getSamlSP(HttpServletRequest request, String attribute) {
        if (request != null && request.getAttribute(attribute) != null) {
            return (String) request.getAttribute(attribute);
        }
        return null;
    }

    private boolean isSamlSingleLogoutInProgress(HttpServletRequest request) {
        return requestHasAttribute(request, Constants.SLOINPROGRESS);
    }

    private boolean requestHasAttribute(HttpServletRequest request, String attrib) {
        if (request.getAttribute(attrib) != null) {
            return (Boolean) request.getAttribute(attrib);
        }
        return false;
    }

    private void performSamlSLO(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest, Subject subject) throws SamlException {
        if (isSamlSingleLogoutInProgress(request)) {
            removeSLOAttribute(request);
            return;
        }
        SsoHandler slohandler = new SLOHandler();
        Map<String, Object> parameters = createParamMap(samlRequest, subject);
        slohandler.handleRequest(request, response, samlRequest, parameters);
    }

    /**
     * @param request
     */
    private void removeSLOAttribute(HttpServletRequest request) {
        request.removeAttribute(Constants.SLOINPROGRESS);
    }

    private Map<String, Object> createParamMap(SsoRequest samlRequest, Subject subject) {
        HashMap<String, Object> results = new HashMap<String, Object>();
        results.put(Constants.KEY_SAML_SERVICE, samlRequest.getSsoSamlService());
        results.put(Constants.KEY_SECURITY_SERVICE, securityServiceRef.getService());
        results.put(Constants.KEY_SECURITY_SUBJECT, subject);
        return results;
    }

    private boolean servletLogoutPerformsSLO(SsoRequest samlRequest) {
        return samlRequest.getSsoConfig().isServletRequestLogoutPerformsSamlLogout();
    }

    /**
     * When we get a valid SP Cookie, authenticate it
     * Then remove the SP Cookie
     *
     * @param req
     * @param samlRequest
     * @return
     */

    boolean handleSpCookie(IExtendedRequest req,
                           HttpServletResponse response,
                           SsoRequest samlRequest,
                           String userName,
                           boolean bSetSubjectAlready) {
        boolean bSetSubject = false;
        // find a valid spCookie and get its subject, otherwise call SAMLRequestTAI directly
        SpCookieRetriver spCookieRetriever = new SpCookieRetriver(authCacheServiceRef.getService(), req, samlRequest);
        Subject subject = spCookieRetriever.getSubjectFromSpCookie();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "subject from spCookie is:" + subject);
        }
        if (subject == null) {
            String spCookieName = samlRequest.getSpCookieName();
            RequestUtil.removeCookie(req, response, spCookieName);
            return false;
        }
        if (samlRequest.isDisableLtpaCookie()) {
            if (!bSetSubjectAlready) {
                bSetSubject = authenticateBeforeRemovingSubject(subject, req, response, userName, spCookieRetriever.getCustomCacheKey(), samlRequest);
            }
            removeSubjectAndCookie(req, response, spCookieRetriever, samlRequest);
        }
        return bSetSubject;
    }

    @FFDCIgnore(value = { CredentialExpiredException.class, CredentialDestroyedException.class })
    boolean authenticateBeforeRemovingSubject(Subject subject, IExtendedRequest req, HttpServletResponse response, String userName, String customCacheKey, SsoRequest samlRequest) {

        WSCredential wsCredential = SamlUtil.getWSCredential(subject);
        if (wsCredential != null) {
            try {
                String accessId = wsCredential.getAccessId();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "wsCredential user:" + accessId);
                }
                if (accessId != null) {
                    AuthenticationData authenticationData = new WSAuthenticationData();
                    authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, req);
                    authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, response);
                    authenticationData.set(AuthenticationData.TOKEN64, customCacheKey);

                    return authenticateSubject(subject, req, response, authenticationData, samlRequest);
                }
            } catch (CredentialExpiredException e) {
                // TODO handle the Exception?
            } catch (CredentialDestroyedException e) {
                // TODO handle the Exception?
            }
        }
        return false;
    }

    /**
     * @param subject
     * @param spCookieRetriever
     * @param samlRequest
     */
    void removeSubjectAndCookie(IExtendedRequest req, HttpServletResponse response, SpCookieRetriver spCookieRetriever, SsoRequest samlRequest) {

        spCookieRetriever.removeSubject();
        // remove the SP cookie from http request/response
        String spCookieName = samlRequest.getSpCookieName();
        RequestUtil.removeCookie(req, response, spCookieName);

    }

    /**
     * @param subject
     * @return
     */
    boolean authenticateSubject(Subject subject, HttpServletRequest req, HttpServletResponse resp, AuthenticationData authenticationData, SsoRequest samlRequest) {
        Hashtable<String, ?> hashtable = subjectHelper.getHashtableFromSubject(subject, new String[] { Constants.SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER });
        if (hashtable == null)
            return false;

        long lSessionNotOnOrAfter = (Long) hashtable.get(Constants.SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER);
        DateTime sessionNotOnOrAfter = new DateTime(lSessionNotOnOrAfter);
        if (!sessionNotOnOrAfter.isAfterNow()) { // cookie expired
            removeInvalidSpCookie(req, resp, samlRequest);
            // TODO remove the subject from the cache
            return false;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "subject from spCookie is:" + subject);
            }
            SecurityService securityService = securityServiceRef.getService();
            AuthenticationService authenticationService = securityService.getAuthenticationService();
            return authenticateWithSubject(req, resp, subject, authenticationService, authenticationData);
        }
    }

    /**
     * @param req
     * @param res
     * @param subject
     * @return
     * @throws AuthenticationException
     */
    @FFDCIgnore(AuthenticationException.class)
    private boolean authenticateWithSubject(HttpServletRequest req, HttpServletResponse res, Subject subject, AuthenticationService authenticationService,
                                            AuthenticationData authenticationData) {
        try {
            Subject newSubject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, subject);
            SubjectManager subjectManager = new SubjectManager();
            subjectManager.setCallerSubject(newSubject);
        } catch (AuthenticationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "authenticationException:" + e);
            }
            return false;
        }
        return true;
    }

    class SsoSamlServiceConfig {
        private static final String SAML_CONTEXT_PATH = "/ibm/saml20";
        public SsoSamlService[] filteredSsoSamlServices = null;
        public int iFilteredSsoCnt = 0;
        public SsoSamlService[] genericSsoSamlServices = null;
        public int iGenericSsoCnt = 0;
        ConcurrentServiceReferenceMap<String, SsoSamlService> activatedSsoSamlServiceRef = null;
        boolean bSamlInboundAsWell;

        public SsoSamlServiceConfig(HttpServletRequest req, ConcurrentServiceReferenceMap<String, SsoSamlService> ssoSamlServiceRef, boolean bSamlInboundAsWell) {
            this.activatedSsoSamlServiceRef = ssoSamlServiceRef;
            this.bSamlInboundAsWell = bSamlInboundAsWell;
            filteredSsoSamlServices = new SsoSamlService[activatedSsoSamlServiceRef.size()];
            genericSsoSamlServices = new SsoSamlService[activatedSsoSamlServiceRef.size()];
            Iterator<SsoSamlService> ssoServices = activatedSsoSamlServiceRef.getServices();
            // Check the ones with AuthnFilter first
            // Otherwise save them in the genericSsoSamlServices
            SsoSamlService ssoSamlService = null;
            while (ssoServices.hasNext()) {
                ssoSamlService = ssoServices.next();
                if (!bSamlInboundAsWell) { // skip saml inbound services
                    if (ssoSamlService.isInboundPropagation()) {
                        continue;
                    }
                }
                if (ssoSamlService.isEnabled()) {
                    String ctxPath = req.getContextPath();
                    if (RequestUtil.isLogoutRequestFromIdP(req, ctxPath) &&
                        ssoSamlService.getProviderId().equals(getProviderNameFromUrl(req))) {
                        // making the slo endpoint protected, so we will have an authenticated subject on the thread and have access to the token
                        iFilteredSsoCnt = 1;
                        filteredSsoSamlServices[0] = ssoSamlService;
                        break;
                    }

                    AuthenticationFilter authFilter = ssoSamlService.getConfig().getAuthFilter(authFilterServiceRef);
                    if (authFilter != null) {
                        if (authFilter.isAccepted(req)) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "pass sso authFilter(" + authFilter + "):" + ssoSamlService.getProviderId());
                            }
                            filteredSsoSamlServices[iFilteredSsoCnt++] = ssoSamlService;
                        }
                    } else {
                        genericSsoSamlServices[iGenericSsoCnt++] = ssoSamlService;
                    }
                }
            }
        }

        public Matcher endpointRequestMatcher(HttpServletRequest request) {
            String path = request.getPathInfo();
            if (path != null) {
                Matcher m = KnownSamlUrl.matchKnownSamlUrl(path);
                if (m.matches()) {
                    return m;
                }
            }
            return null;
        }

        public String getProviderNameFromUrl(HttpServletRequest request) {
            Matcher m = endpointRequestMatcher(request);
            if (m != null) {
                return m.group(1);
            }
            return null;
        }

        /**
         * @param req
         * @param endpointType
         * @return
         */
        public SsoSamlService getGenericConfig(HttpServletRequest req, EndpointType endpointType) {
            SsoSamlService ssoSamlService = null;
            // We come to here when iFilteredSsoCnt is 0
            // Check the genericssoSamlService. The first one wins unless it's the default SP.
            // The default SP is the last one who wins.
            if (iGenericSsoCnt > 0) {
                String spProviderId = null;
                // The spProviderId will be the non-default one, if any.
                // Or the default one if it is the only one available
                for (int iI = 0; iI < iGenericSsoCnt; iI++) {
                    ssoSamlService = genericSsoSamlServices[iI];
                    spProviderId = ssoSamlService.getProviderId();
                    if (!Constants.DEFAULT_SP_ID.equals(spProviderId)) {
                        break;
                    }
                }
                req.setAttribute(Constants.HTTP_ATTRIBUTE_SP_INITIATOR, spProviderId);
                RequestUtil.setSamlRequest(req, ssoSamlService, endpointType);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "the first generic SP passed:" + spProviderId);
                }
            }
            return ssoSamlService;
        }

        /**
         * @param req
         * @return
         */
        public SsoSamlService getSpecificConfig(HttpServletRequest req, Constants.EndpointType endpointType) {
            SsoSamlService ssoSamlService = null;
            //  The request pass only one authnFilter in the SP is good
            if (iFilteredSsoCnt == 1) {
                ssoSamlService = filteredSsoSamlServices[0];
                RequestUtil.setSamlRequest(req, ssoSamlService, endpointType);
                String spProviderId = ssoSamlService.getProviderId();
                req.setAttribute(Constants.HTTP_ATTRIBUTE_SP_INITIATOR, spProviderId);
            }
            return ssoSamlService;
        }

        /**
         *
         */
        public boolean isMultiple(HttpServletRequest req) {
            // The request pass more than one authnFilter in the SP is bad
            // We do not know how to choose the SP. Throw Exception in this case
            if (iFilteredSsoCnt > 1) {
                String spNames = "";
                for (int iI = 0; iI < iFilteredSsoCnt; iI++) {
                    String middle = iI > 0 ? (iFilteredSsoCnt - iI == 1 ? " and " : ", ") : "";
                    spNames = spNames.concat(middle).concat(filteredSsoSamlServices[iI].getProviderId());
                }
                String requestUrl = req.getRequestURL().toString();
                SamlException samlException = new SamlException("SAML20_MULTI_SPECIFIC_SP", null,
                                //SAML20_MULTI_SPECIFIC_SP=CWWKS5077E: Cannot find a specific Service Provider to process the request [{0}]. The qualified Service Providers are [{1}].
                                new Object[] { requestUrl, spNames });
                req.setAttribute(Constants.SAML_SAMLEXCEPTION_FOUND, samlException);
                return true;
            }
            return false;
        }
    }

    /**
     * @param req
     * @return
     * @throws WebTrustAssociationFailedException
     */
    TAIResult handleErrorIfAnyAlready(HttpServletRequest req, HttpServletResponse resp) throws WebTrustAssociationFailedException {
        SamlException exception = (SamlException) req.getAttribute(Constants.SAML_SAMLEXCEPTION_FOUND);
        if (exception == null)
            return null;
        // need to do error handling
        ErrorHandler errorHandler = ErrorHandlerImpl.getInstance();
        try {
            errorHandler.handleException(req, resp, exception);
        } catch (Exception e1) {
            // unexpecting exception which too complicated to handle further
            // especially, we are throwing a WebTrustAssociationFailedException to the caller
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpceted exception during errorHandling" + exception);
            }
        }
        // Since we have handle the error. No need to throw the Exception to ask the webcontainer to handle the error.
        return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.UnprotectedResourceService#postLogout(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */

    @Override
    public boolean postLogout(HttpServletRequest request, HttpServletResponse response) {
        boolean result = true;
        synchronized (reqSsoSamlServiceRef) {

            SsoRequest samlRequest = getSamlRequest(request, Constants.HTTP_ATTRIBUTE_SP_INITIATOR); // the matching service provider selection is already done in the isTargetInterceptor and negotiate... So just use that.
            if (samlRequest != null && servletLogoutPerformsSLO(samlRequest)) { // check whether the new configuration attribute spLogout is set
                samlRequest.setLocationAdminRef(locationAdminRef); // locationAdminRef is needed for get SpCookieName
                try {
                    performSamlSLO(request, response, samlRequest, WSSubject.getCallerSubject());
                } catch (WSSecurityException e) {
                    result = false; //TODO throw servlet exception
                } catch (SamlException e) {
                    result = false;
                }
            }
        }
        return result;
    }

}
