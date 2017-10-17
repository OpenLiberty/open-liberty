/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi.client.security.fat;

import java.io.IOException;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * The callback handler for our test application.
 * <p>
 * When the app tries to login, this class will be called to provide the username and password.
 */
public class AppCallbackHandler implements CallbackHandler {

    public AppCallbackHandler() {
        //TODO: Use injection here instead once defect 169882 is fixed
        //this.credentials = new TestCredentialBean();
    }

    @Inject
    private TestCredentialBean credentials;

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        System.out.println("AppCallbackHandler called");

        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callback;
                nameCallback.setName(credentials.getUsername());
                System.out.println("Name callback: " + nameCallback.getName());
            } else if (callback instanceof PasswordCallback) {
                PasswordCallback pwCallback = (PasswordCallback) callback;
                pwCallback.setPassword(credentials.getPassword());
                System.out.println("Password callback: " + String.valueOf(pwCallback.getPassword()));
            } else {
                System.out.println("Unknown callback: " + callback.toString());
            }
        }
    }
}
