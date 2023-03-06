/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.checkpoint.fat.passwordutil.web;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Set;

import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.security.auth.data.AuthData;
import com.ibm.websphere.security.auth.data.AuthDataProvider;
import com.ibm.wsspi.security.auth.callback.WSMappingCallbackHandler;

import componenttest.app.FATServlet;

@WebServlet("/DefaultPrincipalMappingServlet")
public class DefaultPrincipalMappingServlet extends FATServlet {

    private static final long serialVersionUID = -2465846638436755113L;

    @Test
    public void programmaticLoginWithDefaultPrincipalMapping() throws Exception {
        /*
         * This test requires access to javax.resource.spi.security package.
         * This package is not available in webProfile. The test currently
         * requires JCA to be enabled for it to work properly.
         */
        HashMap<String, String> map = new HashMap<String, String>();
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

    @Test
    public void getAuthDataUsingAuthDataProvider() throws Exception {
        AuthData authData = AuthDataProvider.getAuthData("myAuthData");
        assertEquals("The user name must be set in the auth data.", "testUser", authData.getUserName());
        assertEquals("The password must be set in the auth data.", "testPassword", String.valueOf(authData.getPassword()));
    }
}
