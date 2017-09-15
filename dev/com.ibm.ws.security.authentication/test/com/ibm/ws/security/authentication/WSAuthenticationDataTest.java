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
package com.ibm.ws.security.authentication;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.websphere.ras.ProtectedString;

import test.common.SharedOutputManager;

/**
 *
 */
public class WSAuthenticationDataTest {

    @Rule
    public static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testConstructor() {
        AuthenticationData data = new WSAuthenticationData();
        assertNotNull("There must be AuthenticationData", data);
    }

    @Test
    public void testSetMethod() {
        AuthenticationData data = new WSAuthenticationData();
        String key = "KEY";
        String value = "VALUE";
        data.set(key, value);
    }

    @Test
    public void testGetMethod() {
        AuthenticationData data = new WSAuthenticationData();
        String key = "KEY";
        assertNull("Nothing set so should be null", data.get(key));
    }

    @Test
    public void testSetMethodIsSettingValue() {
        AuthenticationData data = new WSAuthenticationData();
        String key = "KEY";
        String value = "VALUE";
        data.set(key, value);
        String retrievedValue = (String) data.get(key);
        assertEquals("The value must be equals to the retrieved value", value, retrievedValue);
    }

    @Test
    public void testSetMethodOverridesValue() {
        AuthenticationData data = new WSAuthenticationData();
        String key = "KEY";
        String value = "VALUE";
        String overrideValue = "OVERRIDE_VALUE";
        data.set(key, value);
        String initialValue = (String) data.get(key);
        data.set(key, overrideValue);
        String finalValue = (String) data.get(key);
        assertFalse("The final value must be different from the initial value", finalValue.equals(initialValue));
    }

    @Test
    public void testMultipleKeys() {
        AuthenticationData data = new WSAuthenticationData();
        String key = "KEY";
        String value = "VALUE";
        String anotherKey = "ANOTHER_KEY";
        String anotherValue = "ANOTHER_VALUE";
        data.set(key, value);
        data.set(anotherKey, anotherValue);
        String retrievedValue = (String) data.get(key);
        assertFalse("The retrieved value must be different from the another value", retrievedValue.equals(anotherValue));
    }

    @Test
    public void testIsSensitive() {
        WSAuthenticationData data = new WSAuthenticationData();
        data.isSensitive(AuthenticationData.PASSWORD);
    }

    @Test
    public void testSetWithSensitiveCharsAreCorrectlyRetrieved() {
        AuthenticationData data = new WSAuthenticationData();
        String password = "testuserpwd";
        char[] passwordChars = password.toCharArray();
        data.set(AuthenticationData.PASSWORD, passwordChars);
        char[] retrievedPassword = (char[]) data.get(AuthenticationData.PASSWORD);
        assertEquals("Sensitive data must be stored as ProtectedString", password, String.valueOf(retrievedPassword));
    }

    @Test
    public void testEquals() {
        String user = "testuser";
        String password = "testuserpwd";
        AuthenticationData firstData = new WSAuthenticationData();
        AuthenticationData secondData = new WSAuthenticationData();
        firstData.set(AuthenticationData.USERNAME, user);
        firstData.set(AuthenticationData.PASSWORD, password.toCharArray());
        secondData.set(AuthenticationData.USERNAME, user);
        secondData.set(AuthenticationData.PASSWORD, password.toCharArray());
        assertEquals("The authentication data objects must be equals.", firstData, secondData);
    }

    @Test
    public void testEqualsWithTokens() {
        String tokenString = "this is a dummy token";
        AuthenticationData firstData = new WSAuthenticationData();
        AuthenticationData secondData = new WSAuthenticationData();
        firstData.set(AuthenticationData.TOKEN, tokenString.getBytes());
        secondData.set(AuthenticationData.TOKEN, tokenString.getBytes());
        assertEquals("The authentication data objects must be equals.", firstData, secondData);
    }

    @Test
    public void setMethod_acceptsNullPassword() {
        AuthenticationData data = new WSAuthenticationData();
        data.set(AuthenticationData.PASSWORD, null);
        assertNull("Did not get back expected null value for 'password'",
                   data.get(AuthenticationData.PASSWORD));
    }

    @Test
    public void setMethod_acceptsNullProtectedStringPassword() {
        AuthenticationData data = new WSAuthenticationData();
        data.set(AuthenticationData.PASSWORD, ProtectedString.NULL_PROTECTED_STRING);
        assertNull("Did not get back expected null value for 'password' using ProtectedString input",
                   data.get(AuthenticationData.PASSWORD));
    }

    /**
     * Get the 'password' value from AuthenticationData and compare its String value to
     * the expected String.
     */
    private void checkPasswordResult(AuthenticationData data, String expected, String inputDescription) {
        assertTrue("Returned 'password' value was not an instance of char[]",
                   data.get(AuthenticationData.PASSWORD) instanceof char[]);

        char[] pwd = (char[]) data.get(AuthenticationData.PASSWORD);
        assertEquals("Did not get back expected String for 'password' using " + inputDescription,
                     expected, String.valueOf(pwd));
    }

    @Test
    public void setMethod_acceptsEmptyStringPassword() {
        AuthenticationData data = new WSAuthenticationData();
        data.set(AuthenticationData.PASSWORD, "");
        checkPasswordResult(data, "", "empty String input");
    }

    @Test
    public void setMethod_acceptsEmptyProtectedStringPassword() {
        AuthenticationData data = new WSAuthenticationData();
        data.set(AuthenticationData.PASSWORD, ProtectedString.EMPTY_PROTECTED_STRING);
        checkPasswordResult(data, "", "empty ProtectedString input");
    }

    @Test
    public void setMethod_acceptsStringPassword() {
        AuthenticationData data = new WSAuthenticationData();
        data.set(AuthenticationData.PASSWORD, "myPassword");
        checkPasswordResult(data, "myPassword", "simple String input");
    }

    @Test
    public void setMethod_acceptsCharArrayPassword() {
        AuthenticationData data = new WSAuthenticationData();
        data.set(AuthenticationData.PASSWORD, "myPassword".toCharArray());
        checkPasswordResult(data, "myPassword", "char[] input");
    }

    @Test
    public void setMethod_acceptsProtectedStringPassword() {
        AuthenticationData data = new WSAuthenticationData();
        data.set(AuthenticationData.PASSWORD, new ProtectedString("myPassword".toCharArray()));
        checkPasswordResult(data, "myPassword", "ProtectedString input");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMethod_doesNotAcceptsArbitraryPasswordObjects() {
        AuthenticationData data = new WSAuthenticationData();
        data.set(AuthenticationData.PASSWORD, 1);
    }

    @Test
    public void setMethod_acceptsByteArray() {
        AuthenticationData data = new WSAuthenticationData();
        final byte[] expected = "myPassword".getBytes();
        data.set("bytes", expected);
        assertTrue("Returned 'bytes' value was not an instance of byte[]",
                   data.get("bytes") instanceof byte[]);
        byte[] ret = (byte[]) data.get("bytes");
        assertArrayEquals("Did not get back expected value for 'bytes' using byte[] input",
                          expected, ret);
    }

}
