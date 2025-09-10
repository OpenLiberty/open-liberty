/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.acs;

import static com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects.SETUP;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.mockito.Mockito;
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
import com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import test.common.SharedOutputManager;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SAMLMessageXMLSignatureSecurityPolicyRuleTest {

    private static final CommonMockitoObjects common = new CommonMockitoObjects();

    private static final Assertion assertion = common.getAssertion();
    private static final MessageContext messageContext = common.getMessageContext();
    private static final SAMLPeerEntityContext samlPeerEntityContext = common.getSAMLPeerEntityContext();
    private static final SAMLProtocolContext samlProtocolContext = Mockito.mock(SAMLProtocolContext.class);
    private static final SecurityParametersContext securityParamContext = Mockito.mock(SecurityParametersContext.class);
    private static final SignatureValidationParameters signatureValidationParams = Mockito.mock(SignatureValidationParameters.class);
    private static final Response samlResponse = common.getSamlResponse();
    private static final Signature signature = common.getSignature();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final BasicMessageContext<?, ?> basicMessageContext = common.getBasicMessageContext();
    private static final TrustEngine engine = Mockito.mock(TrustEngine.class, "engine");
    private static final SignatureTrustEngine signatureTrustEngine = Mockito.mock(SignatureTrustEngine.class);
    private static final SAMLSignatureProfileValidator signatureProfileValidator = Mockito.mock(SAMLSignatureProfileValidator.class);
  
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

        listAssertion = new ArrayList<Assertion>();

        // Setup common behaviors
        when(basicMessageContext.getSsoConfig()).thenReturn(ssoConfig);
        when(basicMessageContext.getMessageContext()).thenReturn(messageContext);
        
        when(messageContext.getSubcontext(SecurityParametersContext.class)).thenReturn(securityParamContext);
        when(securityParamContext.getSignatureValidationParameters()).thenReturn(signatureValidationParams);
        when(signatureValidationParams.getSignatureTrustEngine()).thenReturn(signatureTrustEngine);
        when(basicMessageContext.getInboundSamlMessageIssuer()).thenReturn("Issuer");

        when(messageContext.getSubcontext(SAMLPeerEntityContext.class, true)).thenReturn(samlPeerEntityContext);
        when(messageContext.getSubcontext(SAMLPeerEntityContext.class)).thenReturn(samlPeerEntityContext);
        when(samlPeerEntityContext.getRole()).thenReturn(role);
        when(samlPeerEntityContext.isAuthenticated()).thenReturn(false);
        
        when(messageContext.getSubcontext(SAMLProtocolContext.class)).thenReturn(samlProtocolContext);
        when(samlProtocolContext.getProtocol()).thenReturn(protocol);
        
        doNothing().when(signatureProfileValidator).validate(any(Signature.class));

        when(samlResponse.getAssertions()).thenReturn(listAssertion);

        when(assertion.getSignature()).thenReturn(signature);
        when(assertion.getElementQName()).thenReturn(conditionQName);

        when(ssoConfig.getSignatureMethodAlgorithm()).thenReturn(LOW_VALUE);

        when(signature.getSignatureAlgorithm()).thenReturn(HIGH_VALUE);

        when(signatureTrustEngine.validate(any(Signature.class), any(CriteriaSet.class))).thenReturn(true);
        when(engine.validate(any(Object.class), any(CriteriaSet.class))).thenReturn(true);
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @Before
    public void before() {
        stateTest = currentTest.getMethodName();

        // Reset mocks to clear any test-specific behaviors
        reset(messageContext, samlResponse, assertion);
        
        // Re-setup common behaviors after reset
        when(messageContext.getSubcontext(SecurityParametersContext.class)).thenReturn(securityParamContext);
        when(securityParamContext.getSignatureValidationParameters()).thenReturn(signatureValidationParams);
        when(signatureValidationParams.getSignatureTrustEngine()).thenReturn(signatureTrustEngine);
        when(basicMessageContext.getInboundSamlMessageIssuer()).thenReturn("Issuer");
        when(messageContext.getSubcontext(SAMLPeerEntityContext.class, true)).thenReturn(samlPeerEntityContext);
        when(messageContext.getSubcontext(SAMLPeerEntityContext.class)).thenReturn(samlPeerEntityContext);
        when(samlPeerEntityContext.getRole()).thenReturn(role);
        when(samlPeerEntityContext.isAuthenticated()).thenReturn(false);
        when(messageContext.getSubcontext(SAMLProtocolContext.class)).thenReturn(samlProtocolContext);
        when(samlProtocolContext.getProtocol()).thenReturn(protocol);
        when(assertion.getSignature()).thenReturn(signature);
        when(assertion.getElementQName()).thenReturn(conditionQName);
        when(samlResponse.getAssertions()).thenReturn(listAssertion);
        
        listAssertion.clear();
        samlMessageXmlSignature = new SAMLMessageXMLSignatureSecurityPolicyRule();
    }


    @Test
    public void testEvaluateProfile() throws SignatureException {
        listAssertion.add(assertion);

        // Setup test-specific behaviors
        when(messageContext.getMessage()).thenReturn(samlResponse);
        when(samlResponse.isSigned()).thenReturn(false);
        when(assertion.isSigned()).thenReturn(false);

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
        // Setup test-specific behaviors
        when(messageContext.getMessage()).thenReturn(samlResponse);
        when(samlResponse.isSigned()).thenReturn(true);
        when(assertion.isSigned()).thenReturn(true);
        
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
