/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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
import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
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
    private String internalAuthProvider = null;

    private final String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                                        AttributeNameConstants.WSCREDENTIAL_USERID,
                                                        AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                                        AttributeNameConstants.WSCREDENTIAL_REALM,
                                                        AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
                                                        AuthenticationConstants.INTERNAL_ASSERTION_KEY,
                                                        AuthenticationConstants.INTERNAL_JSON_WEB_TOKEN,
                                                        AuthenticationConstants.INTERNAL_AUTH_PROVIDER };

    private boolean uniquedIdAndSecurityNameLogin = false;
    private boolean useIdAndPasswordLogin = false;
    private boolean userIdNoPasswordLogin = false;
    private Hashtable<String, ?> customProperties = null;

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
        internalAuthProvider = (String) customProperties.get(AuthenticationConstants.INTERNAL_AUTH_PROVIDER);

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
            setWSPrincipal(temporarySubject, username, accessId, WSPrincipal.AUTH_METHOD_HASH_TABLE);
            setCredentials(temporarySubject, username, null);
            setOtherPrincipals(temporarySubject, username, accessId, WSPrincipal.AUTH_METHOD_HASH_TABLE, customProperties);
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

    private void setUpTemporarySubject() throws Exception {
        temporarySubject = new Subject();
        UserRegistry userRegistry = getUserRegistry();
        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER,
                                                      userRegistry.getRealm(),
                                                      uniqueUserId);
        setWSPrincipal(temporarySubject, username, accessId, WSPrincipal.AUTH_METHOD_HASH_TABLE);
        setCredentials(temporarySubject, username, urAuthenticatedId);
        setOtherPrincipals(temporarySubject, username, accessId, WSPrincipal.AUTH_METHOD_HASH_TABLE, customProperties);
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
        if (customCacheKey != null || customRealm != null || internalAuthProvider != null) {
            addCustomAttributesToSSOToken();
            //Recreate the jwtSSOToken with custom Attributes in hashtable such as customCacheKey, customRealm, authProvider
            JwtSSOTokenHelper.addAttributesToJwtSSOToken(subject);
        }

        return true;
    }

    /**
     *
     */
    private void addCustomAttributesToSSOToken() {
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
            if (internalAuthProvider != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Add authentication provider into SSOToken");
                }
                ssoToken.addAttribute(AuthenticationConstants.INTERNAL_AUTH_PROVIDER, internalAuthProvider);
            }
        }
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
}
