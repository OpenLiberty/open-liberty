/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.authentication.internal;

import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.PasswordExpiredException;
import com.ibm.ws.security.authentication.UserRevokedException;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.internal.cache.keyproviders.BasicAuthCacheKeyProvider;
import com.ibm.ws.security.authentication.internal.cache.keyproviders.CustomCacheKeyProvider;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.jaas.modules.CertificateLoginModule;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.delegation.DefaultDelegationProvider;
import com.ibm.ws.security.delegation.DelegationProvider;
import com.ibm.ws.security.jaas.common.callback.CallbackHandlerAuthenticationData;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;

import io.openliberty.checkpoint.spi.CheckpointPhase;

@TraceOptions(messageBundle = "com.ibm.ws.security.authentication.internal.resources.AuthenticationMessages")
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final TraceComponent tc = Tr.register(AuthenticationServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static final String CFG_ALLOW_HASHTABLE_LOGIN_WITH_ID_ONLY = "allowHashtableLoginWithIdOnly";
    static final String CFG_CACHE_ENABLED = "cacheEnabled";
    static final String CFG_USE_DISPLAYNAME_FOR_SECURITYNAME = "useDisplayNameForSecurityName";
    static final String CFG_IGNORE_CUSTOM_CACHE_KEY = "ignoreCustomCacheKey";
    static final String KEY_AUTH_CACHE_SERVICE = "authCacheService";
    static final String KEY_USER_REGISTRY_SERVICE = "userRegistryService";
    static final String KEY_DELEGATION_PROVIDER = "delegationProvider";
    static final String KEY_DEFAULT_DELEGATION_PROVIDER = "defaultDelegationProvider";
    static final String KEY_CREDENTIALS_SERVICE = "credentialsService";
    static final String KEY_LTPA_CONFIGURATION = "ltpaConfiguration";
    private static final String LTPA_OID = "oid:1.3.18.0.2.30.2";
    private static final String JWT_OID = "oid:1.3.18.0.2.30.3"; // ?????
    private static final long MILLIS_PER_MINUTE = 60 * 1000;

    private final AtomicServiceReference<AuthCacheService> authCacheServiceRef = new AtomicServiceReference<AuthCacheService>(KEY_AUTH_CACHE_SERVICE);
    private final AtomicServiceReference<UserRegistryService> userRegistryServiceRef = new AtomicServiceReference<UserRegistryService>(KEY_USER_REGISTRY_SERVICE);
    private final AtomicServiceReference<DelegationProvider> delegationProviderRef = new AtomicServiceReference<DelegationProvider>(KEY_DELEGATION_PROVIDER);
    private final AtomicServiceReference<DefaultDelegationProvider> defaultDelegationProviderRef = new AtomicServiceReference<DefaultDelegationProvider>(KEY_DEFAULT_DELEGATION_PROVIDER);
    private final AtomicServiceReference<CredentialsService> credentialsServiceRef = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIALS_SERVICE);
    private final AtomicServiceReference<LTPAConfiguration> ltpaConfigurationRef = new AtomicServiceReference<LTPAConfiguration>(KEY_LTPA_CONFIGURATION);
    private JAASService jaasService;
    private ComponentContext cc;
    private boolean cacheEnabled = true;
    private boolean allowHashtableLoginWithIdOnly = false;
    private boolean useDisplayNameForSecurityName = false;
    private boolean ignoreCustomCacheKey = false;
    private String invalidDelegationUser = "";

    private final AuthenticationGuard authenticationGuard = new AuthenticationGuard();

    /**
     * Helper method to check if debug tracing is enabled.
     *
     * @return true if debug tracing is enabled, false otherwise
     */
    private boolean isDebugEnabled() {
        return TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
    }

    protected void setJaasService(JAASService jaasService) {
        this.jaasService = jaasService;
        if (jaasService instanceof JAASServiceImpl) {
            ((JAASServiceImpl) jaasService).setAuthenticationService(this);
        }
    }

    protected void unsetJaasService(JAASService jaasService) {
        if (this.jaasService == jaasService) {
            this.jaasService = null;
            ((JAASServiceImpl) jaasService).unsetAuthenticationService(this);
        }
    }

    protected void setAuthCacheService(ServiceReference<AuthCacheService> reference) {
        authCacheServiceRef.setReference(reference);
    }

    protected void unsetAuthCacheService(ServiceReference<AuthCacheService> reference) {
        authCacheServiceRef.unsetReference(reference);
    }

    protected void setUserRegistryService(ServiceReference<UserRegistryService> reference) {
        userRegistryServiceRef.setReference(reference);
    }

    protected void unsetUserRegistryService(ServiceReference<UserRegistryService> reference) {
        userRegistryServiceRef.unsetReference(reference);
    }

    protected void setDelegationProvider(ServiceReference<DelegationProvider> reference) {
        delegationProviderRef.setReference(reference);
    }

    protected void unsetDelegationProvider(ServiceReference<DelegationProvider> reference) {
        delegationProviderRef.unsetReference(reference);
    }

    protected void setDefaultDelegationProvider(ServiceReference<DefaultDelegationProvider> reference) {
        defaultDelegationProviderRef.setReference(reference);
    }

    protected void unsetDefaultDelegationProvider(ServiceReference<DefaultDelegationProvider> reference) {
        defaultDelegationProviderRef.unsetReference(reference);
    }

    protected void setCredentialsService(ServiceReference<CredentialsService> reference) {
        credentialsServiceRef.setReference(reference);
    }

    protected void unsetCredentialsService(ServiceReference<CredentialsService> reference) {
        credentialsServiceRef.unsetReference(reference);
    }

    protected void setLtpaConfiguration(ServiceReference<LTPAConfiguration> reference) {
        ltpaConfigurationRef.setReference(reference);
    }

    protected void unsetLtpaConfiguration(ServiceReference<LTPAConfiguration> reference) {
        ltpaConfigurationRef.unsetReference(reference);
    }

    /**
     * Based on the configuration properties, the auth cache should either
     * be active or not.
     *
     * @param props
     */
    private void updateCacheState(Map<String, Object> props) {
        getAuthenticationConfig(props);

        if (cacheEnabled) {
            authCacheServiceRef.activate(cc);
        } else {
            authCacheServiceRef.deactivate(cc);
        }
    }

    /**
     * @param props
     */
    private void getAuthenticationConfig(Map<String, Object> props) {

        Boolean loginWithIdOnly = (Boolean) props.get(CFG_ALLOW_HASHTABLE_LOGIN_WITH_ID_ONLY);
        if (loginWithIdOnly != null)
            allowHashtableLoginWithIdOnly = loginWithIdOnly;

        Boolean state = (Boolean) props.get(CFG_CACHE_ENABLED);
        if (state != null) {
            cacheEnabled = state;
        }

        Boolean useDisplayNameForSecurityNameState = (Boolean) props.get(CFG_USE_DISPLAYNAME_FOR_SECURITYNAME);
        if (useDisplayNameForSecurityNameState != null) {
            useDisplayNameForSecurityName = useDisplayNameForSecurityNameState;
        }

        Boolean ignoreCustomCacheKeyState = (Boolean) props.get(CFG_IGNORE_CUSTOM_CACHE_KEY);
        if (ignoreCustomCacheKeyState != null) {
            ignoreCustomCacheKey = ignoreCustomCacheKeyState;
        }
    }

    protected void activate(ComponentContext cc, Map<String, Object> props) {
        this.cc = cc;
        authCacheServiceRef.activate(cc);
        userRegistryServiceRef.activate(cc);
        delegationProviderRef.activate(cc);
        defaultDelegationProviderRef.activate(cc);
        credentialsServiceRef.activate(cc);
        ltpaConfigurationRef.activate(cc);
        updateCacheState(props);
    }

    protected void modified(Map<String, Object> props) {
        updateCacheState(props);
    }

    protected void deactivate() {
        authCacheServiceRef.deactivate(cc);
        userRegistryServiceRef.deactivate(cc);
        delegationProviderRef.deactivate(cc);
        defaultDelegationProviderRef.deactivate(cc);
        credentialsServiceRef.deactivate(cc);
        ltpaConfigurationRef.deactivate(cc);
        if (jaasService instanceof JAASServiceImpl) {
            ((JAASServiceImpl) jaasService).unsetAuthenticationService(this);
        }
        cc = null;
    }

    /** {@inheritDoc} */
    @Override
    public Subject authenticate(String jaasEntryName, Subject inputSubject) throws AuthenticationException {
        AuthenticationData emptyAuthenticationData = new WSAuthenticationData();
        return authenticate(jaasEntryName, emptyAuthenticationData, inputSubject);
    }

    /** {@inheritDoc} */
    @Override
    public Subject authenticate(String jaasEntryName, AuthenticationData authenticationData, Subject subject) throws AuthenticationException {
        AuthenticationData hashtableAuthData = getHashtable(subject);
        ReentrantLock currentLock = obtainCurrentLock(authenticationData, hashtableAuthData);

        try {
            // If basic auth login to a different realm, then create a basic auth subject
            if (isBasicAuthLogin(authenticationData)) {
                return createBasicAuthSubject(authenticationData, subject);
            } else {
                Subject cachedAuthenticatedSubject = findSubjectInAuthCache(authenticationData, subject, hashtableAuthData);
                if (cachedAuthenticatedSubject != null) {
                    return cachedAuthenticatedSubject;
                }

                Subject authenticatedSubject = performJAASLogin(jaasEntryName, authenticationData, subject);
                //Initializing the cache should happen first which is located inside io.openliberty.jcache.internal.CacheServiceImpl.activate(Map<String, Object>)
                //Therefore ranking 3 is given here.
                CheckpointPhase.onRestore(3, () -> insertSubjectInAuthCache(authenticationData, authenticatedSubject));
                return authenticatedSubject;
            }
        } finally {
            releaseLock(authenticationData, hashtableAuthData, currentLock);
            CertificateLoginModule.collectiveCertificate.set(false);
        }
    }

    /**
     * If we have hashtableAuthData from the subject, then use the hashtableAuthData to lock it.
     * Otherwise, will use the regular authenticationData to lock it.
     *
     * @param authenticationData
     * @param hashtableAuthData
     * @return
     */
    private ReentrantLock obtainCurrentLock(AuthenticationData authenticationData, AuthenticationData hashtableAuthData) {
        ReentrantLock currentLock;
        if (!hashtableAuthData.isEmpty())
            currentLock = optionallyObtainLockedLock(hashtableAuthData);
        else
            currentLock = optionallyObtainLockedLock(authenticationData);
        return currentLock;
    }

    private boolean isBasicAuthLogin(AuthenticationData authenticationData) {
        boolean result = false;
        if (authenticationData != null) {
            String currentRealm = getRealm();
            String loginRealm = (String) authenticationData.get(AuthenticationData.REALM);
            // TODO: Determine how to find out default realm from the WSRealmNameCallbackImpl for when a realm is not specified in the handler
            result = loginRealm != null && loginRealm.equals(currentRealm) == false && loginRealm.equals("defaultRealm") == false;
        }
        return result;
    }

    private Subject createBasicAuthSubject(AuthenticationData authenticationData, Subject subject) throws AuthenticationException {
        Subject basicAuthSubject = subject != null ? subject : new Subject();

        String loginRealm = (String) authenticationData.get(AuthenticationData.REALM);
        String username = (String) authenticationData.get(AuthenticationData.USERNAME);
        String password = getPassword((char[]) authenticationData.get(AuthenticationData.PASSWORD));

        CredentialsService credentialsService = credentialsServiceRef.getService();
        try {
            // TODO: call getServiceWithException here and wrap any exception in an AuthenticationException instead of checking for null;
            if (credentialsService != null) {
                credentialsService.setBasicAuthCredential(basicAuthSubject, loginRealm, username, password);
            }
        } catch (CredentialException e) {
            throw new AuthenticationException(e.getMessage());
        }

        //basicAuthSubject.setReadOnly();
        return basicAuthSubject;
    }

    /** {@inheritDoc} */
    @Override
    public Subject authenticate(String jaasEntryName, CallbackHandler callbackHandler, Subject subject) throws AuthenticationException {
        CallbackHandlerAuthenticationData cAuthData = new CallbackHandlerAuthenticationData(callbackHandler);
        AuthenticationData authenticationData = null;
        try {
            authenticationData = cAuthData.createAuthenticationData();
        } catch (Exception e) {
            throw new AuthenticationException(e.getMessage());
        }
        AuthenticationData hashtableAuthData = getHashtable(subject);
        ReentrantLock currentLock = obtainCurrentLock(authenticationData, hashtableAuthData);

        try {
            // If basic auth login to a different realm, then create a basic auth subject
            if (isBasicAuthLogin(authenticationData)) {
                return createBasicAuthSubject(authenticationData, subject);
            } else {
                Subject cachedAuthenticatedSubject = findSubjectInAuthCache(authenticationData, subject, hashtableAuthData);
                if (cachedAuthenticatedSubject != null) {
                    return cachedAuthenticatedSubject;
                }

                Subject authenticatedSubject = performJAASLogin(jaasEntryName, callbackHandler, subject);
                final AuthenticationData fAuthenticationData = authenticationData;
                //Initializing the cache should happen first which is located inside io.openliberty.jcache.internal.CacheServiceImpl.activate(Map<String, Object>)
                //Therefore ranking 3 is given here.
                CheckpointPhase.onRestore(3, () -> insertSubjectInAuthCache(fAuthenticationData, authenticatedSubject));
                return authenticatedSubject;
            }
        } finally {
            releaseLock(authenticationData, hashtableAuthData, currentLock);
        }
    }

    /**
     * This method will try to obtain a lock from the authentication guard based on the
     * given authentication data and lock it. If an equals authentication data on another thread
     * is received for which a lock already exists, this method will block that another thread
     * until the first thread relinquishes the lock. This allows having locking based on
     * authentication data instead of blindly locking all access. The intention is to NOT allow
     * multiple concurrent JAAS logins for the same authentication data in order to be able to
     * correctly represent the user with the same runtime subject for the same data, better
     * manage caching, and to prevent cycles doing logins for which potentially many of the
     * results will be discarded.
     *
     * This method has no locking effect when there is no authentication cache.
     */
    private ReentrantLock optionallyObtainLockedLock(AuthenticationData authenticationData) {
        ReentrantLock currentLock = null;
        if (isAuthCacheServiceAvailable()) {
            currentLock = authenticationGuard.requestAccess(authenticationData);
            currentLock.lock();
        }
        return currentLock;
    }

    private boolean isAuthCacheServiceAvailable() {
        AuthCacheService authCacheService = getAuthCacheService();
        return authCacheService != null;
    }

    /**
     * Do not check for authentication cache, always unlock.
     * The authentication cache may have been removed dynamically
     * after the lock was obtained.
     */
    private void releaseLock(AuthenticationData authenticationData, AuthenticationData hashtableAuthData, ReentrantLock currentLock) {
        if (!hashtableAuthData.isEmpty()) {
            authenticationGuard.relinquishAccess(hashtableAuthData, currentLock);
        } else {
            authenticationGuard.relinquishAccess(authenticationData, currentLock);
        }
    }

    private Subject findSubjectInAuthCache(AuthenticationData authenticationData, Subject partialSubject,
                                           AuthenticationData hashtableAuthData) throws AuthenticationException {
        Subject subject = null;

        AuthCacheService authCacheService = getAuthCacheService();
        if (authCacheService != null && authenticationData != null) {
            String jwtSSOToken = (String) authenticationData.get(AuthenticationData.JWT_TOKEN);
            String ssoToken = (String) authenticationData.get(AuthenticationData.TOKEN64);
            if (jwtSSOToken != null) {
                subject = findSubjectByTokenContents(authCacheService, jwtSSOToken, null, authenticationData);
            } else if (ssoToken != null) {
                String oid = (String) authenticationData.get(AuthenticationData.AUTHENTICATION_MECH_OID);
                if (oid == null || oid.equals(LTPA_OID)) {
                    subject = findSubjectByTokenContents(authCacheService, ssoToken, null, authenticationData);
                }
            } else {
                byte[] ssoTokenBytes = (byte[]) authenticationData.get(AuthenticationData.TOKEN);
                if (ssoTokenBytes != null) {
                    subject = findSubjectByTokenContents(authCacheService, null, ssoTokenBytes, authenticationData);
                } else {
                    X509Certificate[] certChain = (X509Certificate[]) authenticationData.get(AuthenticationData.CERTCHAIN);
                    if (certChain != null) {
                        subject = findSubjectByX509Cert(authCacheService, certChain);
                    } else {
                        String userid = (String) authenticationData.get(AuthenticationData.USERNAME);
                        String password = getPassword((char[]) authenticationData.get(AuthenticationData.PASSWORD));
                        if (userid != null && password != null) {
                            subject = findSubjectByUseridAndPassword(authCacheService, userid, password);
                        } else if (partialSubject != null) {
                            subject = findSubjectBySubjectHashtable(authCacheService, partialSubject, hashtableAuthData);
                        }
                    }
                }
            }

            // Check if the cached subject's LTPA token needs refresh
            if (subject != null && shouldRefreshCachedToken(subject)) {
                if (isDebugEnabled()) {
                    Tr.debug(tc, "Cached subject's LTPA token needs refresh, returning null to force validation LTPA token and then refresh it");
                }
                return null;
            }
        }
        return subject;
    }

    /**
     * Checks if the LTPA token in the cached Subject needs to be refreshed based on
     * refreshThreshold and inactivityTimeout settings.
     *
     * <p>When {@code dynamicExpirationValidation} is enabled the expiration stored in the
     * token (and therefore in {@code WSCredential}) is {@code creationTime + inactivityTimeout},
     * not the absolute configured expiration. This method accounts for that: the absolute
     * deadline is recomputed as {@code creationTime + configuredExpiration} when
     * {@code dynamicExpirationValidation=true}, mirroring the logic in
     * {@link com.ibm.ws.security.token.ltpa.internal.LTPAToken2#validateExpiration}.
     *
     * @param subject The Subject retrieved from auth cache
     * @return true if token needs refresh, false otherwise
     */
    private boolean shouldRefreshCachedToken(Subject subject) {
        // Beta guard: Token refresh is only available in beta edition
        if (!ProductInfo.getBetaEdition()) {
            return false;
        }

        // Early-exit: if inactivity timeout is not configured the feature is inactive;
        // avoid the ltpaConfigurationRef.getService() call on every request.
        LTPAConfiguration ltpaConfig = ltpaConfigurationRef.getService();
        if (ltpaConfig == null || ltpaConfig.getInactivityTimeout() <= 0) {
            return false;
        }

        try {
            // Extract WSCredential from Subject
            Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
            if (wsCredentials == null || wsCredentials.isEmpty()) {
                if (isDebugEnabled()) {
                    Tr.debug(tc, "No WSCredential found in subject");
                }
                return false;
            }

            WSCredential wsCredential = wsCredentials.iterator().next();
            if (wsCredential == null) {
                return false;
            }

            long currentTime = System.currentTimeMillis();
            long inactivityTimeoutInMinutes = ltpaConfig.getInactivityTimeout();
            long refreshThresholdInMinutes = ltpaConfig.getRefreshThreshold();
            boolean dynamicExpirationValidation = ltpaConfig.isDynamicExpirationValidation();

            if (isDebugEnabled()) {
                Tr.debug(tc, "ltpaConfig inactivityTimeout=" + inactivityTimeoutInMinutes +
                             ", refreshThreshold=" + refreshThresholdInMinutes +
                             ", dynamicExpirationValidation=" + dynamicExpirationValidation);
            }

            // Get creation time from WSCredential
            Object creationTimeObj = wsCredential.get(AttributeNameConstants.WSTOKEN_CREATION_TIME);
            if (!(creationTimeObj instanceof Long)) {
                // No creationTime — fall back to the stored expiration for the absolute-expiry check only.
                long storedExpiration = wsCredential.getExpiration();
                if (currentTime >= storedExpiration) {
                    if (isDebugEnabled()) {
                        Tr.debug(tc, "Token is expired (no creationTime): current=" + currentTime +
                                     ", storedExpiration=" + storedExpiration);
                    }
                    return true;
                }
                if (isDebugEnabled()) {
                    Tr.debug(tc, "Creation time not found in WSCredential, skipping inactivity timeout check");
                }
                return false;
            }

            long creationTime = (Long) creationTimeObj;

            // Compute the effective expiration.
            // With dynamicExpirationValidation=true the stored expiration is
            // creationTime + inactivityTimeout, not the absolute deadline.
            // Recompute from creationTime + configured expiration, matching LTPAToken2.validateExpiration.
            final long effectiveExpiration;
            if (dynamicExpirationValidation) {
                effectiveExpiration = creationTime + (ltpaConfig.getTokenExpiration() * MILLIS_PER_MINUTE);
            } else {
                effectiveExpiration = wsCredential.getExpiration();
            }

            // Check if token has exceeded absolute expiration
            if (currentTime >= effectiveExpiration) {
                if (isDebugEnabled()) {
                    Tr.debug(tc, "Token is expired: current=" + currentTime + ", absoluteExpiration=" + effectiveExpiration);
                }
                return true;
            }

            // Compute the inactivity expiration and cap it at the absolute deadline.
            long inactivityExpiration = creationTime + (inactivityTimeoutInMinutes * MILLIS_PER_MINUTE);
//            if (inactivityExpiration > effectiveExpiration) {
//                inactivityExpiration = effectiveExpiration;
//            }

            if (isDebugEnabled()) {
                Tr.debug(tc, "Inactivity timeout check: creationTime=" + creationTime +
                             ", inactivityExpiration=" + inactivityExpiration +
                             ", absoluteExpiration=" + effectiveExpiration +
                             ", currentTime=" + currentTime);
            }

            // Check if token has exceeded inactivity timeout
            if (currentTime >= inactivityExpiration) {
                if (isDebugEnabled()) {
                    Tr.debug(tc, "Token exceeded inactivity timeout");
                }
                return true;
            }

            // Check if within refresh threshold of inactivity expiration
            if (refreshThresholdInMinutes > 0) {
                long refreshThresholdInMillis = refreshThresholdInMinutes * MILLIS_PER_MINUTE;
                long timeRemainingUntilInactivity = inactivityExpiration - currentTime;

                if (timeRemainingUntilInactivity <= refreshThresholdInMillis) {
                    if (isDebugEnabled()) {
                        Tr.debug(tc, "Token needs refresh: time until inactivity expiration (" +
                                     timeRemainingUntilInactivity + "ms) <= threshold (" +
                                     refreshThresholdInMillis + "ms)");
                    }
                    return true;
                }
            }

            return false;

        } catch (SecurityException se) {
            // Security exceptions should be logged at warning level
            Tr.warning(tc, "Security exception while checking token refresh: " + se.getMessage());
            throw se;
        } catch (Exception e) {
            // Log other exceptions at warning level instead of just debug
            Tr.warning(tc, "Error checking if cached token needs refresh: " + e.getMessage());
            if (isDebugEnabled()) {
                Tr.debug(tc, "Error checking if cached token needs refresh", e);
            }
            return false;
        }
    }

    private Subject findSubjectByX509Cert(AuthCacheService authCacheService, X509Certificate[] certChain) {
        int certHash = ((java.security.cert.Certificate) certChain[0]).hashCode();
        return authCacheService.getSubject(certHash);
    }

    /**
     * Finds a Subject based on the provided token contents.
     *
     * @param authCacheService   The authentication cache service used to retrieve subjects.
     * @param token              The token string to search for in the cache.
     * @param ssoTokenBytes      The byte array representation of the Single Sign-On (SSO) token.
     * @param authenticationData The authentication data containing the authentication mechanism OID.
     * @return The Subject associated with the provided token, or null if not found.
     * @throws AuthenticationException If the token is invalid or the custom cache key is missing.
     */
    private Subject findSubjectByTokenContents(AuthCacheService authCacheService, String token, byte[] ssoTokenBytes,
                                               AuthenticationData authenticationData) throws AuthenticationException {
        Subject subject = null;
        String oid = (String) authenticationData.get(AuthenticationData.AUTHENTICATION_MECH_OID);
        if (token != null) {
            if (oid == null || oid.equals(LTPA_OID)) {
                subject = authCacheService.getSubject(token);
            } else if (oid != null && oid.equals(JWT_OID)) {
                String cacheKey = JwtSSOTokenHelper.getCacheKeyForJwtSSOToken(subject, token);
                subject = authCacheService.getSubject(cacheKey);
            }
        }
        if (subject == null && ssoTokenBytes != null) {
            subject = authCacheService.getSubject(Base64Coder.base64EncodeToString(ssoTokenBytes));
        }
        if (subject == null) {
            String customCacheKey = null;
            if (oid == null || oid.equals(LTPA_OID)) {
                if (ssoTokenBytes == null && token != null) {
                    ssoTokenBytes = Base64Coder.base64DecodeString(token);
                }
                if (ssoTokenBytes == null) {
                    throw new AuthenticationException("Invalid LTPA Token");
                }
                customCacheKey = CustomCacheKeyProvider.getCustomCacheKey(authCacheService, ssoTokenBytes, authenticationData);

            } else if (oid != null && oid.equals(JWT_OID)) {
                customCacheKey = JwtSSOTokenHelper.getCustomCacheKeyFromJwtSSOToken(token);
            }

            if (customCacheKey != null) {
                subject = authCacheService.getSubject(customCacheKey);
                if (subject == null) {
                    if (ignoreCustomCacheKey()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "ignoreCustomCacheKey is set to true. Continue authentication without re-challenging");
                        }
                    } else {
                        throw new AuthenticationException("Custom cache key missed authentication cache. Need to re-challenge the user to login again.");
                    }
                }
            }
        }
        return subject;
    }

    private Subject findSubjectByUseridAndPassword(AuthCacheService authCacheService, String userid, @Sensitive String password) {
        return authCacheService.getSubject(BasicAuthCacheKeyProvider.createLookupKey(getRealm(), userid, password));
    }

