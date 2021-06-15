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

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.PasswordExpiredException;
import com.ibm.ws.security.authentication.UserRevokedException;
import com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.context.SubjectRegistryManager;
import com.ibm.ws.security.registry.UserRegistry;

/**
 * Handles username and password based authentication, such as BasicAuth.
 */
public class UsernameAndPasswordLoginModule extends ServerCommonLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(UsernameAndPasswordLoginModule.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private UserRegistry userRegistry;
    private String username = null;
    private String urAuthenticatedId = null;

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ AuthenticationException.class, IllegalArgumentException.class, WSLoginFailedException.class })
    public boolean login() throws LoginException {
        if (isAlreadyProcessed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Already processed by other login module, abstaining.");
            }
            return false;
        }

        try {
            Callback[] callbacks = getRequiredCallbacks(callbackHandler);
            String user = ((NameCallback) callbacks[0]).getName();
            char[] passwordChars = ((PasswordCallback) callbacks[1]).getPassword();

            // If we have insufficient data, abstain.
            if (user == null || passwordChars == null) {
                return false;
            }

            if (user.trim().isEmpty()) {
                return false;
            }

            setAlreadyProcessed();

            userRegistry = getUserRegistry();
            urAuthenticatedId = userRegistry.checkPassword(user, String.valueOf(passwordChars));
            if (urAuthenticatedId != null) {
                try {
                    //We only need to start registry detection if there is a SAF registry configured
                    //This method doesn't provide information about the user logging in, only
                    //that there is a SAF registry.
                    SubjectRegistryManager.startSubjectRegistryDetectionOnZOS();
                    username = getSecurityName(user, urAuthenticatedId);
                    setUpTemporarySubject();
                } finally {
                    SubjectRegistryManager.clearSubjectRegistryDetectionOnZOS();
                }
                updateSharedState();
                return true;
            } else {
                Tr.audit(tc, "JAAS_AUTHENTICATION_FAILED_BADUSERPWD", user);
                throw new AuthenticationException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                               TraceConstants.MESSAGE_BUNDLE,
                                                                               "JAAS_AUTHENTICATION_FAILED_BADUSERPWD",
                                                                               new Object[] { user },
                                                                               "CWWKS1100A: Authentication failed for the userid {0}. A bad userid and/or password was specified."));
            }
        } catch (com.ibm.ws.security.registry.PasswordExpiredException e) {
            throw new PasswordExpiredException(e.getLocalizedMessage(), e);
        } catch (com.ibm.ws.security.registry.UserRevokedException e) {
            throw new UserRevokedException(e.getLocalizedMessage(), e);
        } catch (AuthenticationException e) {

            // NO FFDC: AuthenticationExceptions are expected (bad userid/password is pretty normal)
            throw e; // no-need to wrap
        } catch (IllegalArgumentException e) {
            // NO FFDC: This is normal when user and/or password are blank/null
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        } catch (WSLoginFailedException e) {
            // NO FFDC: This is normal when user and/or password are blank/null
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        } catch (LoginException e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            // This is not normal: FFDC will be instrumented
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
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        callbackHandler.handle(callbacks);
        return callbacks;
    }

    /*
     * Populate a temporary subject in response to a successful authentication.
     * We use a temporary Subject because if something goes wrong in this flow,
     * we are not updating the "live" Subject. If performance is a problem, it may
     * be necessary to create a placeholder instead of a subject and modify the credentials
     * service to return a set of credentials or update the holder in order to place in
     * the shared state.
     */
    private void setUpTemporarySubject() throws Exception {
        temporarySubject = new Subject();
        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER,
                                                      userRegistry.getRealm(),
                                                      userRegistry.getUniqueUserId(urAuthenticatedId));
        setWSPrincipal(temporarySubject, username, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        setCredentials(temporarySubject, username, urAuthenticatedId);
        setOtherPrincipals(temporarySubject, username, accessId, WSPrincipal.AUTH_METHOD_PASSWORD, null);
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        if (urAuthenticatedId == null) {
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
        urAuthenticatedId = null;
        username = null;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() {
        cleanUpSubject();
        urAuthenticatedId = null;
        username = null;
        return true;
    }

}
