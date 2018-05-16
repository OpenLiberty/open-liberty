/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal;

import java.util.Hashtable;
import java.util.Map;
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
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
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
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.delegation.DelegationProvider;
import com.ibm.ws.security.jaas.common.callback.CallbackHandlerAuthenticationData;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@TraceOptions(messageBundle = "com.ibm.ws.security.authentication.internal.resources.AuthenticationMessages")
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final TraceComponent tc = Tr.register(AuthenticationServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static final String CFG_ALLOW_HASHTABLE_LOGIN_WITH_ID_ONLY = "allowHashtableLoginWithIdOnly";
    static final String CFG_CACHE_ENABLED = "cacheEnabled";
    static final String KEY_AUTH_CACHE_SERVICE = "authCacheService";
    static final String KEY_USER_REGISTRY_SERVICE = "userRegistryService";
    static final String KEY_DELEGATION_PROVIDER = "delegationProvider";
    static final String KEY_DEFAULT_DELEGATION_PROVIDER = "defaultDelegationProvider";
    static final String KEY_CREDENTIALS_SERVICE = "credentialsService";
    private static final String LTPA_OID = "oid:1.3.18.0.2.30.2";
    private static final String JWT_OID = "oid:1.3.18.0.2.30.3"; // ?????

    private final AtomicServiceReference<AuthCacheService> authCacheServiceRef = new AtomicServiceReference<AuthCacheService>(KEY_AUTH_CACHE_SERVICE);
    private final AtomicServiceReference<UserRegistryService> userRegistryServiceRef = new AtomicServiceReference<UserRegistryService>(KEY_USER_REGISTRY_SERVICE);
    private final AtomicServiceReference<DelegationProvider> delegationProviderRef = new AtomicServiceReference<DelegationProvider>(KEY_DELEGATION_PROVIDER);
    private final AtomicServiceReference<DelegationProvider> defaultDelegationProviderRef = new AtomicServiceReference<DelegationProvider>(KEY_DEFAULT_DELEGATION_PROVIDER);
    private final AtomicServiceReference<CredentialsService> credentialsServiceRef = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIALS_SERVICE);
    private JAASService jaasService;
    private ComponentContext cc;
    private boolean cacheEnabled = true;
    private boolean allowHashtableLoginWithIdOnly = false;
    private String invalidDelegationUser = "";

    private final AuthenticationGuard authenticationGuard = new AuthenticationGuard();

    protected void setJaasService(JAASService jaasService) {
        this.jaasService = jaasService;
        if (jaasService instanceof JAASServiceImpl) {
            JAASServiceImpl.setAuthenticationService(this);
        }
    }

    protected void unsetJaasService(JAASService jaasService) {
        if (this.jaasService == jaasService) {
            this.jaasService = null;
            JAASServiceImpl.unsetAuthenticationService(this);
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

    protected void setDefaultDelegationProvider(ServiceReference<DelegationProvider> reference) {
        defaultDelegationProviderRef.setReference(reference);
    }

    protected void unsetDefaultDelegationProvider(ServiceReference<DelegationProvider> reference) {
        defaultDelegationProviderRef.unsetReference(reference);
    }

    protected void setCredentialsService(ServiceReference<CredentialsService> reference) {
        credentialsServiceRef.setReference(reference);
    }

    protected void unsetCredentialsService(ServiceReference<CredentialsService> reference) {
        credentialsServiceRef.unsetReference(reference);
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
    }

    protected void activate(ComponentContext cc, Map<String, Object> props) {
        this.cc = cc;
        authCacheServiceRef.activate(cc);
        userRegistryServiceRef.activate(cc);
        delegationProviderRef.activate(cc);
        defaultDelegationProviderRef.activate(cc);
        credentialsServiceRef.activate(cc);
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
        JAASServiceImpl.unsetAuthenticationService(this);
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
        ReentrantLock currentLock = optionallyObtainLockedLock(authenticationData);
        try {
            // If basic auth login to a different realm, then create a basic auth subject
            if (isBasicAuthLogin(authenticationData)) {
                return createBasicAuthSubject(authenticationData, subject);
            } else {
                Subject authenticatedSubject = findSubjectInAuthCache(authenticationData, subject);
                if (authenticatedSubject == null) {
                    authenticatedSubject = performJAASLogin(jaasEntryName, authenticationData, subject);
                    insertSubjectInAuthCache(authenticationData, authenticatedSubject);
                }
                return authenticatedSubject;
            }
        } finally {
            releaseLock(authenticationData, currentLock);
        }
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

        ReentrantLock currentLock = optionallyObtainLockedLock(authenticationData);
        try {
            // If basic auth login to a different realm, then create a basic auth subject
            if (isBasicAuthLogin(authenticationData)) {
                return createBasicAuthSubject(authenticationData, subject);
            } else {
                Subject authenticatedSubject = findSubjectInAuthCache(authenticationData, subject);
                if (authenticatedSubject == null) {
                    authenticatedSubject = performJAASLogin(jaasEntryName, callbackHandler, subject);
                    insertSubjectInAuthCache(authenticationData, authenticatedSubject);
                }
                return authenticatedSubject;
            }
        } finally {
            releaseLock(authenticationData, currentLock);
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
    private void releaseLock(AuthenticationData authenticationData, ReentrantLock currentLock) {
        authenticationGuard.relinquishAccess(authenticationData, currentLock);
    }

    private Subject findSubjectInAuthCache(AuthenticationData authenticationData, Subject partialSubject) throws AuthenticationException {
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
                    String userid = (String) authenticationData.get(AuthenticationData.USERNAME);
                    String password = getPassword((char[]) authenticationData.get(AuthenticationData.PASSWORD));
                    if (userid != null && password != null) {
                        subject = findSubjectByUseridAndPassword(authCacheService, userid, password);
                    } else if (partialSubject != null) {
                        subject = findSubjectBySubjectHashtable(authCacheService, partialSubject);
                    }
                }
            }
        }
        return subject;
    }

    /**
     * @param authCacheService An authentication cache service
     * @param token The cache key, can be either a byte[] (SSO Token) or String (SSO Token Base64 encoded)
     * @param ssoTokenBytes Optional SSO token as byte[], if null, it will be constructed from the token
     * @param authenticaitonData TODO
     * @return the cached subject
     * @throws AuthenticationException if no cached subject was found
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
                    throw new AuthenticationException("Custom cache key missed authentication cache. Need to re-challenge the user to login again.");
                }
            }
        }
        return subject;
    }

    private Subject findSubjectByUseridAndPassword(AuthCacheService authCacheService, String userid, @Sensitive String password) {
        return authCacheService.getSubject(BasicAuthCacheKeyProvider.createLookupKey(getRealm(), userid, password));
    }

    private Subject findSubjectBySubjectHashtable(AuthCacheService authCacheService, Subject partialSubject) {
        Subject subject = null;
        SubjectHelper subjectHelper = new SubjectHelper();
        Hashtable<String, ?> hashtable = subjectHelper.getHashtableFromSubject(partialSubject, new String[] { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY });
        if (hashtable != null) {
            String customCacheKey = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
            Boolean internalCachekeyAssertion = (Boolean) hashtable.get(AuthenticationConstants.INTERNAL_ASSERTION_KEY);

            if (customCacheKey != null && internalCachekeyAssertion != null && internalCachekeyAssertion.equals(Boolean.TRUE)) {
                subject = authCacheService.getSubject(customCacheKey);
                return subject;
            }
        }
        hashtable = subjectHelper.getHashtableFromSubject(partialSubject, new String[] { AttributeNameConstants.WSCREDENTIAL_USERID,
                                                                                         AttributeNameConstants.WSCREDENTIAL_PASSWORD });
        if (hashtable != null) {
            String userid = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_USERID);
            String password = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_PASSWORD);

            String lookupKey;
            if (password != null) {
                lookupKey = BasicAuthCacheKeyProvider.createLookupKey(getRealm(), userid, password);
            } else {
                lookupKey = BasicAuthCacheKeyProvider.createLookupKey(getRealm(), userid);
            }
            subject = authCacheService.getSubject(lookupKey);
        }

        return subject;
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
            }
            catch (LoginException e) {
                if(e instanceof PasswordExpiredException) {
                    throw new PasswordExpiredException(e.getLocalizedMessage());
                }
                else if(e instanceof UserRevokedException) {
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
                authCacheService.insert(authenticatedSubject);
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
     * @param appName the name of the application, used to look up the corresponding user.
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
        DelegationProvider delegationProvider = delegationProviderRef.getService();

        try {
            if (delegationProvider == null) {
                delegationProvider = defaultDelegationProviderRef.getService();
            }
            if (delegationProvider != null) {
                runAsSubject = delegationProvider.getRunAsSubject(roleName, appName);
            }

        } catch (AuthenticationException e) {
            setInvalidDelegationUser(delegationProvider.getDelegationUser());
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
}
