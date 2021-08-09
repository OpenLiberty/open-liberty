/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kafka.security;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;

/**
 * LoginModule which supports SASL PLAIN and understands passwords encoded using liberty's securityUtility
 */
public class LibertyLoginModule implements LoginModule {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private Subject subject;
    private String username;
    private String encoded_password;
    private String decoded_password;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.username = (String) options.get(USERNAME);
        this.encoded_password = (String) options.get(PASSWORD);
    }

    @Override
    public boolean login() throws LoginException {
        if (this.encoded_password != null) {
            if (PasswordUtil.isEncrypted(this.encoded_password)) {
                try {
                    this.decoded_password = PasswordUtil.decode(this.encoded_password);
                } catch (InvalidPasswordDecodingException e) {
                    throw new LoginException(e.getMessage());
                } catch (UnsupportedCryptoAlgorithmException e) {
                    throw new LoginException(e.getMessage());
                }
            } else {
                this.decoded_password = this.encoded_password;
            }
        }
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (this.username != null) {
            this.subject.getPublicCredentials().add(username);
        }
        if (this.decoded_password != null) {
            this.subject.getPrivateCredentials().add(decoded_password);
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (this.username != null) {
            this.subject.getPublicCredentials().remove(username);
        }
        if (this.decoded_password != null) {
            this.subject.getPrivateCredentials().remove(decoded_password);
        }
        this.username = null;
        this.encoded_password = null;
        this.decoded_password = null;
        this.subject = null;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return abort();
    }

}
