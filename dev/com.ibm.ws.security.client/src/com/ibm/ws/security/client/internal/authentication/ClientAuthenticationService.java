/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.client.internal.authentication;

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.CredentialException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.jaas.common.callback.CallbackHandlerAuthenticationData;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Authentication Service for the Client
 */

@Component(service = ClientAuthenticationService.class,
                name = "com.ibm.ws.security.client.authentication",
                immediate = true,
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = "service.vendor=IBM")
public class ClientAuthenticationService {
    static final String KEY_CREDENTIALS_SERVICE = "credentialsService";
    private final AtomicServiceReference<CredentialsService> credentialsServiceRef = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIALS_SERVICE);

    @Reference(service = CredentialsService.class,
                    name = KEY_CREDENTIALS_SERVICE)
    protected void setCredentialsService(ServiceReference<CredentialsService> reference) {
        credentialsServiceRef.setReference(reference);
    }

    protected void unsetCredentialsService(ServiceReference<CredentialsService> reference) {
        credentialsServiceRef.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        credentialsServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        credentialsServiceRef.deactivate(cc);
    }

    /**
     * Authenticating on the client will only create a "dummy" basic auth subject and does not
     * truly authenticate anything. This subject is sent to the server ove CSIv2 where the real
     * authentication happens.
     * 
     * @param callbackHandler the callbackhandler to get the authentication data from, must not be <null>
     * @param subject the partial subject, can be null
     * @return a basic auth subject with a basic auth credential
     * @throws WSLoginFailedException if a callback handler is not specified
     * @throws CredentialException if there was a problem creating the WSCredential
     * @throws IOException if there is an I/O error
     */
    public Subject authenticate(CallbackHandler callbackHandler, Subject subject) throws WSLoginFailedException, CredentialException {
        if (callbackHandler == null) {
            throw new WSLoginFailedException(TraceNLS.getFormattedMessage(
                                                                          this.getClass(),
                                                                          TraceConstants.MESSAGE_BUNDLE,
                                                                          "JAAS_LOGIN_NO_CALLBACK_HANDLER",
                                                                          new Object[] {},
                                                                          "CWWKS1170E: The login on the client application failed because the CallbackHandler implementation is null. Ensure a valid CallbackHandler implementation is specified either in the LoginContext constructor or in the client application's deployment descriptor."));
        }

        CallbackHandlerAuthenticationData cAuthData = new CallbackHandlerAuthenticationData(callbackHandler);
        AuthenticationData authenticationData = null;
        try {
            authenticationData = cAuthData.createAuthenticationData();
        } catch (IOException e) {
            throw new WSLoginFailedException(TraceNLS.getFormattedMessage(
                                                                          this.getClass(),
                                                                          TraceConstants.MESSAGE_BUNDLE,
                                                                          "JAAS_LOGIN_UNEXPECTED_EXCEPTION",
                                                                          new Object[] { e.getLocalizedMessage() },
                                                                          "CWWKS1172E: The login on the client application failed because of an unexpected exception. Review the logs to understand the cause of the exception. The exception is: "
                                                                                          + e.getLocalizedMessage()));
        } catch (UnsupportedCallbackException e) {
            throw new WSLoginFailedException(TraceNLS.getFormattedMessage(
                                                                          this.getClass(),
                                                                          TraceConstants.MESSAGE_BUNDLE,
                                                                          "JAAS_LOGIN_UNEXPECTED_EXCEPTION",
                                                                          new Object[] { e.getLocalizedMessage() },
                                                                          "CWWKS1172E: The login on the client application failed because of an unexpected exception. Review the logs to understand the cause of the exception. The exception is: "
                                                                                          + e.getLocalizedMessage()));
        }
        return createBasicAuthSubject(authenticationData, subject);
    }

    /**
     * Create the basic auth subject using the given authentication data
     * 
     * @param authenticationData the user, password and realm to create the subject
     * @param subject the partial subject, can be null
     * @return a basic auth subject that has not been authenticated yet
     * @throws WSLoginFailedException if the user or password are <null> or empty
     * @throws CredentialException if there was a problem creating the WSCredential
     */
    protected Subject createBasicAuthSubject(AuthenticationData authenticationData, Subject subject) throws WSLoginFailedException, CredentialException {
        Subject basicAuthSubject = subject != null ? subject : new Subject();

        String loginRealm = (String) authenticationData.get(AuthenticationData.REALM);
        String username = (String) authenticationData.get(AuthenticationData.USERNAME);
        String password = getPassword((char[]) authenticationData.get(AuthenticationData.PASSWORD));
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new WSLoginFailedException(TraceNLS.getFormattedMessage(
                                                                          this.getClass(),
                                                                          TraceConstants.MESSAGE_BUNDLE,
                                                                          "JAAS_LOGIN_MISSING_CREDENTIALS",
                                                                          new Object[] {},
                                                                          "CWWKS1171E: The login on the client application failed because the user name or password is null. Ensure the CallbackHandler implementation is gathering the necessary credentials."));
        }
        CredentialsService credentialsService = credentialsServiceRef.getServiceWithException();
        credentialsService.setBasicAuthCredential(basicAuthSubject, loginRealm, username, password);
        Principal principal = new WSPrincipal(username, null, WSPrincipal.AUTH_METHOD_BASIC);
        basicAuthSubject.getPrincipals().add(principal);
        return basicAuthSubject;
    }

    @Sensitive
    private String getPassword(@Sensitive char[] passwordBytes) {
        String password = null;
        if (passwordBytes != null) {
            password = String.valueOf(passwordBytes);
        }
        return password;
    }
}