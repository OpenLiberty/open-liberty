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
package com.ibm.ws.security.saml.error;

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

import test.common.SharedOutputManager;

public class SamlExceptionTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static SamlException samlException;

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
    public void testGetMessage() {
        final String MESSAGE = "message_exception";

        samlException = new SamlException(MESSAGE, null);

        assertEquals("Expected to receive the message for " + MESSAGE + " but it was not received.",
                     MESSAGE, samlException.getMessage());
    }

    @Test
    public void testGetMessage_NullMessage() {
        samlException = new SamlException(null, null);

        assertEquals("Expected to receive the message for the default message but it was not received.",
                     null, samlException.getMessage());
        assertEquals("Expected to receive the message for " + SAML20_AUTHENTICATION_FAIL + " but it was not received.",
                     SAML20_AUTHENTICATION_FAIL, samlException.getMsgKey());
    }

    @Test
    public void testSetFfdcAlready() {
        samlException = new SamlException(null, null, true);
        assertTrue("The value of 'ffdcAlready' should be true.", samlException.ffdcAlready());

        samlException.setFfdcAlready(false);
        assertTrue("The value of 'ffdcAlready' should be false.", !(samlException.ffdcAlready()));
    }

    @SuppressWarnings("static-access")
    @Test
    public void testFormatMessage() {
        final String DEFAULT_MSG = "defaultMsg";

        samlException = new SamlException(null, null);
        String result = samlException.formatMessage(null, DEFAULT_MSG, null);

        assertEquals("Expected to receive the message for " + DEFAULT_MSG + " but it was not received.",
                     DEFAULT_MSG, result);
    }
}
