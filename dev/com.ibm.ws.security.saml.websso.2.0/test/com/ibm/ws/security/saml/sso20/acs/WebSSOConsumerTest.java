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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.opensaml.saml.common.SAMLVersion.VERSION_20;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.mockito.Mockito;
import java.time.Instant;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
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
import com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;

import test.common.SharedOutputManager;

@SuppressWarnings("rawtypes")
public class WebSSOConsumerTest {

    private static final CommonMockitoObjects common = new CommonMockitoObjects();

    private static final Assertion assertion = common.getAssertion();
    private static final AudienceRestriction audienceRestriction = common.getAudienceRestriction();
    private static final AuthnStatement authnStatement = common.getAuthnStatement();
    private static final BasicMessageContext messageContext = common.getBasicMessageContext();
    private static final BasicMessageContextBuilder<?, ?, ?> basicMessageContextBuilder = common.getBasicMessageContextBuilder();
    private static final MessageContext mContext = common.getMessageContext();
    private static final SAMLPeerEntityContext samlPeerEntityContext = common.getSAMLPeerEntityContext();
    private static final SAMLProtocolContext samlProtocolContext = Mockito.mock(SAMLProtocolContext.class);
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

    private static final Decrypter decrypter = Mockito.mock(Decrypter.class);

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
        webSSOConsumer = WebSSOConsumer.getInstance();
        conditionQName = OneTimeUse.DEFAULT_ELEMENT_NAME;

        // Setup common behaviors
        when(basicMessageContextBuilder.buildAcs(request, response, ssoService, "externalRelayState", ssoRequest)).thenReturn(messageContext);

        when(messageContext.getMessageContext()).thenReturn(mContext);
        when(mContext.getMessage()).thenReturn(samlResponse);
        when(messageContext.getCachedRequestInfo()).thenReturn(requestInfo);
        when(messageContext.getExternalRelayState()).thenReturn("externalRelayState");
        when(messageContext.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.getSpHostAndPort()).thenReturn(null);
        when(messageContext.getPeerEntityMetadata()).thenReturn(entityDescriptor);
        when(mContext.getSubcontext(SAMLPeerEntityContext.class, true)).thenReturn(samlPeerEntityContext);
        when(mContext.getSubcontext(SAMLProtocolContext.class, true)).thenReturn(samlProtocolContext);
        
        doNothing().when(samlProtocolContext).setProtocol(anyString());
        doNothing().when(samlPeerEntityContext).setEntityId(anyString());
        
        doNothing().when(samlPeerEntityContext).setAuthenticated(anyBoolean());
        doNothing().when(samlPeerEntityContext).setRole(any(QName.class));
        
        doNothing().when(messageContext).setInboundSamlMessageIssuer(anyString());
        
        when(messageContext.getHttpServletRequest()).thenReturn(request);
        when(messageContext.getSsoService()).thenReturn(ssoService);
        doNothing().when(messageContext).setSubjectNameIdentifier(nameId);
        doNothing().when(messageContext).setValidatedAssertion(assertion);
        when(messageContext.getDecrypter()).thenReturn(decrypter);

        when(ssoService.getProviderId()).thenReturn(SERVER_PROVIDER_ID);

        when(samlResponse.getIssuer()).thenReturn(issuer);
        when(samlResponse.getStatus()).thenReturn(status);
        when(samlResponse.getAssertions()).thenReturn(listAssertions);
        when(samlResponse.getVersion()).thenReturn(VALID_SAML_VERSION);
        when(samlResponse.getInResponseTo()).thenReturn(RESPONSE);
        when(samlResponse.getIssueInstant()).thenReturn(Instant.now());
        when(samlResponse.getDestination()).thenReturn(null);
        when(samlResponse.getSignature()).thenReturn(null);
        when(samlResponse.getEncryptedAssertions()).thenReturn(listEncryptedAssertions);
        when(samlResponse.getElementQName()).thenReturn(new QName("test"));
        when(samlResponse.getDOM()).thenReturn(null);
        when(samlResponse.isSigned()).thenReturn(true);
        when(samlResponse.getSignatureReferenceID()).thenReturn("Id");
        when(samlResponse.hasChildren()).thenReturn(false);

        when(issuer.getValue()).thenReturn(ISSUER_IDENTIFIER);
        when(issuer.getFormat()).thenReturn(null);

        when(ssoConfig.getClockSkew()).thenReturn(60000l);
        when(ssoConfig.isWantAssertionsSigned()).thenReturn(false);

        when(status.getStatusCode()).thenReturn(statusCode);

        when(statusCode.getValue()).thenReturn(SUCCESS_URI);

        when(requestInfo.getInResponseToId()).thenReturn(RESPONSE);

        when(entityDescriptor.getEntityID()).thenReturn(ISSUER_IDENTIFIER);

        when(assertion.getAuthnStatements()).thenReturn(listAuthnStatements);
        when(assertion.getSubject()).thenReturn(subject);
        when(assertion.getIssuer()).thenReturn(issuer);
        when(assertion.getSignature()).thenReturn(null);
        when(assertion.getConditions()).thenReturn(conditions);
        when(assertion.getID()).thenReturn("Id");

