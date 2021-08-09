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
package com.ibm.ws.security.saml.sso20.acs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.ws.message.BaseMessageContext;
import org.opensaml.ws.message.MessageContext;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.ws.security.SecurityPolicyRule;
import org.opensaml.ws.security.provider.HTTPRule;
import org.opensaml.ws.transport.http.HTTPInTransport;

import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

import test.common.SharedOutputManager;

public class AcsSecurityPolicyTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final HTTPInTransport transport = mockery.mock(HTTPInTransport.class, "httpInTransport");

    private static final String expectedContentType = "text/html";
    private static final String invalidContentType = "Invalid-Content-Type";
    private static final HTTPRule httpRule = new HTTPRule(expectedContentType, "POST", true);
    private static final AcsSecurityPolicy securityPolicy = new AcsSecurityPolicy();

    private static List<SecurityPolicyRule> rules;
    private static MessageContext messageContext;

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
    }

    @Before
    public void before() {
        rules = securityPolicy.getPolicyRules();
        rules.clear();

        securityPolicy.add(httpRule);
        messageContext = new BaseMessageContext();
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testSecurityPolicyRuleList() {
        boolean containsHttpRule = rules.contains(httpRule);
        assertTrue("The list does not contain the httpRule", containsHttpRule);
    }

    @Test
    public void testEvaluate_Correct() {
        try {
            securityPolicy.evaluate(messageContext);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testEvaluate_Incorrect() {
        mockery.checking(new Expectations() {
            {
                one(transport).getHeaderValue("Content-Type");
                will(returnValue(invalidContentType));
            }
        });

        try {
            messageContext.setInboundMessageTransport(transport);
            securityPolicy.evaluate(messageContext);
            fail("SecurityPolicyException was not thrown");
        } catch (SecurityPolicyException ex) {
            assertEquals("Expected to receive the message for 'Invalid content type' but it was not received.",
                         "Invalid content type, expected " + expectedContentType + " but was " + invalidContentType, ex.getMessage());
        }
    }
}