/*
 * We only create cache key (CustomCacheKeyProvider.java) for hashtable login so there is no need to
 * get the lookup key for userId/pwd and userId only cases.
 */
    private Subject findSubjectBySubjectHashtable(AuthCacheService authCacheService, Subject partialSubject, AuthenticationData hashtableAuthData) {
        Subject subject = null;
        if (hashtableAuthData.isEmpty())
            return subject;

        String customCacheKey = (String) hashtableAuthData.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
        if (customCacheKey != null) {
            subject = authCacheService.getSubject(customCacheKey);
            return subject;
        }
        //We do not create look up key for hashtable userid/pwd or userid only
        String userid = (String) hashtableAuthData.get(AttributeNameConstants.WSCREDENTIAL_USERID);
        String password = (String) hashtableAuthData.get(AttributeNameConstants.WSCREDENTIAL_PASSWORD);

        String lookupKey;
        if (password != null) {
            lookupKey = BasicAuthCacheKeyProvider.createLookupKey(getRealm(), userid, password);
        } else {
            lookupKey = BasicAuthCacheKeyProvider.createLookupKey(getRealm(), userid);
        }
        subject = authCacheService.getSubject(lookupKey);

        return subject;
    }

    private AuthenticationData getHashtable(Subject partialSubject) {
        AuthenticationData authData = new WSAuthenticationData();
        SubjectHelper subjectHelper = new SubjectHelper();
        Hashtable<String, ?> hashtable = subjectHelper.getHashtableFromSubject(partialSubject, new String[] { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY });
        if (hashtable != null) {
            String customCacheKey = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
            if (customCacheKey != null) {
                authData.set(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
            }
        }

        hashtable = subjectHelper.getHashtableFromSubject(partialSubject, new String[] { AttributeNameConstants.WSCREDENTIAL_USERID,
                                                                                         AttributeNameConstants.WSCREDENTIAL_PASSWORD });
        if (hashtable != null) {
            String userid = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_USERID);
            String password = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_PASSWORD);
            if (userid != null & password != null) {
                authData.set(AttributeNameConstants.WSCREDENTIAL_USERID, userid);
                authData.set(AttributeNameConstants.WSCREDENTIAL_PASSWORD, password);
            } else if (userid != null) {
                Boolean internalCachekeyAssertion = (Boolean) hashtable.get(AuthenticationConstants.INTERNAL_ASSERTION_KEY);
                if (internalCachekeyAssertion != null && internalCachekeyAssertion.equals(Boolean.TRUE))
                    authData.set(AttributeNameConstants.WSCREDENTIAL_USERID, userid); //Allow to login with user ID only
            }
        }

        return authData;
    }

    @Sensitive
    private String getPassword(@Sensitive char[] passwordBytes) {
        String password = null;
        if (passwordBytes != null) {
            password = String.valueOf(passwordBytes);
        }
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public AuthCacheService getAuthCacheService() {
        return authCacheServiceRef.getService();
    }

    @FFDCIgnore(RegistryException.class)
    private String getRealm() {
        String realm = "defaultRealm";
        UserRegistry userRegistry;
        try {
            UserRegistryService userRegistryService = userRegistryServiceRef.getService();
            if (userRegistryService.isUserRegistryConfigured()) {
                userRegistry = userRegistryService.getUserRegistry();
                realm = userRegistry.getRealm();
            }
        } catch (RegistryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem getting the realm.", e);
            }
        }
        return realm;
    }

    @FFDCIgnore(LoginException.class)
    private Subject performJAASLogin(String jaasEntryName, CallbackHandler callbackHandler, Subject subject) throws AuthenticationException {
        if (jaasService != null) {
            try {
                return jaasService.performLogin(jaasEntryName, callbackHandler, subject);
            } catch (LoginException e) {
                throw new AuthenticationException(e.getLocalizedMessage());
            }
        }
        Tr.error(tc, "AUTHENTICATION_SERVICE_JAAS_UNAVAILABLE");
        throw new AuthenticationException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                       TraceConstants.MESSAGE_BUNDLE,
                                                                       "AUTHENTICATION_SERVICE_JAAS_UNAVAILABLE",
                                                                       new Object[] {},
                                                                       "CWWKS1000E: The JAAS Service is unavailable."));
    }

    @FFDCIgnore(LoginException.class)
    private Subject performJAASLogin(String jaasEntryName, AuthenticationData authenticationData, Subject subject) throws AuthenticationException {
        if (jaasService != null) {
            try {
                return jaasService.performLogin(jaasEntryName, authenticationData, subject);
            } catch (LoginException e) {
                if (e instanceof PasswordExpiredException) {
                    throw new PasswordExpiredException(e.getLocalizedMessage());
                } else if (e instanceof UserRevokedException) {
                    throw new UserRevokedException(e.getLocalizedMessage());
                }
                throw new AuthenticationException(e.getLocalizedMessage());
            }
        }
        Tr.error(tc, "AUTHENTICATION_SERVICE_JAAS_UNAVAILABLE");
        throw new AuthenticationException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                       TraceConstants.MESSAGE_BUNDLE,
                                                                       "AUTHENTICATION_SERVICE_JAAS_UNAVAILABLE",
                                                                       new Object[] {},
                                                                       "CWWKS1000E: The JAAS Service is unavailable."));
    }

    private void insertSubjectInAuthCache(AuthenticationData authenticationData, Subject authenticatedSubject) {
        AuthCacheService authCacheService = getAuthCacheService();
        if (authCacheService != null) {
            String userid = (String) authenticationData.get(AuthenticationData.USERNAME);
            String password = getPassword((char[]) authenticationData.get(AuthenticationData.PASSWORD));
            if (userid != null && password != null) {
                authCacheService.insert(authenticatedSubject, userid, password);
            } else {
                if (authenticationData.get(authenticationData.CERTCHAIN) != null) {
                    authCacheService.insert(authenticatedSubject, (X509Certificate[]) authenticationData.get(AuthenticationData.CERTCHAIN));
                } else {
                    // If the token was cloned (refreshed), the new subject's SSO token bytes differ
                    // from the incoming request's bytes. Remove the stale old-bytes cache entry so
                    // subsequent requests carrying the old cookie don't keep hitting it and triggering
                    // unnecessary refresh cycles.
                    evictStaleTokenCacheEntry(authCacheService, authenticationData, authenticatedSubject);
                    authCacheService.insert(authenticatedSubject);
                }
            }
        }
    }

    /**
     * If a token clone occurred during this JAAS login, the authenticated subject contains
     * new SSO token bytes while the incoming authenticationData still carries the old bytes.
     * Remove the old-bytes cache entry to prevent stale cache hits on subsequent requests.
     *
     * @param authCacheService     the auth cache service
     * @param authenticationData   the original authentication data from the request
     * @param authenticatedSubject the freshly authenticated subject (may contain cloned token)
     */
    private void evictStaleTokenCacheEntry(AuthCacheService authCacheService,
                                           AuthenticationData authenticationData,
                                           Subject authenticatedSubject) {
        try {
            // Derive the old cache key from the incoming request's token bytes
            String oldCacheKey = null;
            String ssoToken64 = (String) authenticationData.get(AuthenticationData.TOKEN64);
            if (ssoToken64 != null) {
                oldCacheKey = ssoToken64;
            } else {
                byte[] ssoTokenBytes = (byte[]) authenticationData.get(AuthenticationData.TOKEN);
                if (ssoTokenBytes != null) {
                    oldCacheKey = Base64Coder.toString(Base64Coder.base64Encode(ssoTokenBytes));
                }
            }

            if (oldCacheKey == null) {
                return; // Not a token-based login — nothing to evict
            }

            // Derive the new cache key from the authenticated subject's SSO token
            com.ibm.wsspi.security.token.SingleSignonToken newSsoToken = SSOTokenHelper.getSSOToken(authenticatedSubject);
            if (newSsoToken == null) {
                return;
            }
            String newCacheKey = Base64Coder.toString(Base64Coder.base64Encode(newSsoToken.getBytes()));

            // Only remove the old entry if the bytes actually changed (i.e. a clone occurred)
            if (!oldCacheKey.equals(newCacheKey)) {
                if (isDebugEnabled()) {
                    Tr.debug(tc, "Token was cloned during refresh — evicting stale cache entry for old token bytes");
                }
                authCacheService.remove(oldCacheKey);
            }
        } catch (Exception e) {
            // Non-fatal: stale entry will be evicted by the generational cache timer
            if (isDebugEnabled()) {
                Tr.debug(tc, "Could not evict stale token cache entry after clone", e);
            }
        }
    }

    /**
     * Sets the identity of the unauthenticated user specified in the servlet RunAs
     */
    public void setInvalidDelegationUser(String invalidUser) {
        invalidDelegationUser = invalidUser;
    }

    /**
     * Returns the identity of the unauthenticated user specified in the servlet RunAs
     */

    @Override
    public String getInvalidDelegationUser() {
        return invalidDelegationUser;
    }

    /**
     * Gets the delegation subject based on the currently configured delegation provider
     * or the MethodDelegationProvider if one is not configured.
     *
     * @param roleName the name of the role, used to look up the corresponding user.
     * @param appName  the name of the application, used to look up the corresponding user.
     * @return subject a subject representing the user that is mapped to the given run-as role.
     * @throws IllegalArgumentException
     */
    @Override
    public Subject delegate(String roleName, String appName) {
        Subject runAsSubject = getRunAsSubjectFromProvider(roleName, appName);
        return runAsSubject;
    }

    @FFDCIgnore(AuthenticationException.class)
    private Subject getRunAsSubjectFromProvider(String roleName, String appName) {
        Subject runAsSubject = null;
        DefaultDelegationProvider defaultDelegationProvider = null;

        DelegationProvider delegationProvider = delegationProviderRef.getService();
        try {
            if (delegationProvider != null) {
                runAsSubject = delegationProvider.getRunAsSubject(roleName, appName);
            } else {
                defaultDelegationProvider = defaultDelegationProviderRef.getService();
                runAsSubject = defaultDelegationProvider.getRunAsSubject(roleName, appName);
            }

        } catch (AuthenticationException e) {
            if (delegationProvider != null)
                setInvalidDelegationUser(delegationProvider.getDelegationUser());
            else
                setInvalidDelegationUser(defaultDelegationProvider.getDelegationUser());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an authentication exception, so will run as the invocation subject.");
            }
        }
        return runAsSubject;
    }

    /** {@inheritDoc} */
    @Override
    public Boolean isAllowHashTableLoginWithIdOnly() {
        return allowHashtableLoginWithIdOnly;
    }

    /** {@inheritDoc} */
    @Override
    public Boolean isUseDisplayNameForSecurityName() {
        return useDisplayNameForSecurityName;
    }

    /** {@inheritDoc} */
    @Override
    public Boolean ignoreCustomCacheKey() {
        return ignoreCustomCacheKey;
    }

}
