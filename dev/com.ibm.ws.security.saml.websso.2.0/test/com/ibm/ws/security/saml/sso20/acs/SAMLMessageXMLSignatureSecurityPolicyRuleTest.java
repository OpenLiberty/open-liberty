/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.SecurityException;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.xmlsec.SignatureValidationParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import test.common.SharedOutputManager;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SAMLMessageXMLSignatureSecurityPolicyRuleTest {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final Assertion assertion = common.getAssertion();
    private static final MessageContext messageContext = common.getMessageContext();
    private static final SAMLPeerEntityContext samlPeerEntityContext = common.getSAMLPeerEntityContext();
    private static final SAMLProtocolContext samlProtocolContext = mockery.mock(SAMLProtocolContext.class);
    private static final SecurityParametersContext securityParamContext = mockery.mock(SecurityParametersContext.class);
    private static final SignatureValidationParameters signatureValidationParams = mockery.mock(SignatureValidationParameters.class);
    private static final Response samlResponse = common.getSamlResponse();
    private static final Signature signature = common.getSignature();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final States stateMachine = common.getStateMachine();
    private static final BasicMessageContext<?, ?> basicMessageContext = common.getBasicMessageContext();
    private static final TrustEngine engine = mockery.mock(TrustEngine.class, "engine");
    private static final SignatureTrustEngine signatureTrustEngine = mockery.mock(SignatureTrustEngine.class);
    private static final SAMLSignatureProfileValidator signatureProfileValidator = mockery.mock(SAMLSignatureProfileValidator.class);
  
    private static final String LOW_VALUE = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    private static final String HIGH_VALUE = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    private static QName conditionQName = new QName("");
    private static QName role = IDPSSODescriptor.DEFAULT_ELEMENT_NAME;
    private static String protocol = SAMLConstants.SAML20P_NS;
    private static SAMLMessageXMLSignatureSecurityPolicyRule samlMessageXmlSignature;
    private static List<Assertion> listAssertion;
    private static String stateTest;

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;
    @Rule
    public TestName currentTest = new TestName();

    @BeforeClass
    public static void setUp() throws SignatureException, SecurityException {
        outputMgr.trace("*=all");
        stateMachine.startsAs(SETUP);

        listAssertion = new ArrayList<Assertion>();

        mockery.checking(new Expectations() {
            {

                allowing((BasicMessageContext) basicMessageContext).getSsoConfig();
                will(returnValue(ssoConfig));
                allowing(basicMessageContext).getMessageContext();
                will(returnValue(messageContext));
                
                
                allowing(messageContext).getSubcontext(SecurityParametersContext.class);
                will(returnValue(securityParamContext));
                allowing(securityParamContext).getSignatureValidationParameters();
                will(returnValue(signatureValidationParams));
                allowing(signatureValidationParams).getSignatureTrustEngine();
                will(returnValue(signatureTrustEngine));
                allowing(basicMessageContext).getInboundSamlMessageIssuer();
                will(returnValue("Issuer"));


                allowing(messageContext).getSubcontext(SAMLPeerEntityContext.class, true);
                will(returnValue(samlPeerEntityContext));
                allowing(messageContext).getSubcontext(SAMLPeerEntityContext.class);
                will(returnValue(samlPeerEntityContext));
                allowing(samlPeerEntityContext).getRole();
                will(returnValue(role));
                allowing(samlPeerEntityContext).isAuthenticated();
                will(returnValue(false));
                allowing(samlPeerEntityContext).setAuthenticated(with(any(Boolean.class)));
                
                allowing(messageContext).getSubcontext(SAMLProtocolContext.class);
                will(returnValue(samlProtocolContext));                
                allowing(samlProtocolContext).getProtocol();
                will(returnValue(protocol));
                
                allowing(signatureProfileValidator).validate(with(any(Signature.class)));

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

                allowing(signatureTrustEngine).validate(with(any(Signature.class)), with(any(CriteriaSet.class)));
                will(returnValue(true));
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
        samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule();
    }


    @Test
    public void testEvaluateProfile() throws SignatureException {
        stateMachine.become(stateTest);
        listAssertion.add(assertion);

        mockery.checking(new Expectations() {
            {
                allowing(messageContext).getMessage();
                will(returnValue(samlResponse));
                when(stateMachine.is(stateTest));
                allowing(samlResponse).isSigned();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
                one(assertion).isSigned();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule();
            samlMessageXmlSignature.setSignaturePrevalidator(signatureProfileValidator);
            samlMessageXmlSignature.initialize();
            samlMessageXmlSignature.invoke(messageContext);
            samlMessageXmlSignature.evaluateProfile(basicMessageContext);

        } catch (Exception ex) {
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testEvaluateAssertion() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                allowing(messageContext).getMessage();
                will(returnValue(samlResponse));
                when(stateMachine.is(stateTest));
                allowing(samlResponse).isSigned();
                will(returnValue(true));
                when(stateMachine.is(stateTest));
                one(assertion).isSigned();
                will(returnValue(true));
                when(stateMachine.is(stateTest));
            }
        });
        SAMLSignatureProfileValidator preValidator = new SAMLSignatureProfileValidator() {
            
        };
        
        try {
            samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule();
            samlMessageXmlSignature.setSignaturePrevalidator(signatureProfileValidator);
            samlMessageXmlSignature.initialize();
            samlMessageXmlSignature.invoke(messageContext);
            samlMessageXmlSignature.evaluateAssertion(basicMessageContext, assertion);
        } catch (Exception ex) {
            fail("Unexpected exception was thrown: " + ex);
        }
    }


    private boolean match(String regex, String input) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        return m.find();
    }
}
