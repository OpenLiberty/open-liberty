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
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.sso.LTPAConfiguration;
import com.ibm.wsspi.security.ltpa.TokenFactory;

/**
 *
 */
public class LTPATokenServiceTest {
    private static SharedOutputManager outputMgr;
    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext context = mock.mock(ComponentContext.class);
    private final LTPAConfiguration ltpaConfig = mock.mock(LTPAConfiguration.class);
    private final TokenFactory tokenFactory = mock.mock(TokenFactory.class);
    private final Map<String, Object> tokenData = new HashMap<String, Object>();
    private final byte[] tokenBytes = new byte[] {};
    private LTPATokenService ltpaTokenService;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        ltpaTokenService = new LTPATokenService();
        ltpaTokenService.setLtpaConfig(ltpaConfig);
        ltpaTokenService.activate(context);
    }

    @After
    public void tearDown() throws Exception {
        ltpaTokenService.deactivate(context);
        ltpaTokenService.unsetLtpaConfig(ltpaConfig);
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.internal.ltpa.LTPATokenService#createToken(java.util.Map)}.
     */
    @Test
    public void createToken() {
        final String methodName = "createToken";
        try {
            mock.checking(new Expectations() {
                {
                    one(ltpaConfig).getTokenFactory();
                    will(returnValue(tokenFactory));
                    one(tokenFactory).createToken(tokenData);
                }
            });
            assertNotNull("Token should be created",
                          ltpaTokenService.createToken(tokenData));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.internal.ltpa.LTPATokenService#recreateTokenFromBytes(byte[])}.
     */
    @Test
    public void recreateTokenFromBytes() {
        final String methodName = "recreateTokenFromBytes";
        try {
            mock.checking(new Expectations() {
                {
                    one(ltpaConfig).getTokenFactory();
                    will(returnValue(tokenFactory));
                    one(tokenFactory).validateTokenBytes(tokenBytes);
                }
            });
            assertNotNull("Token should be created",
                          ltpaTokenService.recreateTokenFromBytes(tokenBytes));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
