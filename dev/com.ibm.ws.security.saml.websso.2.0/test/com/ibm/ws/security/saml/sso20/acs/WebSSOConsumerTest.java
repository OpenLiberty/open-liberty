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
import static org.opensaml.saml.common.SAMLVersion.VERSION_20;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Condition;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.OneTimeUse;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.xmlsec.encryption.support.DecryptionException;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;

import test.common.SharedOutputManager;

@SuppressWarnings("rawtypes")
public class WebSSOConsumerTest {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();
    private static final States stateMachine = common.getStateMachine();

    private static final Assertion assertion = common.getAssertion();
    private static final AudienceRestriction audienceRestriction = common.getAudienceRestriction();
    private static final AuthnStatement authnStatement = common.getAuthnStatement();
    private static final BasicMessageContext messageContext = common.getBasicMessageContext();
    private static final BasicMessageContextBuilder<?, ?, ?> basicMessageContextBuilder = common.getBasicMessageContextBuilder();
    private static final MessageContext mContext = common.getMessageContext();
    private static final SAMLPeerEntityContext samlPeerEntityContext = common.getSAMLPeerEntityContext();
    private static final SAMLProtocolContext samlProtocolContext = mockery.mock(SAMLProtocolContext.class);
    private static final Condition condition = common.getCondition();
    private static final Conditions conditions = common.getConditions();
    private static final EncryptedAssertion encryptedAssertion = common.getEncryptedAssertion();
    private static final EntityDescriptor entityDescriptor = common.getEntityDescriptor();
    private static final HttpServletRequest request = common.getServletRequest();
    private static final HttpServletResponse response = common.getServletResponse();
    private static final Issuer issuer = common.getIssuer();
    private static final NameID nameId = common.getNameId();
    private static final ForwardRequestInfo requestInfo = common.getRequestInfo();
    private static final Response samlResponse = common.getSamlResponse();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final SsoRequest ssoRequest = common.getSsoRequest();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final Status status = common.getStatus();
    private static final StatusCode statusCode = common.getStatusCode();
    private static final Subject subject = common.getSubject();
    private static final SubjectConfirmation subjectConfirmation = common.getSubjectConfirmation();
    private static final SubjectConfirmationData subjectConfirmationData = common.getSubjectConfirmationData();

    private static final Decrypter decrypter = mockery.mock(Decrypter.class, "decrypter");

    private static final SAMLVersion VALID_SAML_VERSION = VERSION_20;
    private static final String SUCCESS_URI = "urn:oasis:names:tc:SAML:2.0:status:Success";
    private static final String RESPONSE = "HTTP/200";
    private static final String ISSUER_IDENTIFIER = "https://idp.example.org/SAML2";
    private static final String SERVER_NAME = "mx-gdl";
    private static final String SERVER_PROTOCOL = "http";
    private static final String SERVER_PROVIDER_ID = "edu";
    private static final int SERVER_PORT = 8010;
    private static final String RECIPIENT_URL = SERVER_PROTOCOL + "://" + SERVER_NAME + ":" + SERVER_PORT + "/ibm/saml20/" + SERVER_PROVIDER_ID + "/acs";

    private static List<Assertion> listAssertions = new ArrayList<Assertion>();
    private static List<Condition> listConditions = new ArrayList<Condition>();
    private static List<AuthnStatement> listAuthnStatements = new ArrayList<AuthnStatement>();
    private static List<EncryptedAssertion> listEncryptedAssertions = new ArrayList<EncryptedAssertion>();
    private static List<SubjectConfirmation> listSubjectConfirmation = new ArrayList<SubjectConfirmation>();
    private static List<AudienceRestriction> listAudienceRestriction = new ArrayList<AudienceRestriction>();

    private static WebSSOConsumer<?, ?, ?> webSSOConsumer;
    private static QName conditionQName;
    private static String stateTest;

