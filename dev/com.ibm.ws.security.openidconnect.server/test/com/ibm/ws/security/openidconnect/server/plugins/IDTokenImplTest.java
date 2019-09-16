/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.oauth20.util.OIDCConstants;

public class IDTokenImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            String id = "server01";
            String tokenString = "idtokenstring";
            String componentId = "componentId";
            String clientId = "client01";
            String username = "testuser";
            String redirectUri = "https://unknown.ibm.com:8010/oidcclient/redirect";
            String stateId = "areyousurethisis1stateid";
            String[] scopes = new String[] { "openid", "profile" };
            int lifetimeSeconds = 3600;
            IDTokenImpl idti = new IDTokenImpl(id, tokenString, componentId, clientId, username,
                            redirectUri, stateId, scopes, lifetimeSeconds, null, null);
            assertNotNull("Ought to generate an instance of IDTokenImpl", idti);
            assertEquals("did not get back a right ID", id, idti.getId());
            assertEquals("did not get back a right Component ID", componentId, idti.getComponentId());
            assertEquals("did not get back a right type", OIDCConstants.TOKENTYPE_ID_TOKEN,
                         idti.getType());
            assertEquals("did not get back a right subtype", OIDCConstants.ID_TOKEN,
                         idti.getSubType());
            assertEquals("did not get back a right subtype", OIDCConstants.ID_TOKEN,
                         idti.getSubType());
            assertNotNull("did not have a createdAt time", idti.getCreatedAt());
            assertEquals("did not get back a right lifetime", lifetimeSeconds,
                         idti.getLifetimeSeconds());
            assertEquals("did not get back a right iusername", username,
                         idti.getUsername());
            assertEquals("did not get back a right clientId", clientId,
                         idti.getClientId());
            assertEquals("did not get back a right redirectUri", redirectUri,
                         idti.getRedirectUri());
            assertEquals("did not get back a right stateId", stateId,
                         idti.getStateId());
            String[] scopes2 = idti.getScope();
            boolean bOpenid = false;
            boolean bProfile = false;
            for (String scope : scopes2) {
                if (scope.equals("openid"))
                    bOpenid = true;
                if (scope.equals("profile"))
                    bProfile = true;
            }
            assertTrue("openid is noit found in scopes", bOpenid);
            assertTrue("profile is noit found in scopes", bProfile);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
