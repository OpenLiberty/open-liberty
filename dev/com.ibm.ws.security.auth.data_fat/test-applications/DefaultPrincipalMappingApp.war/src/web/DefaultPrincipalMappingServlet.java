/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Set;

import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

import com.ibm.websphere.security.auth.data.AuthData;
import com.ibm.websphere.security.auth.data.AuthDataProvider;
import com.ibm.wsspi.security.auth.callback.WSMappingCallbackHandler;

import componenttest.app.FATServlet;

/**
 *
 */
public class DefaultPrincipalMappingServlet extends FATServlet {

    public void programmaticLoginWithDefaultPrincipalMapping() throws Exception {
        HashMap map = new HashMap();
        map.put(com.ibm.wsspi.security.auth.callback.Constants.MAPPING_ALIAS, "myAuthData");
        CallbackHandler callbackHandler = new WSMappingCallbackHandler(map, null);
        LoginContext loginContext = new LoginContext("DefaultPrincipalMapping", callbackHandler);
        loginContext.login();
        Subject subject = loginContext.getSubject();
        Set<PasswordCredential> creds = subject.getPrivateCredentials(PasswordCredential.class);
        PasswordCredential cred = creds.iterator().next(); // Note this assumes there is one, so you would need to write code to protect against there not being one

        assertEquals("The user name must be set in the password credential.", "testUser", cred.getUserName());
        assertEquals("The password must be set in the password credential.", "testPassword", String.valueOf(cred.getPassword()));
    }

    public void programmaticLoginWithDefaultPrincipalMappingAndJava2Security() throws Exception {
        programmaticLoginWithDefaultPrincipalMapping();
    }

    public void getAuthDataUsingAuthDataProvider() throws Exception {
        AuthData authData = AuthDataProvider.getAuthData("myAuthData");
        assertEquals("The user name must be set in the auth data.", "testUser", authData.getUserName());
        assertEquals("The password must be set in the auth data.", "testPassword", String.valueOf(authData.getPassword()));
    }
}
