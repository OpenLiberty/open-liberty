/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.common.auth.module;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.helper.AuthenticateUserHelper;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;

/**
 * <p>
 * Identity Assertion login module
 * </p>
 * 
 * <p>
 * A principal will be logged in if a trust is established. This login module considers trust to
 * be established if the shared state contains a Map called com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.state.
 * The Map should contain the following variables:
 * <ul>
 * <li> com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.trust set to true </li>
 * <li> com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.principal containing a java.Security.Principal to hold the
 * login identity.</li>
 * <li> OR com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.certificates containing a java.security.cert.X509Certificate[]
 * to hold the login identity. </li>
 * </ul>
 * </p>
 * <p>
 * If the Map is provided in the shared state then the identity will be logged in.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 * @ibm-spi
 * 
 */
public class IdentityAssertionLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(IdentityAssertionLoginModule.class, "Authentication",
                                                         "com.ibm.ws.security.authentication.internal.resources.AuthenticationMessages");
    private static final String KEY_TRUST_STATE = "com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.state";
    private static final String KEY_PRINCIPAL = "com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.principal";
    private static final String KEY_CERTIFICATES = "com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.certificates";
    private static final String KEY_TRUSTED = "com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.trusted";

    private Subject subject;
    private Map sharedState;
    protected Subject temporarySubject;
    private Principal trustedPrincipal;
    private X509Certificate[] certificateChain;
    private String username;
    private UserRegistry userRegistry;

    /**
     * <p>Initialize this login module.</p>
     * 
     * <p>
     * This is called by the <code>LoginContext</code> after this login module is
     * instantiated. The relevant information is passed from the <code>LoginContext</code>
     * to this login module. If the login module does not understands any of the data
     * stored in the <code>sharedState</code> and <code>options</code> parameters,
     * they can be ignored.
     * </p>
     * 
     * @param subject The subject to be authenticated.
     * @param callbackHandler
     *            A <code>CallbackHandler</code> for communicating with the end user to gather login information (e.g., username and password).
     * @param sharedState
     *            The state shared with other configured login modules.
     * @param options The options specified in the login configuration for this particular login module.
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        this.subject = subject;
        this.sharedState = sharedState;
    }

    /**
     * <p>
     * Method to authenticate a Subject (first phase).
     * </p>
     * 
     * <p>
     * This method authenticates a Subject. It uses the Map stored in the shared state property com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.state.
     * The com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.trusted key in the Map is used to determine trust. If true
     * then trusted if false then it not trusted. When trust is established then the principal stored in either the
     * com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.principal or com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.certificates
     * key will contain the identity to login as.
     * </p>
     * 
     * @return <code><b>true</b></code> if the authentication succeeded, or <code><b>false</b></code>
     *         if this login module should be ignored.
     * @exception WSLoginFailedException
     *                If the authentication fails.
     */
    @Override
    @FFDCIgnore(WSLoginFailedException.class)
    public boolean login() throws WSLoginFailedException {
        try {
            userRegistry = getUserRegistry();
            Map trustState = (Map) sharedState.get(KEY_TRUST_STATE);
            setUserNameFromDataInTrustState(trustState);
            setUpTemporarySubject();
        } catch (WSLoginFailedException e) {
            // NO FFDC: WSLoginFailedException are expected
            throw e; // no-need to wrap
        } catch (Exception e) {
            throw new WSLoginFailedException(e.getLocalizedMessage());
        }
        return true;
    }

    private void setUserNameFromDataInTrustState(Map trustState) throws WSLoginFailedException {
        validateTrust(trustState);
        trustedPrincipal = (Principal) trustState.get(KEY_PRINCIPAL);
        certificateChain = (X509Certificate[]) trustState.get(KEY_CERTIFICATES);
        validateSufficientData();
        setUserName();
    }

    private void validateTrust(Map trustState) throws WSLoginFailedException {
        if (trustState == null || ((Boolean) trustState.get(KEY_TRUSTED)) == false) {
            throw new WSLoginFailedException("No Trust information for trust validation.");
        }

        Boolean trusted = ((Boolean) trustState.get(KEY_TRUSTED));
        if (trusted == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Missing a trust key");
            }
            throw new WSLoginFailedException("No Trust Validator configured for trust validation, identity assertion is disabled.");
        }

        if (trusted == false) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "trust is false");
            }
            throw new WSLoginFailedException("No Trust established for trust validation, identity assertion is disabled.");
        }
    }

    private void validateSufficientData() throws WSLoginFailedException {
        if (trustedPrincipal == null && ((certificateChain != null && certificateChain.length != 0) == false)) {
            throw new WSLoginFailedException("No principal or X509Certificate provided to login new user with.");
        }
    }

    private void setUserName() throws WSLoginFailedException {
        username = getUserNameFromPrincipal();
        if (username == null) {
            username = getUserNameFromCertificate();
        }
    }

    private String getUserNameFromPrincipal() {
        String name = null;
        if (trustedPrincipal != null) {
            name = trustedPrincipal.getName();
            if (name != null) {
                int realmDelimiterIndex = name.lastIndexOf("/");
                if (realmDelimiterIndex >= 0) {
                    name = name.substring(realmDelimiterIndex + 1);
                }
            }
        }
        return name;
    }

    private String getUserNameFromCertificate() throws WSLoginFailedException {
        String name = null;
        try {
            name = userRegistry.mapCertificate(certificateChain[0]);
        } catch (Exception e) {
            // if (tc.isDebugEnabled()) Tr.debug(tc, "Exception when calling contextManager.login");
            throw new WSLoginFailedException(e.getLocalizedMessage());
        }
        return name;
    }

    private void setUpTemporarySubject() throws Exception {
        AuthenticateUserHelper authenticateUserHelper = new AuthenticateUserHelper();
        AuthenticationService authenticationService = getAuthenticationService();
        temporarySubject = authenticateUserHelper.authenticateUser(authenticationService, username,
                                                                   JaasLoginConfigConstants.SYSTEM_DEFAULT);
    }

    /**
     * Commit the authentication (phase 2).
     * 
     * <p>
     * If the login module authentication attempted in phase 1 succeeded, then relevant principals and credentials
     * are associated with the subject. If the authentication attempted in phase 1 failed, then this method
     * removes/destroys any state that was originally saved.
     * </p>
     * 
     * @exception WSLoginFailedException if the commit fails
     * 
     * @return true if this LoginModule's own login and commit attempts
     *         succeeded, or false otherwise.
     */
    @Override
    public boolean commit() throws WSLoginFailedException {
        if (temporarySubject == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Authentication did not occur for this login module, abstaining.");
            }
            return false;
        }
        setUpSubject();
        return true;
    }

    /**
     * Commit the newly created elements into the original Subject.
     */
    protected void setUpSubject() throws WSLoginFailedException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    updateSubjectWithTemporarySubjectContents();
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw new WSLoginFailedException("Unable to setup the Subject: " + e.getLocalizedMessage());
        }
    }

    /**
     * Sets the subject with the temporary subject contents that was not set already from the
     * shared state.
     */
    protected void updateSubjectWithTemporarySubjectContents() {
        subject.getPrincipals().addAll(temporarySubject.getPrincipals());
        subject.getPublicCredentials().addAll(temporarySubject.getPublicCredentials());
        subject.getPrivateCredentials().addAll(temporarySubject.getPrivateCredentials());
    }

    /**
     * Abort the authentication (second phase).
     * 
     * <p>
     * This method is called if the <code>LoginContext</code>'s overall authentication failed.
     * </p>
     * <p>
     * If this login module's authentication attempt succeeded, then this method cleans up the previous state
     * saved in phase 1.
     * </p>
     * 
     * @exception LoginException if the abort fails
     * 
     * @return false if this LoginModule's own login and/or commit attempts
     *         failed, and true otherwise.
     */
    @Override
    public boolean abort() throws LoginException {
        cleanUpSubject();
        username = null;
        return true;
    }

    /**
     * Logout the user
     * 
     * <p>
     * The principals and credentials are removed from the Shared state.
     * </p>
     * 
     * @exception LoginException if the logout fails
     * 
     * @return true in all cases (this <code>LoginModule</code>
     *         should not be ignored).
     */
    @Override
    public boolean logout() throws LoginException {
        cleanUpSubject();
        username = null;
        return true;
    }

    private UserRegistry getUserRegistry() throws RegistryException {
        return JAASServiceImpl.getUserRegistry();
    }

    private AuthenticationService getAuthenticationService() {
        return JAASServiceImpl.getAuthenticationService();
    }

    private void cleanUpSubject() {
        if (temporarySubject != null) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    subject.getPrincipals().removeAll(temporarySubject.getPrincipals());
                    subject.getPublicCredentials().removeAll(temporarySubject.getPublicCredentials());
                    subject.getPrivateCredentials().removeAll(temporarySubject.getPrivateCredentials());
                    return null;
                }
            });
        }

        temporarySubject = null;
    }

}
