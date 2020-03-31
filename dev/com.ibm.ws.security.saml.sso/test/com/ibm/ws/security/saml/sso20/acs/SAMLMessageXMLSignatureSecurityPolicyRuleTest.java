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

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SETUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.ws.message.MessageContext;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.trust.TrustEngine;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.Validator;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

import test.common.SharedOutputManager;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SAMLMessageXMLSignatureSecurityPolicyRuleTest {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final Assertion assertion = common.getAssertion();
    private static final MessageContext messageContext = common.getMessageContext();
    private static final Response samlResponse = common.getSamlResponse();
    private static final Signature signature = common.getSignature();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final States stateMachine = common.getStateMachine();
    private static final SAMLMessageContext basicMessageContext = common.getBasicMessageContext();
    private static final TrustEngine engine = mockery.mock(TrustEngine.class, "engine");
    private static final Validator signatureValidator = mockery.mock(Validator.class, "signatureValidator");

    private static final String LOW_VALUE = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    private static final String HIGH_VALUE = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    private static QName conditionQName = new QName("");
    private static SAMLMessageXMLSignatureSecurityPolicyRule samlMessageXmlSignature;
    private static List<Assertion> listAssertion;
    private static String stateTest;

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;
    @Rule
    public TestName currentTest = new TestName();

    @BeforeClass
    public static void setUp() throws ValidationException, SecurityException {
        outputMgr.trace("*=all");
        stateMachine.startsAs(SETUP);

        listAssertion = new ArrayList<Assertion>();

        mockery.checking(new Expectations() {
            {

                allowing((BasicMessageContext) basicMessageContext).getSsoConfig();
                will(returnValue(ssoConfig));
                allowing(basicMessageContext).getInboundMessageIssuer();
                will(returnValue("Issuer"));
                allowing(basicMessageContext).getPeerEntityRole();
                will(returnValue(conditionQName));
                allowing(basicMessageContext).getInboundSAMLProtocol();
                will(returnValue(with(any(String.class))));
                allowing(basicMessageContext).isInboundSAMLMessageAuthenticated();
                will(returnValue(false));
                allowing(basicMessageContext).setInboundSAMLMessageAuthenticated(true);

                allowing(samlResponse).getAssertions();
                will(returnValue(listAssertion));

                allowing(assertion).getSignature();
                will(returnValue(signature));
                allowing(assertion).getElementQName();
                will(returnValue(conditionQName));

                allowing(ssoConfig).getSignatureMethodAlgorithm();
                will(returnValue(LOW_VALUE));

                allowing(signature).getSignatureAlgorithm();
                will(returnValue(HIGH_VALUE));

                allowing(signatureValidator).validate(signature);

                allowing(engine).validate(with(any(Object.class)), with(any(CriteriaSet.class)));
                will(returnValue(true));
            }
        });

    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @Before
    public void before() {
        stateMachine.become(SETUP);
        stateTest = currentTest.getMethodName();

        listAssertion.clear();
        samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule(engine);
    }

    @Test
    public void testEvaluate_InvalidMessageContextType() {
        try {
            samlMessageXmlSignature.evaluate(messageContext);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testEvaluate_ValidMessageContextType() {
        stateMachine.become(stateTest);
        try {
            mockery.checking(new Expectations() {
                {
                    atMost(2).of(basicMessageContext).getInboundSAMLMessage();
                    will(returnValue(null));
                    when(stateMachine.is(stateTest));
                }
            });

            samlMessageXmlSignature.evaluate(basicMessageContext);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testEvaluateProfile() throws ValidationException {
        stateMachine.become(stateTest);
        listAssertion.add(assertion);

        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getInboundSAMLMessage();
                will(returnValue(samlResponse));
                when(stateMachine.is(stateTest));
                one(assertion).isSigned();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule(engine, signatureValidator);

            samlMessageXmlSignature.evaluateProfile(basicMessageContext);

        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testEvaluateAssertion() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getInboundSAMLMessage();
                will(returnValue(samlResponse));
                when(stateMachine.is(stateTest));
                one(assertion).isSigned();
                will(returnValue(true));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            samlMessageXmlSignature.evaluateAssertion(basicMessageContext, assertion);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testEvaluateProtocol_ProtocolMessageNotSigned() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getInboundSAMLMessage();
                will(returnValue(assertion));
                when(stateMachine.is(stateTest));
                one((SignableSAMLObject) assertion).isSigned();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            samlMessageXmlSignature.evaluateProtocol(basicMessageContext);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testEvaluateProtocol_ProtocolMessageSigned() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getInboundSAMLMessage();
                will(returnValue(assertion));
                when(stateMachine.is(stateTest));
                one((SignableSAMLObject) assertion).isSigned();
                will(returnValue(true));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            samlMessageXmlSignature.evaluateProtocol(basicMessageContext);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testEvaluateResponse_SAMLMessageNotSignableSAMLObject() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getInboundSAMLMessage();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            samlMessageXmlSignature.evaluateResponse(basicMessageContext);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testEvaluateResponse_ProtocolMessageNotSigned() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getInboundSAMLMessage();
                will(returnValue(assertion));
                when(stateMachine.is(stateTest));
                one((SignableSAMLObject) assertion).isSigned();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            samlMessageXmlSignature.evaluateResponse(basicMessageContext);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testEvaluateResponse_ProtocolMessageSigned() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getInboundSAMLMessage();
                will(returnValue(assertion));
                when(stateMachine.is(stateTest));
                one((SignableSAMLObject) assertion).isSigned();
                will(returnValue(true));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            samlMessageXmlSignature.evaluateResponse(basicMessageContext);
        } catch (SecurityPolicyException ex) {
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testDoEvaluate_ContextIssuerIsNull() {
        final SAMLMessageContext contextIssuerNull = mockery.mock(BasicMessageContext.class, "contextIssuerNull");

        mockery.checking(new Expectations() {
            {
                one(contextIssuerNull).getInboundMessageIssuer();
                will(returnValue(null));
            }
        });

        try {
            samlMessageXmlSignature.doEvaluate(null, null, contextIssuerNull);
            fail("SecurityPolicyException was not thrown");
        } catch (SecurityPolicyException ex) {
            assertEquals("Expected to receive the message for 'Context issuer unavailable, can not validate signature' but it was not received.",
                         "Context issuer unavailable, can not validate signature", ex.getMessage());
        }
    }

    @Test
    public void testDoEvaluate_BadMessageSignature() throws SecurityException {
        final TrustEngine invalidEngine = mockery.mock(TrustEngine.class, "invalidEngine");
        samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule(invalidEngine);

        mockery.checking(new Expectations() {
            {
                one(invalidEngine).validate(with(any(Object.class)), with(any(CriteriaSet.class)));
                will(returnValue(false));
            }
        });

        try {
            samlMessageXmlSignature.doEvaluate(signature, assertion, basicMessageContext);
            fail("SecurityPolicyException was not thrown");
        } catch (SecurityPolicyException ex) {
            assertTrue("Expected to receive the message for 'Validation of * message signature failed' but it was not received.",
                       match("Validation of * message signature failed", ex.getMessage()));
        }
    }

    @Test
    public void testPerformPreValidation_FailedSignatureValidation() throws ValidationException {
        final ValidationException e = new ValidationException();

        mockery.checking(new Expectations() {
            {
                one(signatureValidator).validate(null);
                will(throwException(e));
            }
        });

        samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule(engine, signatureValidator);

        try {
            samlMessageXmlSignature.performPreValidation(null);
            fail("SecurityPolicyException was not thrown");
        } catch (SecurityPolicyException ex) {
            assertTrue("Expected to receive the message for 'message signature failed signature pre-validation' but it was not received.",
                       match(" * message signature failed signature pre-validation", ex.getMessage()));
        }
    }

    @Test
    public void testPerformPreValidation_SignatureValidatorNull() {
        samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule(engine, null);
        try {
            samlMessageXmlSignature.performPreValidation(signature);
            fail("SecurityPolicyException was not thrown");
        } catch (SecurityPolicyException ex) {
            assertTrue("Expected to receive the message for 'message signature failed signature pre-validation' but it was not received.",
                       match(" * message signature failed signature pre-validation", ex.getMessage()));
        }
    }

    private boolean match(String regex, String input) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        return m.find();
    }
}