        when(subject.getSubjectConfirmations()).thenReturn(listSubjectConfirmation);
        when(subject.getNameID()).thenReturn(nameId);

        when(subjectConfirmation.getMethod()).thenReturn(SubjectConfirmation.METHOD_BEARER);
        when(subjectConfirmation.getSubjectConfirmationData()).thenReturn(subjectConfirmationData);

        when(subjectConfirmationData.getNotBefore()).thenReturn(null);
        when(subjectConfirmationData.getNotOnOrAfter()).thenReturn(Instant.now().plus(1000 * 365, ChronoUnit.DAYS));
        when(subjectConfirmationData.getInResponseTo()).thenReturn(RESPONSE);
        when(subjectConfirmationData.getRecipient()).thenReturn(RECIPIENT_URL);

        when(request.getServerName()).thenReturn(SERVER_NAME);
        when(request.getServerPort()).thenReturn(SERVER_PORT);
        when(request.getScheme()).thenReturn(SERVER_PROTOCOL);
        when(request.isSecure()).thenReturn(true);

        when(conditions.getAudienceRestrictions()).thenReturn(listAudienceRestriction);
        when(conditions.getNotBefore()).thenReturn(null);
        when(conditions.getNotOnOrAfter()).thenReturn(null);
        when(conditions.getConditions()).thenReturn(listConditions);

        when(condition.getElementQName()).thenReturn(conditionQName);

        when(authnStatement.getSessionNotOnOrAfter()).thenReturn(Instant.now());
    }

    @Before
    public void before() {
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

        // Reset mocks to clear any test-specific behaviors
        reset(assertion, subject, subjectConfirmation, samlResponse);
        
        // Re-setup common behaviors after reset
        when(assertion.getAuthnStatements()).thenReturn(listAuthnStatements);
        when(assertion.getSubject()).thenReturn(subject);
        when(assertion.getIssuer()).thenReturn(issuer);
        when(assertion.getSignature()).thenReturn(null);
        when(assertion.getConditions()).thenReturn(conditions);
        when(assertion.getID()).thenReturn("Id");
        
        when(subject.getSubjectConfirmations()).thenReturn(listSubjectConfirmation);
        when(subject.getNameID()).thenReturn(nameId);
        
        when(subjectConfirmation.getMethod()).thenReturn(SubjectConfirmation.METHOD_BEARER);
        when(subjectConfirmation.getSubjectConfirmationData()).thenReturn(subjectConfirmationData);
        when(subjectConfirmationData.getNotBefore()).thenReturn(null);
        when(subjectConfirmationData.getNotOnOrAfter()).thenReturn(Instant.now().plus(1000 * 365, ChronoUnit.DAYS));
        when(subjectConfirmationData.getInResponseTo()).thenReturn(RESPONSE);
        when(subjectConfirmationData.getRecipient()).thenReturn(RECIPIENT_URL);
        
        when(samlResponse.getIssuer()).thenReturn(issuer);
        when(samlResponse.getStatus()).thenReturn(status);
        when(samlResponse.getAssertions()).thenReturn(listAssertions);
        when(samlResponse.getVersion()).thenReturn(VALID_SAML_VERSION);
        when(samlResponse.getInResponseTo()).thenReturn(RESPONSE);
        when(samlResponse.getIssueInstant()).thenReturn(Instant.now());
        when(samlResponse.getDestination()).thenReturn(null);
        when(samlResponse.getSignature()).thenReturn(null);
        when(samlResponse.getEncryptedAssertions()).thenReturn(listEncryptedAssertions);
        when(samlResponse.getElementQName()).thenReturn(new QName("test"));
        when(samlResponse.getDOM()).thenReturn(null);
        when(samlResponse.isSigned()).thenReturn(true);
        when(samlResponse.getSignatureReferenceID()).thenReturn("Id");
        when(samlResponse.hasChildren()).thenReturn(false);
    }

    @AfterClass
    public static void tearDown() {
        BasicMessageContextBuilder.setInstance(instance);
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testHandleSAMLResponse() {
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
        
        when(assertion.getIssuer()).thenReturn(null);
        
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
        when(assertion.getSubject()).thenReturn(null);
        
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
        when(samlResponse.getAssertions()).thenReturn(new ArrayList<Assertion>());
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
        listAssertions.clear();
        listEncryptedAssertions.clear();

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
        listAssertions.clear();
        listEncryptedAssertions.clear();
        listEncryptedAssertions.add(encryptedAssertion);

        when(decrypter.decrypt(encryptedAssertion)).thenReturn(assertion);
        
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
    public void testDecryptEncryptedAssertion_BadAssertion() throws Exception {
        listAssertions.clear();
        listEncryptedAssertions.clear();
        listEncryptedAssertions.add(encryptedAssertion);

        when(decrypter.decrypt(encryptedAssertion))
            .thenThrow(new DecryptionException("bad_assertion"));
        when(samlResponse.getAssertions()).thenReturn(listAssertions);

        try {
            webSSOConsumer.decryptEncryptedAssertion(samlResponse, messageContext);
            fail("Exception was not thrown");
        } catch (SamlException ex) {
            assertEquals(
                "Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                SAML20_AUTHENTICATION_FAIL,
                ex.getMsgKey());
        }
    }
}
