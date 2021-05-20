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

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SETUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.joda.time.DateTime;
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
import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
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
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import test.common.SharedOutputManager;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class AssertionValidatorTest {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

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

    private static final AcsDOMMetadataProvider acsmetadataProvider = mockery.mock(AcsDOMMetadataProvider.class);
    private static final NameID nameId = common.getNameId();
    private static final ForwardRequestInfo requestInfo = common.getRequestInfo();

    private static final Signature signature = common.getSignature();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final States stateMachine = common.getStateMachine();
    private static final Subject subject = common.getSubject();
    private static final SubjectConfirmation subjectConfirmation = common.getSubjectConfirmation();
    private static final SubjectConfirmationData subjectConfirmationData = common.getSubjectConfirmationData();

    private static final SAMLProtocolContext samlProtocolContext = mockery.mock(SAMLProtocolContext.class);
    private static final SecurityParametersContext securityParamContext = mockery.mock(SecurityParametersContext.class);
    private static final SignatureValidationParameters signatureValidationParams = mockery.mock(SignatureValidationParameters.class);
    private static final SignatureTrustEngine signatureTrustEngine = mockery.mock(SignatureTrustEngine.class);
    private static final Audience audience = mockery.mock(Audience.class, "audience");
    
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
    private static DateTime date;
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
        stateMachine.startsAs(SETUP);
        listConditions.add(condition);

        date = new DateTime().plusYears(YEARS);
        conditionQName = OneTimeUse.DEFAULT_ELEMENT_NAME;

        mockery.checking(new Expectations() {
            {
                allowing(ssoConfig).getSpHostAndPort();
                will(returnValue(null));
                allowing(context).getSsoConfig();
                will(returnValue(ssoConfig));
                allowing(ssoConfig).isPkixTrustEngineEnabled();
                will(returnValue(false));
                allowing(context).getMessageContext();
                will(returnValue(messageContext));
                allowing(messageContext).getSubcontext(SAMLPeerEntityContext.class, true);
                will(returnValue(samlPeerEntityContext));
                allowing(messageContext).getSubcontext(SAMLPeerEntityContext.class);
                will(returnValue(samlPeerEntityContext));
                allowing(samlPeerEntityContext).setAuthenticated(with(any(Boolean.class)));
                allowing(samlPeerEntityContext).getRole();
                will(returnValue(role));
                allowing(messageContext).getSubcontext(SAMLProtocolContext.class);
                will(returnValue(samlProtocolContext));                
                allowing(samlProtocolContext).getProtocol();
                will(returnValue(protocol));
                allowing(messageContext).getSubcontext(SecurityParametersContext.class, true);
                will(returnValue(securityParamContext));
                allowing(messageContext).getSubcontext(SecurityParametersContext.class);
                will(returnValue(securityParamContext));
                allowing(securityParamContext).setSignatureValidationParameters(with(any(SignatureValidationParameters.class)));
                allowing(securityParamContext).getSignatureValidationParameters();
                will(returnValue(signatureValidationParams));
                allowing(signatureValidationParams).getSignatureTrustEngine();
                will(returnValue(signatureTrustEngine));

                allowing(context).getPeerEntityMetadata();
                will(returnValue(entityDescriptor));
                allowing(context).getCachedRequestInfo();
                will(returnValue(requestInfo));
                one(context).getExternalRelayState();
                will(returnValue(null));
                when(stateMachine.is(SETUP));
                allowing(context).getHttpServletRequest();
                will(returnValue(request));
                allowing(context).getSsoService();
                will(returnValue(ssoService));
                one(context).setSubjectNameIdentifier(nameId);

                allowing(ssoConfig).getClockSkew();
                will(returnValue(0l));
                one(ssoConfig).isWantAssertionsSigned();
                will(returnValue(false));
                when(stateMachine.is(SETUP));

                one(entityDescriptor).getEntityID();
                will(returnValue(""));
                when(stateMachine.is(SETUP));

                allowing(issuer).getValue();
                will(returnValue(""));
                allowing(ssoConfig).getPkixTrustedIssuers();
                will(returnValue(null));
                when(stateMachine.is(SETUP));
                atLeast(2).of(issuer).getFormat();
                will(returnValue(SAML_ISSUER_FORMAT));
                when(stateMachine.is(SETUP));

                allowing(assertion).getIssuer();
                will(returnValue(issuer));
                one(assertion).getSignature();
                will(returnValue(null));
                when(stateMachine.is(SETUP));
                allowing(assertion).getSubject();
                will(returnValue(subject));
                allowing(assertion).getConditions();
                will(returnValue(conditions));
                allowing(assertion).getAuthnStatements();
                will(returnValue(listAuthn));

                allowing(subject).getSubjectConfirmations();
                will(returnValue(listSubjectConfirmation));
                one(subject).getNameID();
                will(returnValue(nameId));

                allowing(subjectConfirmation).getMethod();
                will(returnValue(METHOD_BEARER));
                when(stateMachine.isNot(INVALID_METHOD));
                allowing(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));
                when(stateMachine.is(SETUP));

                one(subjectConfirmationData).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(SETUP));
                allowing(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(date));
                when(stateMachine.is(SETUP));
                one(subjectConfirmationData).getInResponseTo();
                will(returnValue(SAML_REQUESTINFO_ID));
                when(stateMachine.is(SETUP));
                allowing(subjectConfirmationData).getRecipient();
                will(returnValue(RECIPIENT_URL));
                when(stateMachine.is(SETUP));

                one(requestInfo).getInResponseToId();
                will(returnValue(SAML_REQUESTINFO_ID));
                when(stateMachine.is(SETUP));

                allowing(ssoService).getProviderId();
                will(returnValue(SERVER_PROVIDER_ID));
                when(stateMachine.isNot(INVALID_PROVIDERID));

                allowing(request).getServerName();
                will(returnValue(SERVER_NAME));
                allowing(request).getServerPort();
                will(returnValue(SERVER_PORT));
                allowing(request).getScheme();
                will(returnValue(SERVER_PROTOCOL));
                allowing(request).isSecure();
                will(returnValue(true));

                allowing(conditions).getAudienceRestrictions();
                will(returnValue(listAudienceRestriction));
                one(conditions).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(SETUP));
                one(conditions).getNotOnOrAfter();
                will(returnValue(null));
                when(stateMachine.is(SETUP));
                allowing(conditions).getConditions();
                will(returnValue(listConditions));

                allowing(condition).getElementQName();
                will(returnValue(conditionQName));
                when(stateMachine.is(SETUP));

            }

        });

        validator = new AssertionValidator(context, assertion);
    }

    @Before
    public void before() {
        stateMachine.become(SETUP);
        stateTest = currentTest.getMethodName();

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
        stateMachine.become(stateTest);

        mockery.checking(new Expectations() {
            {
                atLeast(3).of(issuer).getFormat();
                will(returnValue("unmatched format"));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        mockery.checking(new Expectations() {
            {
                one(issuer).getFormat();
                will(returnValue(null));
                when(stateMachine.is(stateTest));

                atMost(2).of(entityDescriptor).getEntityID();
                will(returnValue("incorrect_issuer"));
                when(stateMachine.is(stateTest));

                one(ssoConfig).getPkixTrustedIssuers();//
                will(returnValue(null));//

                atMost(2).of(issuer).getValue();
                will(returnValue("correct_issuer"));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        mockery.checking(new Expectations() {
            {
                one(context).getMetadataProvider();
                will(returnValue(acsmetadataProvider));
                when(stateMachine.is(stateTest));

                allowing(samlPeerEntityContext).isAuthenticated();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
                allowing(messageContext).getMessage();
                will(returnValue(samlResponse));
                when(stateMachine.is(stateTest));

                one(ssoConfig).isWantAssertionsSigned();
                will(returnValue(true));
                when(stateMachine.is(stateTest));

                one(assertion).getSignature();
                will(returnValue(signature));
                when(stateMachine.is(stateTest));
                one(assertion).isSigned();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
                allowing(samlResponse).isSigned();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        final String LOW_VALUE = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        final String HIGH_VALUE = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

        mockery.checking(new Expectations() {
            {
                one(context).getMetadataProvider();
                will(returnValue(acsmetadataProvider));
                when(stateMachine.is(stateTest));

                allowing(messageContext).getMessage();
                will(returnValue(samlResponse));
                when(stateMachine.is(stateTest));
                
                one(assertion).isSigned();
                will(returnValue(true));
                when(stateMachine.is(stateTest));
                allowing(samlResponse).isSigned();
                will(returnValue(false));
                when(stateMachine.is(stateTest));
                
                one(assertion).getSignature();
                will(returnValue(signature));
                when(stateMachine.is(stateTest));

                one(ssoConfig).getSignatureMethodAlgorithm();
                will(returnValue(HIGH_VALUE));

                one(signature).getSignatureAlgorithm();
                will(returnValue(LOW_VALUE));
            }
        });

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
        stateMachine.become(INVALID_METHOD);
        mockery.checking(new Expectations() {
            {
                atMost(2).of(subjectConfirmation).getMethod();
                will(returnValue(INVALID_METHOD));
            }
        });

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
        stateMachine.become(stateTest);

        mockery.checking(new Expectations() {
            {
                one(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        date = new DateTime();

        mockery.checking(new Expectations() {
            {
                one(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));
                when(stateMachine.is(stateTest));

                one(subjectConfirmationData).getNotBefore();
                will(returnValue(date));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        mockery.checking(new Expectations() {
            {
                one(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));
                when(stateMachine.is(stateTest));

                one(subjectConfirmationData).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(stateTest));

                allowing(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        date = new DateTime().minusYears(1000);

        mockery.checking(new Expectations() {
            {
                one(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));
                when(stateMachine.is(stateTest));

                one(subjectConfirmationData).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
                allowing(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(date));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        date = new DateTime().plusYears(1000);

        mockery.checking(new Expectations() {
            {
                one(context).getExternalRelayState();
                will(returnValue(with(any(String.class))));
                when(stateMachine.is(stateTest));

                one(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));
                when(stateMachine.is(stateTest));

                one(subjectConfirmationData).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
                allowing(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(date));
                when(stateMachine.is(stateTest));
                one(subjectConfirmationData).getInResponseTo();
                will(returnValue(SAME_VALUE));
                when(stateMachine.is(stateTest));
                one(subjectConfirmationData).getRecipient();
                will(returnValue(null));
                when(stateMachine.is(stateTest));

                one(requestInfo).getInResponseToId();
                will(returnValue(SAME_VALUE));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        date = new DateTime().plusYears(1000);

        mockery.checking(new Expectations() {
            {
                one(context).getExternalRelayState();
                will(returnValue(with(any(String.class))));
                when(stateMachine.is(stateTest));

                one(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));
                when(stateMachine.is(stateTest));

                one(subjectConfirmationData).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
                allowing(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(date));
                when(stateMachine.is(stateTest));
                one(subjectConfirmationData).getInResponseTo();
                will(returnValue(SAME_VALUE));
                when(stateMachine.is(stateTest));
                allowing(subjectConfirmationData).getRecipient();
                will(returnValue("recipient_does_not_match"));
                when(stateMachine.is(stateTest));

                one(requestInfo).getInResponseToId();
                will(returnValue(SAME_VALUE));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        date = new DateTime().plusYears(1000);

        mockery.checking(new Expectations() {
            {
                one(context).getExternalRelayState();
                will(returnValue(with(any(String.class))));
                when(stateMachine.is(stateTest));

                one(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));
                when(stateMachine.is(stateTest));

                one(subjectConfirmationData).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
                allowing(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(date));
                when(stateMachine.is(stateTest));
                one(subjectConfirmationData).getInResponseTo();
                will(returnValue(SAME_VALUE));
                when(stateMachine.is(stateTest));
                allowing(subjectConfirmationData).getRecipient();
                will(returnValue("http://bogusmachine.ibm.com"));
                when(stateMachine.is(stateTest));

                one(requestInfo).getInResponseToId();
                will(returnValue(SAME_VALUE));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        date = new DateTime().plusYears(1000);

        mockery.checking(new Expectations() {
            {
                allowing(conditions).getNotBefore();
                will(returnValue(date));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        date = new DateTime().minusYears(1000);

        mockery.checking(new Expectations() {
            {
                one(conditions).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
                allowing(conditions).getNotOnOrAfter();
                will(returnValue(date));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(stateTest);

        conditionQName = AudienceRestriction.DEFAULT_ELEMENT_NAME;

        listAudience.clear();
        listAudience.add(audience);

        mockery.checking(new Expectations() {
            {
                one(conditions).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
                one(conditions).getNotOnOrAfter();
                will(returnValue(null));
                when(stateMachine.is(stateTest));

                one(condition).getElementQName();
                will(returnValue(conditionQName));
                when(stateMachine.is(stateTest));

                one(audienceRestriction).getAudiences();
                will(returnValue(listAudience));
                when(stateMachine.is(stateTest));

                allowing(audience).getAudienceURI();
                will(returnValue(AUDIENCE_URL));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testVerifyConditions_UnknownCondition() {
        stateMachine.become(stateTest);

        conditionQName = new QName("unknown_condition");

        mockery.checking(new Expectations() {
            {
                one(conditions).getNotBefore();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
                one(conditions).getNotOnOrAfter();
                will(returnValue(null));
                when(stateMachine.is(stateTest));

                allowing(condition).getElementQName();
                will(returnValue(conditionQName));
                when(stateMachine.is(stateTest));
            }
        });

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
        stateMachine.become(INVALID_PROVIDERID);

        listAudience.clear();
        listAudience.add(audience);

        mockery.checking(new Expectations() {
            {
                one(ssoService).getProviderId();
                will(returnValue(INVALID_PROVIDERID));
                when(stateMachine.is(INVALID_PROVIDERID));

                one(audienceRestriction).getAudiences();
                will(returnValue(listAudience));
                when(stateMachine.is(INVALID_PROVIDERID));

                allowing(audience).getAudienceURI();
                will(returnValue("http://audience.ibm.com"));
                when(stateMachine.is(INVALID_PROVIDERID));
            }
        });

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
        listAuthn.add(authnStatement);
        date = new DateTime().minusMinutes(3);

        mockery.checking(new Expectations() {
            {
                allowing(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(date));
            }
        });

        try {
            validator.verifyAuthnStatement();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }
}