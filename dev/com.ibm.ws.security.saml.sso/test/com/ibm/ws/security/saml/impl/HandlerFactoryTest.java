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
package com.ibm.ws.security.saml.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoHandler;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.metadata.MetadataHandler;

import test.common.SharedOutputManager;

public class HandlerFactoryTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final SsoRequest ssoRequest = common.getSsoRequest();
    private static final MetadataHandler metadataHandler = mockery.mock(MetadataHandler.class, "metadataHandler");

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testGetHandlerInstance_InvalidSAMLVersion() {
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getSamlVersion();
                will(returnValue(Constants.SamlSsoVersion.SAMLSSO11));
                one(ssoRequest).getType();
                will(returnValue(null));
            }
        });

        SsoHandler result = HandlerFactory.getHandlerInstance(ssoRequest);
        assertNull("Expected to receive a null value but was received " + result, result);
    }

    @Test
    public void testGetHandlerInstance_MetadataHandler() {
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getSamlVersion();
                will(returnValue(Constants.SamlSsoVersion.SAMLSSO20));
                one(ssoRequest).getType();
                will(returnValue(Constants.EndpointType.SAMLMETADATA));
            }
        });

        HandlerFactory.setMetadataHandler(metadataHandler);

        SsoHandler result = HandlerFactory.getHandlerInstance(ssoRequest);

        assertEquals("Expected to receive a MetadataHandler object but it was not received.",
                     HandlerFactory.getMetadataHandler(), result);
    }
}
