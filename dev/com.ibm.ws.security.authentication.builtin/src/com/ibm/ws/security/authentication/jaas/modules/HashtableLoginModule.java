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
package com.ibm.ws.security.authentication.jaas.modules;

import java.io.IOException;
import java.security.Principal;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.mp.jwt.proxy.MpJwtHelper;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Handles username/password, userId(identity assertion), or uniquedId/securityName login.
 */
public class HashtableLoginModule extends ServerCommonLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(HashtableLoginModule.class);
    private Object customCacheKey = null;
    private String uniqueUserId = null;
    private String username = null;
    private String urAuthenticatedId = null;
    private String customRealm = null;

    private final String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                                        AttributeNameConstants.WSCREDENTIAL_USERID,
                                                        AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                                        AttributeNameConstants.WSCREDENTIAL_REALM,
                                                        AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
                                                        AuthenticationConstants.INTERNAL_ASSERTION_KEY,
                                                        AuthenticationConstants.INTERNAL_JSON_WEB_TOKEN };

    private final String[] userIdOnlyProperties = { AttributeNameConstants.WSCREDENTIAL_USERID,
                                                    AuthenticationConstants.INTERNAL_ASSERTION_KEY };

    private final String[] jsonWebTokenProperties = { AuthenticationConstants.INTERNAL_JSON_WEB_TOKEN };

    private boolean uniquedIdAndSecurityNameLogin = false;
    private boolean useIdAndPasswordLogin = false;
    private boolean userIdNoPasswordLogin = false;
    private Hashtable<String, ?> customProperties = null;
    private boolean customPropertiesFromSubject = false;

    /** {@inheritDoc} */
    @Override
    public Callback[] getRequiredCallbacks(CallbackHandler callbackHandler) throws IOException, UnsupportedCallbackException {
        return null;
    }

    /**
     * {@inheritDoc} Look for the Hashtable object either in the shared state
     * or in the passed-in Subject. If we do not find it, just return out of
     * this login module.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean login() throws LoginException {
        if (isAlreadyProcessed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Already processed by other login module, abstaining.");
            }
            return false;
        }
        customProperties = (Hashtable) sharedState.get(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY);
        if (customProperties == null && subject != null) {
            SubjectHelper subjectHelper = new SubjectHelper();
            customProperties = subjectHelper.getHashtableFromSubject(subject, hashtableLoginProperties);
            if (customProperties != null) {
                customPropertiesFromSubject = true;
                sharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, customProperties);
            }
        }

        // We do not have a Hashtable, abstain.
        if (customProperties == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No Hashtable could be found, abstaining.");
            }
            return false;
        }

        customCacheKey = getCustomCacheKey(customProperties);
        customRealm = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM);

        String uniqueId = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID);
        String securityName = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME);
        String userId = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_USERID);
        String password = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_PASSWORD);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (userId != null && uniqueId != null && securityName != null) {
                Tr.debug(tc, "The userId is set in addition to uniqueId and securityName. Only the uniqueId and securityName will be used to create the subject.");
            }
        }

        if (userId != null && password != null) {
            return handleUserIdAndPassword(userId, password);
        } else if (uniqueId != null && securityName != null) {
            return handleUniquedIdAndSecurityName(uniqueId, securityName, customProperties);
        } else if (userId != null && allowLoginWithIdOnly(customProperties)) {
            return handleUserId(userId);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Not enough information in Hashtable to continue, abstaining.");
        }
        return false;
    }

    /**
     * @return
     */
    private boolean allowLoginWithIdOnly(Hashtable<String, ?> customProperties) {
        AuthenticationService authService = getAuthenticationService();
        if (authService != null && authService.isAllowHashTableLoginWithIdOnly())
            return true;

        Boolean assertion = Boolean.FALSE;
        if (customPropertiesFromSubject) {
            Object value = customProperties.get(AuthenticationConstants.INTERNAL_ASSERTION_KEY);
            assertion = (Boolean) (value != null ? value : Boolean.FALSE);
            removeInternalAssertionHashtable(customProperties, userIdOnlyProperties);
        } else {
            String[] hashtableInternalProperty = { AuthenticationConstants.INTERNAL_ASSERTION_KEY };
            Hashtable<String, ?> internalProperties = subjectHelper.getHashtableFromSubject(subject, hashtableInternalProperty);
            if (internalProperties != null && !internalProperties.isEmpty()) {
                assertion = Boolean.TRUE;
                removeInternalAssertionHashtable(internalProperties, userIdOnlyProperties);
            }
        }
        if (assertion.booleanValue()) {
            return true;
        }

        return false;
    }

    /**
     * @param userId
     * @return
     * @throws AuthenticationException
     * @throws RegistryException
     */
    private boolean handleUserId(String userId) throws AuthenticationException {
        userIdNoPasswordLogin = true;
        setAlreadyProcessed();
        try {
            setAlreadyProcessed();
            userIdNoPasswordLogin = true;
            UserRegistry userRegistry = getUserRegistry();
            String ret = userRegistry.getUniqueUserId(userId);
            if (ret != null) {
                username = userId;
                uniqueUserId = ret;
                setUpTemporarySubject();
                addJsonWebToken(temporarySubject);
                updateSharedState();
                return true;
            }

        } catch (EntryNotFoundException e) {
            Tr.audit(tc, "JAAS_AUTHENTICATION_FAILED_BADUSER", userId);
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        }
        return false;
    }

    /**
     * @param userId
     * @param password
     * @return
     * @throws Exception
     * @throws AuthenticationException
     * @throws LoginException
     */
    @FFDCIgnore(AuthenticationException.class)
    private boolean handleUserIdAndPassword(String userId, String password) throws AuthenticationException, LoginException {
        try {
            setAlreadyProcessed();
            useIdAndPasswordLogin = true;

            UserRegistry userRegistry = getUserRegistry();
            urAuthenticatedId = userRegistry.checkPassword(userId, password);
            if (urAuthenticatedId != null) {
                username = getSecurityName(userId, urAuthenticatedId);
                uniqueUserId = userRegistry.getUniqueUserId(urAuthenticatedId);
                setUpTemporarySubject();
                updateSharedState();
                return true;
            } else {
                Tr.audit(tc, "JAAS_AUTHENTICATION_FAILED_BADUSERPWD", userId);
                throw new AuthenticationException(TraceNLS.getFormattedMessage(
                                                                               this.getClass(),
                                                                               TraceConstants.MESSAGE_BUNDLE,
                                                                               "JAAS_AUTHENTICATION_FAILED_BADUSERPWD",
                                                                               new Object[] { userId },
                                                                               "CWWKS1100A: Authentication failed for the userid {0}. A bad userid and/or password was specified."));
            }
        } catch (AuthenticationException e) {
            // FFDC is ignored for bad user/pwd.
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @return
     * @throws AuthenticationException
     * @throws Exception
     * @throws Exception
     */
    private boolean handleUniquedIdAndSecurityName(String uniqueId, String securityName, Hashtable<String, ?> customProperties) throws AuthenticationException {
        setAlreadyProcessed();
        temporarySubject = new Subject();
        temporarySubject.getPrivateCredentials().add(customProperties);
        uniquedIdAndSecurityNameLogin = true;

        uniqueUserId = uniqueId;
        username = securityName;
        String accessId = uniqueId;
        try {
            if (!AccessIdUtil.isUserAccessId(accessId)) {

                accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER,
                                                       getRealm(customProperties),
                                                       username);
            }
            setPrincipalAndCredentials(temporarySubject, username, null, username, accessId, WSPrincipal.AUTH_METHOD_HASH_TABLE);
            addJaspicPrincipal(temporarySubject);
            addJsonWebToken(temporarySubject);
            updateSharedState();
        } catch (Exception e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        }
        return true;
    }

    /**
     * @return
     * @throws RegistryException
     */
    private String getRealm(Hashtable<String, ?> customProperties) {
        String realm = customRealm;
        if (realm == null) {
            realm = AccessIdUtil.getRealm(uniqueUserId);
            if (realm == null) {
                UserRegistry userRegistry;
                try {
                    userRegistry = getUserRegistry();
                    realm = userRegistry.getRealm();
                } catch (RegistryException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getUserRegistry() caught an exception: " + e.getMessage());
                    }
                }
            }
            if (realm == null) {
                realm = "defaultRealm";
            }
        }
        return realm;
    }

    /**
     * If a JASPIC provider supplied a Principal via CallerPrincipalCallback
     * then put the Principal in the Principals set and in the WSCredential
     *
     * @param subject
     */
    private void addJaspicPrincipal(Subject subject) throws Exception {
        Principal jaspiPrincipal = (Principal) customProperties.get("com.ibm.wsspi.security.cred.jaspi.principal");
        if (jaspiPrincipal != null) {
            WSCredential wsCredential = null;
            Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
            Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
            if (wsCredentialsIterator.hasNext()) {
                wsCredential = wsCredentialsIterator.next();
                if (wsCredential != null) // paranoid safety check (it's gm time)
                    wsCredential.set("com.ibm.wsspi.security.cred.jaspi.principal", jaspiPrincipal);
            }
            subject.getPrincipals().add(jaspiPrincipal);
        }
    }

    private void addJsonWebToken(Subject subject) {
        if (customProperties != null && customProperties.get(AuthenticationConstants.INTERNAL_JSON_WEB_TOKEN) != null) {
            MpJwtHelper.addJsonWebToken(subject, customProperties, AuthenticationConstants.INTERNAL_JSON_WEB_TOKEN);
            removeInternalAssertionHashtable(customProperties, jsonWebTokenProperties);
        }
    }

    private void setUpTemporarySubject() throws Exception {
        temporarySubject = new Subject();
        UserRegistry userRegistry = getUserRegistry();
        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER,
                                                      userRegistry.getRealm(),
                                                      uniqueUserId);
        setPrincipalAndCredentials(temporarySubject, username, urAuthenticatedId, username, accessId, WSPrincipal.AUTH_METHOD_HASH_TABLE);
        addJaspicPrincipal(temporarySubject);
    }

    /**
     * @param customProperties
     */
    private Object getCustomCacheKey(Hashtable<String, ?> customProperties) {

        Object cacheKey = customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
        if ((cacheKey != null) && (cacheKey instanceof java.lang.String) && ((String) cacheKey).equals(""))
            cacheKey = null;
        return cacheKey;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        if (customCacheKey == null && !uniquedIdAndSecurityNameLogin && !useIdAndPasswordLogin && !userIdNoPasswordLogin) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Authentication did not occur for this login module, abstaining.");
            return false;
        }
        if (uniquedIdAndSecurityNameLogin || useIdAndPasswordLogin || userIdNoPasswordLogin) {
            setUpSubject();
        }
        if (customCacheKey != null || customRealm != null) {
            SingleSignonToken ssoToken = getSSOToken(subject);
            if (ssoToken != null) {
                if (customCacheKey != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Add custom cache key into SSOToken");
                    }
                    ssoToken.addAttribute(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, (String) customCacheKey);
                }
                if (customRealm != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Add custom realm into SSOToken");
                    }
                    ssoToken.addAttribute(AttributeNameConstants.WSCREDENTIAL_REALM, customRealm);
                }
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() {
        return cleanup();
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() {
        return cleanup();
    }

    private boolean cleanup() {
        cleanUpSubject();
        customCacheKey = null;
        username = null;
        uniqueUserId = null;
        urAuthenticatedId = null;
        return true;
    }

    private void removeInternalAssertionHashtable(Hashtable<String, ?> props, String propNames[]) {
        Set<Object> publicCredentials = subject.getPublicCredentials();
        publicCredentials.remove(props);
        for (String propName : propNames) {
            props.remove(propName);
        }
        if (!props.isEmpty()) {
            publicCredentials.add(props);
        }
    }
}
