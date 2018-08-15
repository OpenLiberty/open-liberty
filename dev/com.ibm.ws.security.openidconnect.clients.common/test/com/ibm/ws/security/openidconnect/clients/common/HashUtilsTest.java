/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.HashUtils;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;

import test.common.SharedOutputManager;

public class HashUtilsTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected final ConvergedClientConfig clientConfig = mock.mock(ConvergedClientConfig.class, "clientConfig");

    String input = "abcdefghijklmnopqrstuvwxyz10123456789";
    String strRandom = OidcUtil.generateRandom(OidcUtil.RANDOM_LENGTH);
    String timestamp = OidcUtil.getTimeStamp();
    String state = timestamp + strRandom;

    @Test
    public void testDigest() {
        String digestValue1 = HashUtils.digest(input);
        String digestValue2 = HashUtils.digest(input);
        assertEquals("The digest value:'" + digestValue1 + "' is not '" + digestValue2 + "'", digestValue1, digestValue2);
    }

    @Test
    public void testGetStrHashCode() {
        String strHashCode = HashUtils.getStrHashCode(null);
        assertEquals("strHashCode is not empty", "", strHashCode);
        strHashCode = HashUtils.getStrHashCode(input);
        int iHashCode = input.hashCode();
        int oldHashCode = 0;
        if (strHashCode.startsWith("n")) {
            String strTmp = "-" + strHashCode.substring(1);
            oldHashCode = Integer.parseInt(strTmp);
        } else {
            String strTmp = strHashCode.substring(1);
            oldHashCode = Integer.parseInt(strTmp);
        }
        assertEquals("strHashCode is expect " + iHashCode + " but gets " + oldHashCode, iHashCode, oldHashCode);
    }

    @Test
    public void testGetCookieName() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getId();
                will(returnValue("client01"));
            }
        });
        String cookieName = HashUtils.getCookieName("WASOidc", clientConfig, state);
        String newValue = state + "client01";
        String newName = HashUtils.getStrHashCode(newValue);
        String newCookieName = "WASOidc" + newName;
        assertEquals("The cookie name is '" + cookieName + "' which is not the expected value '" + newCookieName + "'",
                cookieName, newCookieName);
    }

    @Test
    public void testCreateStateCookieValue() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClientSecret();
                will(returnValue("secret"));
            }
        });
        String cookieValue = HashUtils.createStateCookieValue(clientConfig, state);
        String timestamp = state.substring(0, OidcUtil.TIMESTAMP_LENGTH);
        String newValue = state + "secret";
        String value = HashUtils.digest(newValue);
        String newCookieValue = timestamp + value;
        assertEquals("The cookie value is '" + cookieValue + "' which is not the expected value '" + newCookieValue + "'",
                cookieValue, newCookieValue);
    }

}