    static BasicMessageContextBuilder<?, ?, ?> instance = new BasicMessageContextBuilder();
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;
    @Rule
    public TestName currentTest = new TestName();

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setUp() throws SamlException, DecryptionException {
        outputMgr.trace("*=all");
        stateMachine.startsAs(SETUP);
        webSSOConsumer = WebSSOConsumer.getInstance();
        conditionQName = OneTimeUse.DEFAULT_ELEMENT_NAME;

        mockery.checking(new Expectations() {
            {
                allowing(basicMessageContextBuilder).buildAcs(request, response, ssoService, "externalRelayState", ssoRequest);
                will(returnValue(messageContext));

                //allowing(messageContext).getInboundMessage();
                allowing(messageContext).getMessageContext();
                will(returnValue(mContext));
                allowing(mContext).getMessage();
                will(returnValue(samlResponse));
                allowing(messageContext).getCachedRequestInfo();
                will(returnValue(requestInfo));
                allowing(messageContext).getExternalRelayState();
                will(returnValue(with(any(String.class))));
                allowing(messageContext).getSsoConfig();
                will(returnValue(ssoConfig));
                allowing(ssoConfig).getSpHostAndPort();
                will(returnValue(null));
                allowing(messageContext).getPeerEntityMetadata();
                will(returnValue(entityDescriptor));
                allowing(mContext).getSubcontext(SAMLPeerEntityContext.class, true);
                will(returnValue(samlPeerEntityContext));
                allowing(mContext).getSubcontext(SAMLProtocolContext.class, true);
                will(returnValue(samlProtocolContext));
                
                allowing(samlProtocolContext).setProtocol(with(any(String.class)));
                allowing(samlPeerEntityContext).setEntityId(with(any(String.class)));
                
                allowing(samlPeerEntityContext).setAuthenticated(with(any(Boolean.class)));
                allowing(samlPeerEntityContext).setRole(with(any(QName.class)));
                
                allowing(messageContext).setInboundSamlMessageIssuer(with(any(String.class)));
                
                //one(messageContext).setInboundSAMLMessageAuthenticated(with(any(Boolean.class)));
                allowing(messageContext).getHttpServletRequest();
                will(returnValue(request));
                one(messageContext).getSsoService();
                will(returnValue(ssoService));
                one(messageContext).setSubjectNameIdentifier(nameId);
                one(messageContext).setValidatedAssertion(assertion);
                allowing(messageContext).getDecrypter();
                will(returnValue(decrypter));

                one(ssoService).getProviderId();
                will(returnValue(SERVER_PROVIDER_ID));

                allowing(samlResponse).getIssuer();
                will(returnValue(issuer));
                allowing(samlResponse).getStatus();
                will(returnValue(status));
                allowing(samlResponse).getAssertions();
                will(returnValue(listAssertions));
                when(stateMachine.is(SETUP));
                allowing(samlResponse).getVersion();
                will(returnValue(VALID_SAML_VERSION));
                allowing(samlResponse).getInResponseTo();
                will(returnValue(RESPONSE));
                allowing(samlResponse).getIssueInstant();
                will(returnValue(new DateTime()));
                allowing(samlResponse).getDestination();
                will(returnValue(null));
                allowing(samlResponse).getSignature();
                will(returnValue(null));
                allowing(samlResponse).getEncryptedAssertions();
                will(returnValue(listEncryptedAssertions));
                allowing(samlResponse).getElementQName();
                will(returnValue(new QName("test")));
                allowing(samlResponse).getDOM();
                will(returnValue(null));
                allowing(samlResponse).isSigned();
                will(returnValue(true));
                allowing(samlResponse).getSignatureReferenceID();
                will(returnValue("Id"));
                allowing(samlResponse).hasChildren();
                will(returnValue(false));

                allowing(issuer).getValue();
                will(returnValue(ISSUER_IDENTIFIER));
                allowing(issuer).getFormat();
                will(returnValue(null));

                allowing(ssoConfig).getClockSkew();
                will(returnValue(60000l));
                one(ssoConfig).isWantAssertionsSigned();
                will(returnValue(false));

                allowing(status).getStatusCode();
                will(returnValue(statusCode));

                allowing(statusCode).getValue();
                will(returnValue(SUCCESS_URI));

                allowing(requestInfo).getInResponseToId();
                will(returnValue(RESPONSE));

                allowing(entityDescriptor).getEntityID();
                will(returnValue(ISSUER_IDENTIFIER));

                allowing(assertion).getAuthnStatements();
                will(returnValue(listAuthnStatements));
                allowing(assertion).getSubject();
                will(returnValue(subject));
                when(stateMachine.is(SETUP));
                one(assertion).getIssuer();
                will(returnValue(issuer));
                when(stateMachine.is(SETUP));
                one(assertion).getSignature();
                will(returnValue(null));
                one(assertion).getConditions();
                will(returnValue(conditions));
                allowing(assertion).getID();
                will(returnValue("Id"));

                one(subject).getSubjectConfirmations();
                will(returnValue(listSubjectConfirmation));
                one(subject).getNameID();
                will(returnValue(nameId));

                one(subjectConfirmation).getMethod();
                will(returnValue(SubjectConfirmation.METHOD_BEARER));
                one(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));

                one(subjectConfirmationData).getNotBefore();
                will(returnValue(null));
                allowing(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(new DateTime().plusYears(1000)));
                one(subjectConfirmationData).getInResponseTo();
                will(returnValue(RESPONSE));
                allowing(subjectConfirmationData).getRecipient();
                will(returnValue(RECIPIENT_URL));

                one(request).getServerName();
                will(returnValue(SERVER_NAME));
                one(request).getServerPort();
                will(returnValue(SERVER_PORT));
                allowing(request).getScheme();
                will(returnValue(SERVER_PROTOCOL));
                allowing(request).isSecure();
                will(returnValue(true));

                one(conditions).getAudienceRestrictions();
                will(returnValue(listAudienceRestriction));
                one(conditions).getNotBefore();
                will(returnValue(null));
                one(conditions).getNotOnOrAfter();
                will(returnValue(null));
                allowing(conditions).getConditions();
                will(returnValue(listConditions));

                allowing(condition).getElementQName();
                will(returnValue(conditionQName));

                allowing(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(new DateTime()));

            }
        });
    }

    @Before
    public void before() {
        stateMachine.become(SETUP);
        BasicMessageContextBuilder.setInstance(basicMessageContextBuilder);
        stateTest = currentTest.getMethodName();

        listAssertions.clear();
        listConditions.clear();
        listAuthnStatements.clear();
        listSubjectConfirmation.clear();
        listAudienceRestriction.clear();
        listEncryptedAssertions.clear();

        listAssertions.add(assertion);
        listConditions.add(condition);
        listAuthnStatements.add(authnStatement);
        listSubjectConfirmation.add(subjectConfirmation);
        listAudienceRestriction.add(audienceRestriction);
    }

    @AfterClass
    public static void tearDown() {
        BasicMessageContextBuilder.setInstance(instance);
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testHandleSAMLResponse() {
        mockery.checking(new Expectations() {
            {
                one(subjectConfirmation).getMethod();
                will(returnValue(null));
            }
        });
        try {
            BasicMessageContext result = webSSOConsumer.handleSAMLResponse(request, response, ssoService, "externalRelayState", ssoRequest);
            assertEquals("Expected to receive the correct message context but it was not received.",
                         result, messageContext);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testHandleSAMLResponse_NullIssuer() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                allowing(assertion).getSubject();
                will(returnValue(subject));
                when(stateMachine.is(stateTest));
                allowing(samlResponse).getAssertions();
                will(returnValue(listAssertions));
                when(stateMachine.is(stateTest));
                one(assertion).getIssuer();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
            }
        });
        try {
            webSSOConsumer.handleSAMLResponse(request, response, ssoService, "externalRelayState", ssoRequest);
            fail("Exception was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleSAMLResponse_NullSubject() {
        stateMachine.become(stateTest);
        mockery.checking(new Expectations() {
            {
                allowing(samlResponse).getAssertions();
                will(returnValue(listAssertions));
                when(stateMachine.is(stateTest));
                one(assertion).getIssuer();
                will(returnValue(issuer));
                when(stateMachine.is(stateTest));
                allowing(assertion).getSubject();
                will(returnValue(null));
                when(stateMachine.is(stateTest));
            }
        });
        try {
            webSSOConsumer.handleSAMLResponse(request, response, ssoService, "externalRelayState", ssoRequest);
            fail("Exception was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleSAMLResponse_NoAuthnStatement() {
        listAuthnStatements.clear();
        try {
            webSSOConsumer.handleSAMLResponse(request, response, ssoService, "externalRelayState", ssoRequest);
            fail("Exception was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleSAMLResponse_EmptyListAssertion() {
        stateMachine.become(stateTest);
        final List<Assertion> emptyListAssertions = new ArrayList<Assertion>();

        mockery.checking(new Expectations() {
            {
                one(samlResponse).getAssertions();
                will(returnValue(listAssertions));
                when(stateMachine.is(stateTest));
                one(samlResponse).getAssertions();
                will(returnValue(emptyListAssertions));
                when(stateMachine.is(stateTest));
            }
        });
        try {
            webSSOConsumer.handleSAMLResponse(request, response, ssoService, "externalRelayState", ssoRequest);
            fail("Exception was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleSAMLResponse_EmptyListAssertion_EmptyListEncryptedAssertions() {
        stateMachine.become(stateTest);
        listAssertions.clear();
        listEncryptedAssertions.clear();
        mockery.checking(new Expectations() {
            {
                allowing(samlResponse).getAssertions();
                will(returnValue(listAssertions));
                when(stateMachine.is(stateTest));
            }
        });
        try {
            webSSOConsumer.handleSAMLResponse(request, response, ssoService, "externalRelayState", ssoRequest);
            fail("Exception was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleSAMLResponse_NullInstance() {
        BasicMessageContextBuilder.setInstance(null);
        try {
            webSSOConsumer.handleSAMLResponse(request, response, ssoService, "externalRelayState", ssoRequest);
            fail("Exception was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testDecryptEncryptedAssertion() throws DecryptionException {
        stateMachine.become(stateTest);
        listAssertions.clear();
        listEncryptedAssertions.clear();
        listEncryptedAssertions.add(encryptedAssertion);

        mockery.checking(new Expectations() {
            {
                one(decrypter).decrypt(encryptedAssertion);
                will(returnValue(assertion));
                when(stateMachine.is(stateTest));
                allowing(samlResponse).getAssertions();
                will(returnValue(listAssertions));
                when(stateMachine.is(stateTest));
            }
        });
        try {
            listAssertions = webSSOConsumer.decryptEncryptedAssertion(samlResponse, messageContext);
            if (!listAssertions.isEmpty()) {
                assertEquals("Expected to receive the correct assertion but it was not received.",
                             listAssertions.get(0), assertion);
            } else {
                fail("Assertion was not returned");
            }
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testDecryptEncryptedAssertion_BadAssertion() throws DecryptionException {
        stateMachine.become(stateTest);
        listAssertions.clear();
        listEncryptedAssertions.clear();
        listEncryptedAssertions.add(encryptedAssertion);

        mockery.checking(new Expectations() {
            {
                one(decrypter).decrypt(encryptedAssertion);
                will(returnValue("bad_assertion"));
                when(stateMachine.is(stateTest));
                allowing(samlResponse).getAssertions();
                will(returnValue(listAssertions));
                when(stateMachine.is(stateTest));
            }
        });

        try {
            webSSOConsumer.decryptEncryptedAssertion(samlResponse, messageContext);
            fail("Exception was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }
}
