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
import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.websphere.security.auth.callback.WSAuthMechOidCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.jaas.common.callback.AuthenticationHelper;
import com.ibm.ws.security.jaas.common.callback.JwtTokenCallback;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Handles token based authentication, such as Single Sign-on.
 */
public class TokenLoginModule extends ServerCommonLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(TokenLoginModule.class);
    private static final String LTPA_OID = "oid:1.3.18.0.2.30.2";
    private static final String JWT_OID = "oid:1.3.18.0.2.30.3"; // ?????
    private String accessId = null;
    private Token recreatedToken;

    private final String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                                        AttributeNameConstants.WSCREDENTIAL_USERID,
                                                        AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                                        AttributeNameConstants.WSCREDENTIAL_REALM,
                                                        AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
                                                        AuthenticationConstants.INTERNAL_ASSERTION_KEY,
                                                        AuthenticationConstants.INTERNAL_JSON_WEB_TOKEN };

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ InvalidTokenException.class, TokenExpiredException.class })
    public boolean login() throws LoginException {
        if (isAlreadyProcessed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Already processed by other login module, abstaining.");
            }
            return false;
        }

        try {
            Callback[] callbacks = getRequiredCallbacks(callbackHandler);
            byte[] token = ((WSCredTokenCallbackImpl) callbacks[0]).getCredToken();
            String jwtToken = ((JwtTokenCallback) callbacks[2]).getToken();
            // If we have insufficient data, abstain.
            if (token == null && jwtToken == null) {
                return false;
            }

            setAlreadyProcessed();

            if (jwtToken != null) {
                setUpTemporaryUserSubjectForJsonWebToken(jwtToken);
            } else {
                byte[] credToken = AuthenticationHelper.copyCredToken(token);
                TokenManager tokenManager = getTokenManager();
                recreatedToken = tokenManager.recreateTokenFromBytes(credToken);
                accessId = recreatedToken.getAttributes("u")[0];
                if (AccessIdUtil.isServerAccessId(accessId)) {
                    setUpTemporaryServerSubject();
                } else {
                    setUpTemporaryUserSubject();
                }
            }
            updateSharedState();
            return true;
        } catch (

        InvalidTokenException e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        } catch (TokenExpiredException e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Gets the required Callback objects needed by this login module.
     *
     * @param callbackHandler
     * @return
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    @Override
    public Callback[] getRequiredCallbacks(CallbackHandler callbackHandler) throws IOException, UnsupportedCallbackException {
        Callback[] callbacks = new Callback[3];
        callbacks[0] = new WSCredTokenCallbackImpl("Credential Token");
        callbacks[1] = new WSAuthMechOidCallbackImpl("AuthMechOid");
        callbacks[2] = new JwtTokenCallback();
        callbackHandler.handle(callbacks);
        return callbacks;
    }

    /**
     * @see #setUpTemporaryUserSubject()
     * @throws Exception
     */
    private void setUpTemporaryServerSubject() throws Exception {
        temporarySubject = new Subject();
        temporarySubject.getPrivateCredentials().add(recreatedToken);
        String securityName = AccessIdUtil.getUniqueId(accessId);
        setWSPrincipal(temporarySubject, securityName, accessId, WSPrincipal.AUTH_METHOD_TOKEN);
        setCredentials(temporarySubject, securityName, null);
        setPrincipals(temporarySubject, securityName, accessId, WSPrincipal.AUTH_METHOD_TOKEN, null);
    }

    /**
     * Populate a temporary subject in response to a successful authentication.
     * We use a temporary Subject because if something goes wrong in this flow,
     * we are not updating the "live" Subject. If performance is a problem, it may
     * be necessary to create a placeholder instead of a subject and modify the credentials
     * service to return a set of credentials or update the holder in order to place in
     * the shared state.
     *
     * @throws Exception
     */
    private void setUpTemporaryUserSubject() throws Exception {
        temporarySubject = new Subject();
        temporarySubject.getPrivateCredentials().add(recreatedToken);
        UserRegistry ur = getUserRegistry();
        String securityName = ur.getUserSecurityName(AccessIdUtil.getUniqueId(accessId));
        securityName = getSecurityName(securityName, securityName); // Special handling for LDAP under here.
        setWSPrincipal(temporarySubject, securityName, accessId, WSPrincipal.AUTH_METHOD_TOKEN);
        setCredentials(temporarySubject, securityName, null);
        setPrincipals(temporarySubject, securityName, accessId, WSPrincipal.AUTH_METHOD_TOKEN, null);
    }

    private void setUpTemporaryUserSubjectForJsonWebToken(String jwtToken) throws Exception {
        temporarySubject = new Subject();
        temporarySubject = JwtSSOTokenHelper.handleJwtSSOToken(jwtToken);
        SubjectHelper subjectHelper = new SubjectHelper();
        //TODO: call JwtSSOTokenHelper to get accessId, securityname and groups
        Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(temporarySubject, hashtableLoginProperties);
        accessId = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID);
        String securityName = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME);
        setWSPrincipal(temporarySubject, securityName, accessId, WSPrincipal.AUTH_METHOD_HASH_TABLE);
        setCredentials(temporarySubject, securityName, securityName);
        setPrincipals(temporarySubject, securityName, accessId, WSPrincipal.AUTH_METHOD_HASH_TABLE, customProperties);
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        if (accessId == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Authentication did not occur for this login module, abstaining.");
            return false;
        }
        setUpSubject();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() {
        cleanUpSubject();
        accessId = null;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() {
        cleanUpSubject();
        accessId = null;
        return true;
    }
}
