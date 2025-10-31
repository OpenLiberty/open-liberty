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

import static com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects.SAML20_AUTHENTICATION_FAIL;
import static com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects.SETUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
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
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Condition;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.OneTimeUse;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.xmlsec.SignatureValidationParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import test.common.SharedOutputManager;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class AssertionValidatorTest {

    private static final CommonMockitoObjects common = new CommonMockitoObjects();

    private static final Assertion assertion = common.getAssertion();
    private static final MessageContext messageContext = common.getMessageContext();
    private static final SAMLPeerEntityContext samlPeerEntityContext = common.getSAMLPeerEntityContext();
    private static final AudienceRestriction audienceRestriction = common.getAudienceRestriction();
    private static final AuthnStatement authnStatement = common.getAuthnStatement();
    private static final BasicMessageContext context = common.getBasicMessageContext();
    private static final Condition condition = common.getCondition();
    private static final Conditions conditions = common.getConditions();
    private static final EntityDescriptor entityDescriptor = common.getEntityDescriptor();
    private static final HttpServletRequest request = common.getServletRequest();
    private static final Issuer issuer = common.getIssuer();

    private static final AcsDOMMetadataProvider acsmetadataProvider = Mockito.mock(AcsDOMMetadataProvider.class);
    private static final NameID nameId = common.getNameId();
    private static final ForwardRequestInfo requestInfo = common.getRequestInfo();

    private static final Signature signature = common.getSignature();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final Subject subject = common.getSubject();
    private static final SubjectConfirmation subjectConfirmation = common.getSubjectConfirmation();
    private static final SubjectConfirmationData subjectConfirmationData = common.getSubjectConfirmationData();

    private static final SAMLProtocolContext samlProtocolContext = Mockito.mock(SAMLProtocolContext.class);
    private static final SecurityParametersContext securityParamContext = Mockito.mock(SecurityParametersContext.class);
    private static final SignatureValidationParameters signatureValidationParams = Mockito.mock(SignatureValidationParameters.class);
    private static final SignatureTrustEngine signatureTrustEngine = Mockito.mock(SignatureTrustEngine.class);
    private static final Audience audience = Mockito.mock(Audience.class);
    
    private static final Response samlResponse = common.getSamlResponse();

    private static final String INVALID_PROVIDERID = "invalid_providerID";
    private static final String METHOD_BEARER = "urn:oasis:names:tc:SAML:2.0:cm:bearer";
    private static final String SAME_VALUE = "same value";
    private static final String SAML_ISSUER_FORMAT = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
    private static final String SAML_REQUESTINFO_ID = "response to id";
    private static final String SERVER_NAME = "mx-gdl";
    private static final String SERVER_PROTOCOL = "http";
    private static final String SERVER_PROVIDER_ID = "edu";
    private static final int SERVER_PORT = 8010;
    private static final int YEARS = 1000;
    private static final String RECIPIENT_URL = SERVER_PROTOCOL + "://" + SERVER_NAME + ":" + SERVER_PORT + "/ibm/saml20/" + SERVER_PROVIDER_ID + "/acs";
    private static final String AUDIENCE_URL = SERVER_PROTOCOL + "://" + SERVER_NAME + ":" + SERVER_PORT + "/ibm/saml20/" + SERVER_PROVIDER_ID;
    private static final String INVALID_METHOD = SubjectConfirmation.METHOD_HOLDER_OF_KEY;

    private static QName role = IDPSSODescriptor.DEFAULT_ELEMENT_NAME;
    
    private static AssertionValidator validator;
    private static String stateTest;
    private static Instant date;
    private static QName conditionQName;
    private static String protocol = SAMLConstants.SAML20P_NS;
    private static List<Audience> listAudience = new ArrayList<Audience>();
    private static List<AudienceRestriction> listAudienceRestriction = new ArrayList<AudienceRestriction>();
    private static List<AuthnStatement> listAuthn = new ArrayList<AuthnStatement>();
    private static List<Condition> listConditions = new ArrayList<Condition>();
    private static List<SubjectConfirmation> listSubjectConfirmation = new ArrayList<SubjectConfirmation>();

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;
    @Rule
    public TestName currentTest = new TestName();

    @BeforeClass
    public static void setup() {
        outputMgr.trace("*=all");
        listConditions.add(condition);

        date = Instant.now().plus(YEARS * 365, ChronoUnit.DAYS);
        conditionQName = OneTimeUse.DEFAULT_ELEMENT_NAME;

        // Setup common mock behaviors
        when(ssoConfig.getSpHostAndPort()).thenReturn(null);
        when(context.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.isPkixTrustEngineEnabled()).thenReturn(false);
        when(context.getMessageContext()).thenReturn(messageContext);
        when(messageContext.getSubcontext(SAMLPeerEntityContext.class, true)).thenReturn(samlPeerEntityContext);
        when(messageContext.getSubcontext(SAMLPeerEntityContext.class)).thenReturn(samlPeerEntityContext);
        when(samlPeerEntityContext.getRole()).thenReturn(role);
        when(messageContext.getSubcontext(SAMLProtocolContext.class)).thenReturn(samlProtocolContext);
        when(samlProtocolContext.getProtocol()).thenReturn(protocol);
        when(messageContext.getSubcontext(SecurityParametersContext.class, true)).thenReturn(securityParamContext);
        when(messageContext.getSubcontext(SecurityParametersContext.class)).thenReturn(securityParamContext);
        when(securityParamContext.getSignatureValidationParameters()).thenReturn(signatureValidationParams);
        when(signatureValidationParams.getSignatureTrustEngine()).thenReturn(signatureTrustEngine);

        when(context.getPeerEntityMetadata()).thenReturn(entityDescriptor);
        when(context.getCachedRequestInfo()).thenReturn(requestInfo);
        when(context.getExternalRelayState()).thenReturn(null);
        when(context.getHttpServletRequest()).thenReturn(request);
        when(context.getSsoService()).thenReturn(ssoService);

        when(ssoConfig.getClockSkew()).thenReturn(0L);
        when(ssoConfig.isWantAssertionsSigned()).thenReturn(false);

        when(entityDescriptor.getEntityID()).thenReturn("");

        when(issuer.getValue()).thenReturn("");
        when(ssoConfig.getPkixTrustedIssuers()).thenReturn(null);
        when(issuer.getFormat()).thenReturn(SAML_ISSUER_FORMAT);

        when(assertion.getIssuer()).thenReturn(issuer);
        when(assertion.getSignature()).thenReturn(null);
        when(assertion.getSubject()).thenReturn(subject);
        when(assertion.getConditions()).thenReturn(conditions);
        when(assertion.getAuthnStatements()).thenReturn(listAuthn);

        when(subject.getSubjectConfirmations()).thenReturn(listSubjectConfirmation);
        when(subject.getNameID()).thenReturn(nameId);

        when(subjectConfirmation.getMethod()).thenReturn(METHOD_BEARER);
        when(subjectConfirmation.getSubjectConfirmationData()).thenReturn(subjectConfirmationData);

        when(subjectConfirmationData.getNotBefore()).thenReturn(null);
        when(subjectConfirmationData.getNotOnOrAfter()).thenReturn(date);
        when(subjectConfirmationData.getInResponseTo()).thenReturn(SAML_REQUESTINFO_ID);
        when(subjectConfirmationData.getRecipient()).thenReturn(RECIPIENT_URL);

        when(requestInfo.getInResponseToId()).thenReturn(SAML_REQUESTINFO_ID);

        when(ssoService.getProviderId()).thenReturn(SERVER_PROVIDER_ID);

        when(request.getServerName()).thenReturn(SERVER_NAME);
        when(request.getServerPort()).thenReturn(SERVER_PORT);
        when(request.getScheme()).thenReturn(SERVER_PROTOCOL);
        when(request.isSecure()).thenReturn(true);

        when(conditions.getAudienceRestrictions()).thenReturn(listAudienceRestriction);
        when(conditions.getNotBefore()).thenReturn(null);
        when(conditions.getNotOnOrAfter()).thenReturn(null);
        when(conditions.getConditions()).thenReturn(listConditions);

        when(condition.getElementQName()).thenReturn(conditionQName);

        validator = new AssertionValidator(context, assertion);
    }

    @Before
    public void before() {
        stateTest = currentTest.getMethodName();
        
        // Reset mocks for each test
        Mockito.reset(
            acsmetadataProvider, samlProtocolContext, securityParamContext, 
            signatureValidationParams, signatureTrustEngine, audience
        );
        
        // Re-setup common behaviors
        when(samlProtocolContext.getProtocol()).thenReturn(protocol);
        when(securityParamContext.getSignatureValidationParameters()).thenReturn(signatureValidationParams);
        when(signatureValidationParams.getSignatureTrustEngine()).thenReturn(signatureTrustEngine);
        when(ssoService.getProviderId()).thenReturn(SERVER_PROVIDER_ID);
        
        // Clear and reset lists
        listAuthn.clear();
        listSubjectConfirmation.clear();
        listAudienceRestriction.clear();
        listSubjectConfirmation.add(subjectConfirmation);
        listAudienceRestriction.add(audienceRestriction);
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testValidateAssertion() {
        try {
            validator.validateAssertion();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testValidateIssuer_NoIssuer() {
        // Setup specific mock behavior for this test
        when(issuer.getFormat()).thenReturn("unmatched format");

        try {
            validator.validateIssuer(false);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateIssuer_IncorrectIssuer() {
        // Setup specific mock behavior for this test
        when(issuer.getFormat()).thenReturn(null);
        when(entityDescriptor.getEntityID()).thenReturn("incorrect_issuer");
        when(issuer.getValue()).thenReturn("correct_issuer");
        when(ssoConfig.getPkixTrustedIssuers()).thenReturn(null);

        try {
            validator.validateIssuer(false);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateSignature() {
        // Setup specific mock behavior for this test
        when(context.getMetadataProvider()).thenReturn(acsmetadataProvider);
        when(samlPeerEntityContext.isAuthenticated()).thenReturn(false);
        when(messageContext.getMessage()).thenReturn(samlResponse);
        when(ssoConfig.isWantAssertionsSigned()).thenReturn(true);
        when(assertion.getSignature()).thenReturn(signature);
        when(assertion.isSigned()).thenReturn(false);
        when(samlResponse.isSigned()).thenReturn(false);

        try {
            validator.validateSignature();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyAssertionSignature() {
        final String LOW_VALUE = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        final String HIGH_VALUE = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

        // Setup specific mock behavior for this test
        when(context.getMetadataProvider()).thenReturn(acsmetadataProvider);
        when(messageContext.getMessage()).thenReturn(samlResponse);
        when(assertion.isSigned()).thenReturn(true);
        when(samlResponse.isSigned()).thenReturn(false);
        when(assertion.getSignature()).thenReturn(signature);
        when(ssoConfig.getSignatureMethodAlgorithm()).thenReturn(HIGH_VALUE);
        when(signature.getSignatureAlgorithm()).thenReturn(LOW_VALUE);

        try {
            validator.verifyAssertionSignature();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_InvalidMethod() {
        // Setup specific mock behavior for this test
        when(subjectConfirmation.getMethod()).thenReturn(INVALID_METHOD);

        try {
            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_SubjectConfirmationData_IsNull() {
        // Setup specific mock behavior for this test
        when(subjectConfirmation.getSubjectConfirmationData()).thenReturn(null);

        try {
            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_NotBefore_IsNull() {
        // Setup specific mock behavior for this test
        date = Instant.now();
        when(subjectConfirmationData.getNotBefore()).thenReturn(date);

        try {
            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_NotOnOrAfter_IsNull() {
        // Setup specific mock behavior for this test
        when(subjectConfirmationData.getNotOnOrAfter()).thenReturn(null);

        try {
            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_NotOnOrAfter_IsBeforeNow() {
        // Setup specific mock behavior for this test
        date = Instant.now().minus(1000 * 365, ChronoUnit.DAYS);
        when(subjectConfirmationData.getNotOnOrAfter()).thenReturn(date);

        try {
            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_RecipientNull() {
        // Setup specific mock behavior for this test
        date = Instant.now().plus(1000 * 365, ChronoUnit.DAYS);
        when(context.getExternalRelayState()).thenReturn("some-relay-state");
        when(subjectConfirmationData.getNotOnOrAfter()).thenReturn(date);
        when(subjectConfirmationData.getInResponseTo()).thenReturn(SAME_VALUE);
        when(subjectConfirmationData.getRecipient()).thenReturn(null);
        when(requestInfo.getInResponseToId()).thenReturn(SAME_VALUE);

        try {
            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_RecipientNotMatch() {
        // Setup specific mock behavior for this test
        date = Instant.now().plus(1000 * 365, ChronoUnit.DAYS);
        when(context.getExternalRelayState()).thenReturn("some-relay-state");
        when(subjectConfirmationData.getNotOnOrAfter()).thenReturn(date);
        when(subjectConfirmationData.getInResponseTo()).thenReturn(SAME_VALUE);
        when(subjectConfirmationData.getRecipient()).thenReturn("recipient_does_not_match");
        when(requestInfo.getInResponseToId()).thenReturn(SAME_VALUE);

        try {
            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_RecipientDoesNotMatchAcsUrl() throws SamlException {
        // Setup specific mock behavior for this test
        date = Instant.now().plus(1000 * 365, ChronoUnit.DAYS);
        when(context.getExternalRelayState()).thenReturn("some-relay-state");
        when(subjectConfirmationData.getNotOnOrAfter()).thenReturn(date);
        when(subjectConfirmationData.getInResponseTo()).thenReturn(SAME_VALUE);
        when(subjectConfirmationData.getRecipient()).thenReturn("http://bogusmachine.ibm.com");
        when(requestInfo.getInResponseToId()).thenReturn(SAME_VALUE);

        try {
            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifySubject_NoValidAssertion() {
        try {
            listSubjectConfirmation.clear();

            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyConditions_NoConditions() {
        try {
            listAudienceRestriction.clear();

            validator.verifyConditions();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyConditions_AssertionBefore() {
        // Setup specific mock behavior for this test
        date = Instant.now().plus(1000 * 365, ChronoUnit.DAYS);
        when(conditions.getNotBefore()).thenReturn(date);

        try {
            validator.verifyConditions();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyConditions_AssertionAfter() {
        // Setup specific mock behavior for this test
        date = Instant.now().minus(1000 * 365, ChronoUnit.DAYS);
        when(conditions.getNotBefore()).thenReturn(null);
        when(conditions.getNotOnOrAfter()).thenReturn(date);

        try {
            validator.verifyConditions();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyConditions_ConditionQNameEqualsAudienceRestriction() {
        // Setup specific mock behavior for this test
        conditionQName = AudienceRestriction.DEFAULT_ELEMENT_NAME;
        
        listAudience.clear();
        listAudience.add(audience);
        
        when(conditions.getNotBefore()).thenReturn(null);
        when(conditions.getNotOnOrAfter()).thenReturn(null);
        when(condition.getElementQName()).thenReturn(conditionQName);
        when(audienceRestriction.getAudiences()).thenReturn(listAudience);
        when(audience.getAudienceURI()).thenReturn(AUDIENCE_URL);

        try {
            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testVerifyConditions_UnknownCondition() {
        // Setup specific mock behavior for this test
        conditionQName = new QName("unknown_condition");
        
        when(conditions.getNotBefore()).thenReturn(null);
        when(conditions.getNotOnOrAfter()).thenReturn(null);
        when(condition.getElementQName()).thenReturn(conditionQName);

        try {
            validator.verifyConditions();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyAudience_InvalidAudienceAttribute() {
        // Setup specific mock behavior for this test
        listAudience.clear();
        listAudience.add(audience);
        
        when(ssoService.getProviderId()).thenReturn(INVALID_PROVIDERID);
        when(audienceRestriction.getAudiences()).thenReturn(listAudience);
        when(audience.getAudienceURI()).thenReturn("http://audience.ibm.com");

        try {
            validator.verifyAudience(listAudienceRestriction);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyAudience_NoAudienceAttribute() {
        try {
            listAudienceRestriction.clear();

            validator.verifyAudience(listAudienceRestriction);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyAuthnStatement_SessionError() {
        // Setup specific mock behavior for this test
        listAuthn.add(authnStatement);
        date = Instant.now().minus(3, ChronoUnit.MINUTES);
        
        when(authnStatement.getSessionNotOnOrAfter()).thenReturn(date);

        try {
            validator.verifyAuthnStatement();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }
}