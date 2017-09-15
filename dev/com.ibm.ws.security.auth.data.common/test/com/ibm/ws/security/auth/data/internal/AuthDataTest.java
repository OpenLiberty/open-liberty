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
package com.ibm.ws.security.auth.data.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class AuthDataTest {

    private static final String TEST_USER = "testUser";
    private static final String ANOTHER_TEST_USER = "anotherTestUser";
    private static final String TEST_USER_PWD = "testUserPwd";
    private static final String ANOTHER_TEST_USER_PWD = "anotherTestUserPwd";

    @Test
    public void getName() throws Exception {
        Map<String, Object> testProps = new HashMap<String, Object>();
        testProps.put(AuthDataImpl.CFG_KEY_USER, TEST_USER);
        AuthDataImpl authData = new AuthDataImpl();
        authData.activate(testProps);

        assertEquals("There must be user name in the auth data config.", TEST_USER, authData.getUserName());
    }

    @Test
    public void getName_DifferentName() throws Exception {
        Map<String, Object> testProps = new HashMap<String, Object>();
        testProps.put(AuthDataImpl.CFG_KEY_USER, ANOTHER_TEST_USER);
        AuthDataImpl authData = new AuthDataImpl();
        authData.activate(testProps);

        assertEquals("There must be user name in the auth data config.", ANOTHER_TEST_USER, authData.getUserName());
    }

    @Test
    public void getPassword() throws Exception {
        Map<String, Object> testProps = new HashMap<String, Object>();
        testProps.put(AuthDataImpl.CFG_KEY_PASSWORD, new SerializableProtectedString(TEST_USER_PWD.toCharArray()));
        AuthDataImpl authData = new AuthDataImpl();
        authData.activate(testProps);

        assertEquals("There must be password in the auth data config.", TEST_USER_PWD, String.valueOf(authData.getPassword()));
    }

    @Test
    public void getPassword_DifferentPassword() throws Exception {
        Map<String, Object> testProps = new HashMap<String, Object>();
        testProps.put(AuthDataImpl.CFG_KEY_PASSWORD, new SerializableProtectedString(ANOTHER_TEST_USER_PWD.toCharArray()));
        AuthDataImpl authData = new AuthDataImpl();
        authData.activate(testProps);

        assertEquals("There must be password in the auth data config.", ANOTHER_TEST_USER_PWD, String.valueOf(authData.getPassword()));
    }

    @Test
    public void getPassword_Encoded() throws Exception {
        String encodedPassword = PasswordUtil.passwordEncode(TEST_USER_PWD);
        Map<String, Object> testProps = new HashMap<String, Object>();
        testProps.put(AuthDataImpl.CFG_KEY_PASSWORD, new SerializableProtectedString(encodedPassword.toCharArray()));
        AuthDataImpl authData = new AuthDataImpl();
        authData.activate(testProps);

        assertEquals("There must be password in the auth data config.", TEST_USER_PWD, String.valueOf(authData.getPassword()));
    }

    @Test
    public void get_Null() throws Exception {
        Map<String, Object> testProps = new HashMap<String, Object>();
        AuthDataImpl authData = new AuthDataImpl();
        authData.activate(testProps);

        assertEquals("There must be a null user name in the auth data config.", null, authData.getUserName());
        assertEquals("There must be an empty password in the auth data config.", "", String.valueOf(authData.getPassword()));
    }

}
