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
package com.ibm.ws.security.token.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.security.token.TokenService;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

public class SingleSignonTokenImplTest {
    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final Mockery mock = new JUnit4Mockery();
    private final TokenService tokenService = mock.mock(TokenService.class);
    private final Token token = mock.mock(Token.class);
    private SingleSignonTokenImpl singleSignonToken;
    private final byte[] ssoToken = new byte[] {};

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setUp() throws Exception {
        singleSignonToken = new SingleSignonTokenImpl(tokenService);
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.internal.SingleSignonTokenImpl#getName()}.
     */
    @Test
    public void getName() {
        assertEquals("The name must be LtpaToken.",
                     AttributeNameConstants.WSSSOTOKEN_NAME,
                     singleSignonToken.getName());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.internal.SingleSignonTokenImpl#getVersion()}.
     */
    @Test
    public void getVersion() {
        assertEquals("The version must be 2.",
                     2, singleSignonToken.getVersion());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.internal.SingleSignonTokenImpl#clone()}.
     */
    @Test
    public void testClone() {
        mock.checking(new Expectations() {
            {
                one(token).clone();
                will(returnValue(token));
            }
        });
        singleSignonToken.initializeToken(token);
        SingleSignonToken clone = (SingleSignonToken) singleSignonToken.clone();
        assertNotNull("The clone action should create a new SingleSignonToken with the " +
                      "same internal token", clone);
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.internal.SingleSignonTokenImpl#initializeToken(byte[])}.
     */
    @Test
    public void initializeTokenByteArray() throws Exception {
        mock.checking(new Expectations() {
            {
                one(tokenService).recreateTokenFromBytes(ssoToken);
            }
        });
        singleSignonToken.initializeToken(ssoToken);
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.internal.SingleSignonTokenImpl#initializeToken(byte[], boolean)}.
     */
    @Test
    public void initializeTokenByteArrayBoolean() throws Exception {
        mock.checking(new Expectations() {
            {
                one(tokenService).recreateTokenFromBytes(ssoToken);
            }
        });
        singleSignonToken.initializeToken(ssoToken, true);
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.internal.SingleSignonTokenImpl#initializeToken(com.ibm.wsspi.security.ltpa.Token)}.
     */
    @Test
    public void initializeTokenToken() {
        singleSignonToken.initializeToken(token);
    }

}
