/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;

/**
 *
 */
public class AuthenticationResultTest {

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.AuthenticationResult#toString()}.
     */
    @Test
    public void testToString_success() {
        AuthenticationResult result = new AuthenticationResult(AuthResult.SUCCESS, "successMessage");
        assertEquals("Did not get expected toString result",
                     "AuthenticationResult status=SUCCESS",
                     result.toString());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.AuthenticationResult#toString()}.
     */
    @Test
    public void testToString_failure() {
        AuthenticationResult result = new AuthenticationResult(AuthResult.FAILURE, "failureMessage");
        assertEquals("Did not get expected toString result",
                     "AuthenticationResult status=FAILURE reason=failureMessage",
                     result.toString());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.AuthenticationResult#toString()}.
     */
    @Test
    public void testToString_401() {
        AuthenticationResult result = new AuthenticationResult(AuthResult.SEND_401, "authRealm");
        assertEquals("Did not get expected toString result",
                     "AuthenticationResult status=SEND_401 realm=authRealm",
                     result.toString());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.AuthenticationResult#toString()}.
     */
    @Test
    public void testToString_redirect() {
        AuthenticationResult result = new AuthenticationResult(AuthResult.REDIRECT, "newURL");
        assertEquals("Did not get expected toString result",
                     "AuthenticationResult status=REDIRECT redirectURL=newURL",
                     result.toString());
    }

}
